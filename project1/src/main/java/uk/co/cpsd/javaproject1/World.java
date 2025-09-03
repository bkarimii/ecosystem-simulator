package uk.co.cpsd.javaproject1;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.awt.Point;

public class World {
    public final int size = 10;
    private List<Animal> animals;
    private int totalTicks;
    private final int MAX_GRASS_AGE = 100;
    private int[][] grassDeathTime = new int[size][size];
    private List<Integer> goatPopulationHistory = new ArrayList<>();
    private List<Integer> grassPopulationHistory = new ArrayList<>();
    private List<Integer> lionPopulationHistory = new ArrayList<>();
    public static int numOfDeadGoats = 0;
    private int numOfAliveGoats = 0;
    private boolean isGUIMode;
    private final AudioPlayer player;
    private static boolean isFirstWrite = true;

    public World(int numOfGoats, int numOfLions, boolean isGUIMode, AudioPlayer player) {
        animals = new ArrayList<>();
        for (int i = 0; i < numOfGoats; i++) {
//            animals.add(new Goat((int) (Math.random() * size), (int) (Math.random() * size)));
            Animal goat = new Goat((int) (Math.random() * size), (int) (Math.random() * size));
            animals.add(goat);
            writeAnimalTraitsToCSV(goat); // Log traits for goats

        }

        for (int j = 0; j < numOfLions; j++) {
//            animals.add(new Lion((int) (Math.random() * size), (int) (Math.random() * size)));
            Animal lion = new Lion((int) (Math.random() * size), (int) (Math.random() * size));
            animals.add(lion);
            writeAnimalTraitsToCSV(lion); // Log traits for initial lions
        }
        this.isGUIMode = isGUIMode;
        this.player = player;
    }

    public List<Integer> getGoatPopulationHistory() {
        return goatPopulationHistory;
    }

    public List<Integer> getLionPopulationHistory() {
        return lionPopulationHistory;
    }

    public List<Integer> getGrassPopulationHistory() {
        return grassPopulationHistory;
    }

    public boolean hasGrass(int x, int y) {
        return grassDeathTime[x][y] > totalTicks;
    }

    public int goatCount() {
        return (int) animals.stream().filter(animal -> animal instanceof Goat).count();
    }

    public int lionCount() {
        return (int) animals.stream().filter(animal -> animal instanceof Lion).count();
    }

    public List<Animal> getAnimals() {
        return animals;
    }

    public int grassCount() {
        int numOfGrass = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (hasGrass(x, y)) {
                    numOfGrass++;
                }
            }
        }
        return numOfGrass;
    }

    public int getTicksElapsed() {
        return totalTicks;
    }

    public void growGrass() {
        int x = (int) (Math.random() * size);
        int y = (int) (Math.random() * size);
        if (!hasGrass(x, y)) {
            grassDeathTime[x][y] = totalTicks + MAX_GRASS_AGE;
        }
    }

    public void removeGrass(int x, int y) {
        grassDeathTime[x][y] = 0;
    }

    public Stream<Animal> animals() {
        return animals.stream();
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    // =============================

    public void writeToCSV(List<Integer> goatHistory, List<Integer> grassHistory, List<Integer> lionHistory) {

        try {

            FileWriter csvData = new FileWriter("data.csv");
            csvData.write("Tick, NumOfGoat, NumOfGrass, NumOfLion \n");
            for (int i = 0; i < goatHistory.size(); i++) {
                csvData.write(i + ", " + goatHistory.get(i) + ", " + grassHistory.get(i) + ", " + lionHistory.get(i));
                csvData.write("\n");
            }
            csvData.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void tick() {
        numOfAliveGoats=0;
        List<Animal> babyAnimalHolder = new ArrayList<>();
        List<Animal> removedAnimalsHolder = new ArrayList<>();
        totalTicks++;
        for (int i = 0; i < 50; i++) {
            growGrass();
        }

        if (isGUIMode && totalTicks % 12 == 0) {
            player.playSound("/roar.wav");
        }

        if (isGUIMode && totalTicks % 8 == 0) {
            player.playSound("/goat.wav");
        }

        goatPopulationHistory.add(goatCount());
        grassPopulationHistory.add(grassCount());
        lionPopulationHistory.add(lionCount());
        List<Animal> deadAnimals = new ArrayList<>();
        for (Animal animal : animals) {
            animal.increaseAge();

            boolean isTooOld = animal.hasReachedEndOfLife();
            boolean isDead = animal.isEnergyZero(totalTicks);
            if (isDead || isTooOld) {
                deadAnimals.add(animal);
                if(animal instanceof Goat){
                    numOfDeadGoats++;
               }

            }
            if (animal instanceof Goat) {
               numOfAliveGoats++;
            }
            animal.act(this, babyAnimalHolder, removedAnimalsHolder);
        }

        animals.addAll(babyAnimalHolder);
        animals.removeAll(deadAnimals);
        animals.removeAll(removedAnimalsHolder);

    }

    public Map<Point, List<Object>> scanNeighbour(int x, int y) {

        Map<Point, List<Object>> resultOfScan = new HashMap<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {

                int nx = x + dx;
                int ny = y + dy;
                if (!isValid(nx, ny, size))
                    continue;
                Point position = new Point(nx, ny);
                List<Object> infoOfPosition = new ArrayList<>();
                if (hasGrass(nx, ny)) {
                    infoOfPosition.add("grass");
                }

                Animal animalAtPos = getAnimalAt(nx, ny);
                if (animalAtPos != null) {
                    infoOfPosition.add(animalAtPos);

                }

                resultOfScan.put(position, infoOfPosition);
            }
        }

        return resultOfScan;
    }

    public boolean isValid(int x, int y, int worldSize) {
        return x >= 0 && y >= 0 && x < worldSize && y < worldSize;
    }

    public Animal getAnimalAt(int x, int y) {
        for (Animal animal : animals) {
            if (animal.getX() == x && animal.getY() == y) {
                return animal;
            }
        }
        return null;
    }

    public void removeAnimal(int x, int y, List<Animal> removedAnimalsHolder) {
        Animal animal = getAnimalAt(x, y);
        removedAnimalsHolder.add(animal);
    }

    public int getNumOfAliveGoats() {
        return numOfAliveGoats;
    }

    public void writeAnimalTraitsToCSV(Animal animal) {
        try (FileWriter csvData = new FileWriter("animal_traits.csv", !isFirstWrite)) {
            if (isFirstWrite) {
                csvData.write("Tick,AnimalId,Species,generation,fleeingPower,huntingPower,speed,parentsId,reproductionPower\n");
                isFirstWrite = false; // Mark file as initialized
            }

            StringBuilder row = new StringBuilder();
            row.append(totalTicks).append(",");
            row.append(animal.getId()).append(",");
            row.append(animal.getClass().getSimpleName()).append(",");

            Integer generation = animal.dna.getTrait("generation", Integer.class);
            row.append(generation != null ? generation : 1).append(",");

            Double fleeingPower = animal.dna.getTrait("fleeingPower", Double.class);
            row.append(fleeingPower != null ? fleeingPower : 0.0).append(",");

            Double huntingPower = animal.dna.getTrait("huntingPower", Double.class);
            row.append(huntingPower != null ? huntingPower : 0.0).append(",");

            Double speed = animal.dna.getTrait("speed", Double.class);
            row.append(speed != null ? speed : 0.0).append(",");

            String parentsId = animal.dna.getTrait("parentsId", String.class);
            row.append(parentsId != null ? parentsId : "f0m0").append(",");

            Double reproductionPower = animal.dna.getTrait("reproductionPower", Double.class);
            row.append(reproductionPower != null ? reproductionPower : 0.0);

            row.append("\n");
            csvData.write(row.toString());
        } catch (IOException e) {
            System.out.println("Error writing to animal_traits.csv: " + e);
        }
    }

}
