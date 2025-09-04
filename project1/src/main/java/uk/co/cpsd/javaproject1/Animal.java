package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.List;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Animal {

    protected int energyLevel;
    protected int animalId;
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    protected int lastEnergyDecreaseTick = 0;
    protected int lastReproductionTick = -1;
    private int age = 0;
    protected Point position;

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

    public Animal reproduceWithTwo(Animal partner, int currentTick) {
        // both animals are of the same species
        if (!this.getClass().equals(partner.getClass())) {
            throw new IllegalArgumentException("Animals must be of the same species to reproduce.");
        }

        // Update reproduction tick
        this.lastReproductionTick = currentTick;
        partner.lastReproductionTick = currentTick;

        // Subtract energy based on species and gender
        this.energyLevel -= getReproductionEnergyCost(this.gender);
        partner.energyLevel -= getReproductionEnergyCost(partner.gender);

        // Create baby
        Animal baby = createBaby(this.position.x, this.position.y);
        baby.energyLevel = getInitialBabyEnergy();

        return baby;
    }

    public int getLastReproductionTick() {
        return lastReproductionTick;
    }

    public void setLastReproductionTick(int tick) {
        this.lastReproductionTick = tick;
    }

    public boolean willMate(Animal otherAnimal, int currentTick) {

        boolean bothAnimalHaveEnergy = this.isFertile(currentTick) && otherAnimal.isFertile(currentTick);
        boolean isOppositeGender = this.gender != otherAnimal.gender;

        return bothAnimalHaveEnergy && isOppositeGender;
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
