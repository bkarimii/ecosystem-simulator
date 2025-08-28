package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.Point;

import uk.co.cpsd.javaproject1.DecisionInfo.DecisionType;

public class Lion extends Animal {

    public final int HUNGER_THRESHOLDS = 40;
    private static final int Lion_MAX_AGE = 45;
    private double huntingPower; // specific trait for Lions

    public static int numOfEatenGoats = 0;

    public Lion(int x, int y) {
        super(x, y, 40);
        Random random=new Random();
        if (!dna.containsKey("huntingPower")) {
            dna.put("huntingPower", 7.0 + new Random().nextDouble() * 3); // Random 7-10
        }

        huntingPower=dna.getOrDefault("huntingPower",8.0);
    }

    @Override
    public boolean isHungry() {
        return energyLevel <= HUNGER_THRESHOLDS;
    }

    @Override
    public Color getColor() {
        return Color.GRAY;
    }

    @Override
    public DecisionInfo animalDecisionMaking(World world) {

        Map<Point, List<Object>> scannedNeighbourHoodByLion = world.scanNeighbour(getX(), getY());

        // 1. Priority: Eat if hungry
        if (isHungry()) {
            for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByLion.entrySet()) {
                for (Object obj : entry.getValue()) {
                    if (obj instanceof Goat) {
                        return new DecisionInfo(DecisionType.EAT, entry.getKey());
                    }
                }
            }
        }

        // 2. Priority: Reproduce (check nearby lions)
        for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByLion.entrySet()) {
            for (Object obj : entry.getValue()) {
                if (obj instanceof Lion otherLion && this.willMate(otherLion, world.getTotalTicks())) {
                    return new DecisionInfo(DecisionType.REPRODUCE, entry.getKey());
                }
            }
        }

        // 3. New: Score tiles based on affinities (goat=10, lion=5)
        Point bestMove = null;
        double bestScore = -1;
        for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByLion.entrySet()) {
            double score = 0;
            for (Object obj : entry.getValue()) {
                if (obj instanceof Goat) {
//                    score += 10; // Affinity for goats
                    score+=huntingPower;
                } else if (obj instanceof Lion) {
                    score += 5;  // Affinity for other lions
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = entry.getKey();
            }
        }

        // 4. Default: Stay in place or Random move
        boolean moveChance = Math.random() < 0.2;
        if (moveChance) {
            Point randomMove = findRandomPos(scannedNeighbourHoodByLion);
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
        this.energyLevel += 40;
        numOfEatenGoats++;
        // RoarPlayer.playSound("/roar.wav");

    }

    @Override
    public void act(World world, List<Animal> babyAnimalHolder, List<Animal> removedAnimalsHolder) {
        // Handle pregnancy before other actions
        handlePregnancy(world, babyAnimalHolder);

        DecisionInfo decisionInfo = animalDecisionMaking(world);
        Point nextPos = decisionInfo.nextPos();

        int currentTick=world.getTotalTicks();
        boolean canMove= lastMoveTick==-1 || (currentTick-lastMoveTick)>moveCooldown();

        switch (decisionInfo.type()) {
            case EAT -> {
                if (world.getAnimalAt(nextPos.x, nextPos.y) instanceof Goat) {
                    int currentAliveGoats = world.getNumOfAliveGoats();
                    boolean successfulHunt = attemptHunting(world.getNumOfAliveGoats()) && currentAliveGoats > 10;
                    if (successfulHunt) {
                        if (!nextPos.equals(position)) {
                            if (canMove) {
                                eatGoat();
                                world.removeAnimal(nextPos.x, nextPos.y, removedAnimalsHolder);
                                setPosition(nextPos, 3);
                                lastMoveTick = currentTick;
                            } else {
                                setPosition(new Point(getX(), getY()), 0);
                            }
                        } else {
                            eatGoat();
                            world.removeAnimal(nextPos.x, nextPos.y, removedAnimalsHolder);
                            setPosition(nextPos, 0);
                        }

                    } else {
                        Point samePosition = new Point(this.getX(), this.getY());
                        setPosition(samePosition, 5);
                    }

                }
            }
            case REPRODUCE -> {
                Point partnerLocation = decisionInfo.nextPos();
                Animal partnerLion = world
                        .getAnimalAt(partnerLocation.x, partnerLocation.y);

                if (partnerLion instanceof Lion otherLion && this.willMate(otherLion, currentTick)) {
                     this.reproduceWith(otherLion, currentTick);
                }
            }

            case FLEE -> {
                if (!nextPos.equals(position)) {
                    if (canMove) {
                        setPosition(nextPos, isPregnant ? 7 : 5);
                        lastMoveTick = currentTick;
                        System.out.println("Lion ID " + animalId + " fled to (" + nextPos.x + "," + nextPos.y + ") at tick " + currentTick);
                    } else {
                        setPosition(new Point(getX(), getY()), 0);
                        System.out.println("Lion ID " + animalId + " cannot flee (cooldown) at tick " + currentTick);
                    }
                } else {
                    setPosition(nextPos, 0);
                }
                break;
            }
            case WANDER -> {
                if (!nextPos.equals(position)) {
                    if (canMove) {
                        setPosition(nextPos, isPregnant ? 2 : 1);
                        lastMoveTick = currentTick;
                    } else {
                        setPosition(new Point(getX(), getY()), 0);
                    }
                } else {
                    setPosition(nextPos, 0);
                }

            }

        }
    }

    public boolean hasReachedEndOfLife() {
        return this.getAge() > Lion_MAX_AGE;
    }

    public double getHuntingChance(int numOfAliveGoats) {
        double baseChance;
        if (numOfAliveGoats <= 10) baseChance = 0.0;
        else if (numOfAliveGoats <= 15) baseChance = 0.3;
        else if (numOfAliveGoats <= 25) baseChance = 0.6;
        else if (numOfAliveGoats <= 30) baseChance = 0.8;
        else baseChance = 0.95;
        return baseChance * (huntingPower / 9.5);
    }

    @Override
    protected int getPregnancyDuration() {
        return 4; // 12 ticks = 12 seconds
    }

    public boolean attemptHunting(int numOfAliveGoats) {

        return Math.random() < getHuntingChance(numOfAliveGoats);
    }

    public int getReproductionEnergyCost(Gender gender) {
        return gender == Gender.FEMALE ? 10 : 7;
    };

    public int getReproductionCooldown(Gender gender) {
        // Use reproductionPower to adjust cooldown
        int baseCooldown = gender == Gender.FEMALE ? 12 : 2;
        return (int)(baseCooldown / (reproductionPower) / 5.0);
    }

    public Animal createBaby(int x, int y) {
        return new Lion(x, y);
    };

    public int getInitialBabyEnergy() {
        return 30;
    };

    @Override
    public boolean isFertile(int currentTick) {
        boolean sinceLastReproduce = currentTick
                - this.lastReproductionTick >= getReproductionCooldown(this.getGender());
        boolean hasEnergy = this.energyLevel >= getReproductionEnergyCost(this.getGender());
        return sinceLastReproduce && hasEnergy;
    }

    @Override
    public int moveCooldown(){
        double speed=dna.get("speed");
        // at the moment lions are slower than Goats
        return (int)Math.max(4,10-speed);

    }

}
