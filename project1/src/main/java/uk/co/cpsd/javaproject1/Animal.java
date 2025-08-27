package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.awt.Point;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Animal {

    protected int energyLevel;
    protected int animalId;
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    protected int lastEnergyDecreaseTick = 0;
    protected int lastReproductionTick = -1;
    private int age = 0;
    protected Point position;
    protected Map<String, Double> dna; // DNA carries animal traits and makes it easy for newborns to inherit their parents' traits
    protected double generation; // define generation of the animal ,
    protected double speed; // common traits among all Species
    protected double reproductionPower; // common traits among all Species

    protected boolean isPregnant = false;
    protected int pregnancyStartTick = -1;
    protected int pregnancyDurationTicks = 0;
    protected Animal pregnancyPartner;

    Random random = new Random();


    private final Gender gender;

    public enum Gender {
        MALE,
        FEMALE
    }

    public void increaseAge() {
        // System.out.println("animal by id" + this.animalId + " is age:" + getAge());
        this.age++;
    }

    public int getAge() {
        return age;
    }

    public Animal(int x, int y, int energyLevel) {
        this.position = new Point(x, y);
        this.energyLevel = energyLevel;
        this.gender = Math.random() < .5 ? Gender.MALE : Gender.FEMALE;
        this.animalId = idCounter.getAndIncrement();

        // Initialize DNA only if not set (for babies)
        if (dna == null) {
            dna = new HashMap<>();
            dna.put("reproductionPower", 5.0 + random.nextDouble() * 2); // Random 5-7
            dna.put("speed", 5.0 + random.nextDouble() * 2); // Random 5-7
            dna.put("generation", 1.0); // Initial animals are Gen 1
            dna.put("animalId", (double) animalId);
        }

        reproductionPower = dna.getOrDefault("reproductionPower", 5.0);
        speed = dna.getOrDefault("speed", 5.0);
    }

    public Gender getGender() {
        return gender;
    }

    public boolean isEnergyZero(int currentTime) {
        return energyLevel <= 0;
    }

    public abstract Color getColor();

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public int getId() {
        return animalId;
    }

    public abstract void act(World world, List<Animal> babyAnimalHolder, List<Animal> removedAnimalsHolder);

    public int getEnergy() {
        return energyLevel;
    }

    public abstract boolean isHungry();

    public abstract DecisionInfo animalDecisionMaking(World world);

    public void setPosition(Point point, int cost) {
        applyMovementCost(cost);
        this.position = new Point(point); // returns a copy
    }

    public Animal reproduceWith(Animal partner, int currentTick) {
        // both animals are of the same species
        if (!this.getClass().equals(partner.getClass())) {
            throw new IllegalArgumentException("Animals must be of the same species to reproduce.");
        }

        // Update reproduction tick
        this.lastReproductionTick = currentTick;
        partner.lastReproductionTick = currentTick;

        // Only females get pregnant
        Animal female = this.gender == Gender.FEMALE ? this : partner;
        Animal male = this.gender == Gender.MALE ? this : partner;

        if (!female.isPregnant) {
            female.isPregnant = true;
            female.pregnancyStartTick = currentTick;
            female.pregnancyDurationTicks = female.getPregnancyDuration();
            female.pregnancyPartner = male;

            // Start of preg
            System.out.println("Pregnancy started for " + female.getClass().getSimpleName() +
                    " ID " + female.animalId + " at tick " + currentTick +
                    ", duration: " + female.pregnancyDurationTicks + " ticks");
        }

        // Subtract energy based on species and gender
        this.energyLevel -= getReproductionEnergyCost(this.gender);
        partner.energyLevel -= getReproductionEnergyCost(partner.gender);

        return null; // Baby created later in act()

    }

    protected abstract int getPregnancyDuration();

    public int getLastReproductionTick() {
        return lastReproductionTick;
    }

    public void setLastReproductionTick(int tick) {
        this.lastReproductionTick = tick;
    }

    public boolean willMate(Animal otherAnimal, int currentTick) {

        boolean bothAnimalHaveEnergy = this.isFertile(currentTick) && otherAnimal.isFertile(currentTick);
        boolean isOppositeGender = this.gender != otherAnimal.gender;
        boolean notPregnant = !this.isPregnant && !otherAnimal.isPregnant;
        return bothAnimalHaveEnergy && isOppositeGender && notPregnant;
    }

    protected void handlePregnancy(World world, List<Animal> babyAnimalHolder) {
        if (isPregnant && (world.getTotalTicks() - pregnancyStartTick >= pregnancyDurationTicks)) {
            // Create baby
            Animal baby = createBaby(position.x, position.y);
            baby.energyLevel = getInitialBabyEnergy();

            // Initialize baby DNA
            Map<String, Double> babyDNA = new HashMap<>();
            for (String key : dna.keySet()) {
                if (!key.equals("generation") && !key.equals("animalId")) {
                    double parent1Value = dna.getOrDefault(key, 5.0);
                    double parent2Value = pregnancyPartner != null ? pregnancyPartner.dna.getOrDefault(key, 5.0) : parent1Value;
                    double avgValue = (parent1Value + parent2Value) / 2;
                    if (random.nextDouble() < 0.1) {
                        avgValue += random.nextGaussian() * 1;
                        avgValue = Math.max(0, Math.min(avgValue, 12));
                    }
                    babyDNA.put(key, avgValue);
                }
            }
            double parent1Gen = dna.get("generation");
            double parent2Gen = pregnancyPartner != null ? pregnancyPartner.dna.getOrDefault("generation", 1.0) : parent1Gen;
            babyDNA.put("generation", Math.max(parent1Gen, parent2Gen) + 1);
            baby.dna = babyDNA; // Note: animalId set by constructor

            // baby birth check
            System.out.println("Baby " + baby.getClass().getSimpleName() + " ID " + baby.animalId +
                    " born to " + this.getClass().getSimpleName() + " ID " + this.animalId +
                    " at tick " + world.getTotalTicks() +
                    " at position (" + position.x + "," + position.y + ")");

            // Reset pregnancy
            isPregnant = false;
            pregnancyStartTick = -1;
            pregnancyDurationTicks = 0;
            pregnancyPartner = null;

            // Add baby to the world
            babyAnimalHolder.add(baby);
        }
    }


    public abstract boolean isFertile(int tick);

    protected abstract int getReproductionEnergyCost(Gender gender);

    protected abstract int getInitialBabyEnergy();

    protected abstract Animal createBaby(int x, int y);

    protected abstract int getReproductionCooldown(Gender gender);

    public void applyMovementCost(int cost) {
        energyLevel = energyLevel - cost;
    };

    /**
     * Determines if the animal has reached the age at which it should be considered
     * deceased or removed from the simulation.
     */
    public abstract boolean hasReachedEndOfLife();

    public Point getAnimalCoordinates() {
        return new Point(position);
    }
}
