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
    protected int generation; // define generation of the animal ,
    protected double speed; // common traits among all Species
    protected double reproductionPower; // common traits among all Species
    protected int lastMoveTick=-1;

    protected boolean isPregnant = false;
    protected int pregnancyStartTick = -1;
    protected int pregnancyDurationTicks = 0;
    protected Animal pregnancyPartner;

    protected DNA dna=new DNA();

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

        // Initialize DNA traits if not already present
        if (!dna.hasTraits("reproductionPower")) {
            dna.setTrait("reproductionPower", 5.0 + random.nextDouble() * 2); // Random 5-7
        }
        if (!dna.hasTraits("speed")) {
            dna.setTrait("speed", 5.0 + random.nextDouble() * 2); // Random 5-7
        }
        if (!dna.hasTraits("generation")) {
            dna.setTrait("generation", 1); // Initial generation
        }
        if (!dna.hasTraits("animalId")) {
            dna.setTrait("animalId", animalId); // store as Integer
        }

        if(!dna.hasTraits("parentsId")){
            dna.setTrait("parentId","f0m0");
        }

        reproductionPower = dna.getTrait("reproductionPower", Double.class);
        speed = dna.getTrait("speed", Double.class);
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

    public abstract int moveCooldown();

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
            //  Map<String, Double> babyDNA = new HashMap<>();
            DNA babyDNA=new DNA();
            for (String key : dna.getTraitsName()) {

                if (key.equals("generation") || key.equals("animalId") || key.equals("parentsId")) {
                    continue;
                }

                Object parent1Value = dna.getTrait(key, Object.class); // Mother's value
                Object parent2Value = pregnancyPartner != null ?
                        pregnancyPartner.dna.getTrait(key, Object.class) : parent1Value;

                if (parent1Value instanceof Number && parent2Value instanceof Number) {

                    double val1 = ((Number) parent1Value).doubleValue();
                    double val2 = ((Number) parent2Value).doubleValue();
                    double avgValue = (val1 + val2) / 2.0;
                    if (random.nextDouble() < 0.1) {
                        avgValue += random.nextGaussian();
                        avgValue = Math.max(0, Math.min(avgValue, 12));
                    }
                    avgValue = Math.round(avgValue * 100.0) / 100.0;
                    babyDNA.setTrait(key, avgValue);
                } else {
                    babyDNA.setTrait(key, parent1Value);
                }
            }

            Integer parent1Gen = dna.getTrait("generation", Integer.class);
            Integer parent2Gen = pregnancyPartner != null ?
                    pregnancyPartner.dna.getTrait("generation", Integer.class) : parent1Gen;
            babyDNA.setTrait("generation", Math.max(parent1Gen, parent2Gen) + 1);

            babyDNA.setTrait("animalId", baby.animalId);

            //parentsId
            String parentsId = "f" + this.animalId + "m" + (pregnancyPartner != null ? pregnancyPartner.animalId : 0);
            babyDNA.setTrait("parentsId", parentsId);

            baby.dna = babyDNA;

            // baby birth check
            System.out.println("Baby " + baby.getClass().getSimpleName() + " ID " + baby.animalId +
                    " born to " + this.getClass().getSimpleName() + " ID " + this.animalId +
                    " at tick " + world.getTotalTicks() +
                    " at position (" + position.x + "," + position.y + ")");

            // Log the baby's traits to CSV
            world.writeAnimalTraitsToCSV(baby);

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
