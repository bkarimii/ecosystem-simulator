package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.Point;

import uk.co.cpsd.javaproject1.DecisionInfo.DecisionType;

public class Lion extends Animal {

    public final int HUNGER_TRESHHOLDS = 50;
    private static final int Lion_MAX_AGE = 60;

    public static int numOfEatenGoats = 0;

    public Lion(int x, int y) {
        super(x, y, 20);
    }

    @Override
    public boolean isHungry() {
        return energyLevel <= HUNGER_TRESHHOLDS;
    }

    @Override
    public Color getColor() {
        return Color.GRAY;
    }

    @Override
    public DecisionInfo animalDecisionMaking(World world) {

        Map<Point, List<Object>> scanedNeighbourHoodByLion = world.scanNeighbour(getX(), getY());

        // 1. Priority: Eat if hungry
        if (isHungry()) {
            for (Map.Entry<Point, List<Object>> entry : scanedNeighbourHoodByLion.entrySet()) {
                for (Object obj : entry.getValue()) {
                    if (obj instanceof Goat) {
                        return new DecisionInfo(DecisionType.EAT, entry.getKey());
                    }
                }
            }
        }

        // 2. Priority: Reproduce (check nearby lions)
        for (Map.Entry<Point, List<Object>> entry : scanedNeighbourHoodByLion.entrySet()) {
            for (Object obj : entry.getValue()) {
                if (obj instanceof Lion otherLion && this.willMate(otherLion, world.getTotalTicks())) {
                    return new DecisionInfo(DecisionType.REPRODUCE, entry.getKey());
                }
            }
        }

        // 3. Default: Stay in place or Random move
        boolean moveChance = Math.random() < 0.25;
        if (moveChance) {
            Point randomMove = findRandomPos(scanedNeighbourHoodByLion);
            return new DecisionInfo(DecisionType.WANDER, randomMove);
        } else {
            Point sameLocation = new Point(getX(), getY());
            return new DecisionInfo(DecisionType.WANDER, sameLocation);
        }

    }

    public Point findRandomPos(Map<Point, List<Object>> neighbourHoodPos) {
        List<Point> allTiles = new ArrayList<>(neighbourHoodPos.keySet());
        if (!allTiles.isEmpty()) {
            return allTiles.get(new Random().nextInt(allTiles.size()));
        }
        return new Point(this.getX(), this.getY());
    }

    public void eatGoat() {
        this.energyLevel += 20;
        numOfEatenGoats++;
        // RoarPlayer.playSound("/roar.wav");

    }

    @Override
    public void act(World world, List<Animal> babyAnimalHolder, List<Animal> removedAnimalsHolder) {
        DecisionInfo decisionInfo = animalDecisionMaking(world);
        Point nextPos = decisionInfo.nextPos();

        switch (decisionInfo.type()) {
            case EAT -> {
                if (world.getAnimalAt(nextPos.x, nextPos.y) instanceof Goat) {
                    int currentAliveGoats = world.getNumOfAliveGoats();
                    boolean successfulHunt = attemptHunting(world.getNumOfAliveGoats()) && currentAliveGoats > 10;
                    if (successfulHunt) {
                        eatGoat();
                        world.removeAnimal(nextPos.x, nextPos.y, removedAnimalsHolder);
                        setPosition(nextPos, 5);
                    } else {
                        Point samePosition = new Point(this.getX(), this.getY());
                        setPosition(samePosition, 5);
                        System.out.println("=============Hunting failed=============");
                    }

                }
            }
            case REPRODUCE -> {
                Point partnerlocation = decisionInfo.nextPos();
                Animal partnerLion = world
                        .getAnimalAt(partnerlocation.x, partnerlocation.y);

                if (partnerLion instanceof Lion otherLion && this.willMate(otherLion, world.getTotalTicks())) {
                    Animal babyLion = this.reproduceWithTwo(otherLion, world.getTotalTicks());
                    babyAnimalHolder.add(babyLion);
                }
            }
            case FLEE -> {
                Point safeRandomPoint = decisionInfo.nextPos();
                setPosition(safeRandomPoint, 5);
            }
            case WANDER -> {
                Point randomMove = decisionInfo.nextPos();
                if (randomMove.x == getX() && randomMove.y == getY()) {
                    setPosition(randomMove, 0);
                } else {
                    setPosition(randomMove, 1);
                }

            }

        }
    }

    public boolean hasReachedEndOfLife() {
        return this.getAge() > Lion_MAX_AGE;
    }

    public double getHuntingChance(int numOfAliveGoats) {
        if (numOfAliveGoats <= 10)
            return 0.0;
        if (numOfAliveGoats <= 15)
            return 0.3;
        if (numOfAliveGoats <= 25)
            return 0.6;
        if (numOfAliveGoats <= 30)
            return 0.8;
        return 1.0;
    }

    public boolean attemptHunting(int numOfAliveGoats) {

        return Math.random() < getHuntingChance(numOfAliveGoats);
    }

    public int getReproductionEnergyCost(Gender gender) {
        return gender == Gender.FEMALE ? 15 : 10;
    };

    public Animal createBaby(int x, int y) {
        return new Lion(x, y);
    };

    public int getInitialBabyEnergy() {
        return 20;
    };

    public int getReproductionCooldown(Gender gender) {
        return gender == Gender.FEMALE ? 10 : 5;
    };

    @Override
    public boolean isFertile(int currentTick) {
        boolean sinceLastReproduce = currentTick
                - this.lastReproductionTick >= getReproductionCooldown(this.getGender());
        boolean hasEnergy = this.energyLevel >= getReproductionEnergyCost(this.getGender());
        return sinceLastReproduce && hasEnergy;
    }

}
