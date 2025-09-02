package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import uk.co.cpsd.javaproject1.DecisionInfo.DecisionType;

import java.awt.Point;

public class Goat extends Animal {

    public final int HUNGER_THRESHOLDS = 30;
    public final int GOAT_MAX_AGE = 60;
    private double fleeingPower;

    public Goat(int x, int y) {
        super(x, y, 40);
        this.setLastReproductionTick(0);
        Random random = new Random();
        if (!dna.hasTraits("fleeingPower")) {
            dna.setTrait("fleeingPower", 7.0 + new Random().nextDouble() * 2 - 1); // Random 6-8
        }
        fleeingPower = dna.getTrait("fleeingPower", Double.class);
    }

    public void eatGrass() {
        this.energyLevel += 20;
//        System.out.println(energyLevel+"<=====Energy level====");
    }

    @Override
    public Color getColor() {
        return Color.RED;
    }

    @Override
    public boolean isHungry() {
        return this.energyLevel < HUNGER_THRESHOLDS;
    }

    @Override
    public void act(World world, List<Animal> babyAnimalHolder, List<Animal> removedAnimalsHolder) {
        // Handle pregnancy before other actions
        handlePregnancy(world, babyAnimalHolder);

        DecisionInfo decisionInfo = animalDecisionMaking(world);
        int currentTick=world.getTotalTicks();
        boolean canMove= lastMoveTick==-1 || (currentTick-lastMoveTick)>moveCooldown();
        Point nextPos=decisionInfo.nextPos();
        switch (decisionInfo.type()) {
            case EAT:
                if (world.hasGrass(nextPos.x, nextPos.y)) {

                    if (!nextPos.equals(position)) {
                        // Check if goat can move due to cooldown
                        if (canMove) {
                            eatGrass();
                            world.removeGrass(nextPos.x, nextPos.y);
                            setPosition(nextPos, isPregnant ? 2 : 1);
                            lastMoveTick = currentTick;
                            System.out.println("Goat ID " + animalId + " moved to eat at (" + nextPos.x + "," + nextPos.y + ") at tick " + currentTick);
                        } else {
                            setPosition(new Point(getX(), getY()), 0); // Stay put
                            System.out.println("Goat ID " + animalId + " cannot move (cooldown) at tick " + currentTick);
                        }
                    } else {
                        eatGrass();
                        world.removeGrass(nextPos.x, nextPos.y);
                        setPosition(nextPos, 0); // No move, no cost
                    }
                }
                break;
            case REPRODUCE:
                Point partnerLocation = decisionInfo.nextPos();
                Animal partnerGoat = world.getAnimalAt(partnerLocation.x, partnerLocation.y);

                if (partnerGoat instanceof Goat otherGoat && this.willMate(otherGoat, world.getTotalTicks())) {
                    this.reproduceWith(otherGoat, world.getTotalTicks());
                }
                break;
            case FLEE:
                if (!nextPos.equals(position)) {
                    if (canMove) {
                        setPosition(nextPos, isPregnant ? 7 : 5);
                        lastMoveTick = currentTick;
                        System.out.println("Goat ID " + animalId + " at tick " + currentTick);
                    } else {
                        setPosition(new Point(getX(), getY()), 0);
                        System.out.println("Goat ID " + animalId + " cannot flee at tick " + currentTick);
                    }
                } else {
                    setPosition(nextPos, 0); // No move, no cost
                }
                break;
            case WANDER:
                if (!nextPos.equals(position)) {
                    if (canMove) {
                        setPosition(nextPos, isPregnant ? 2 : 1);
                        lastMoveTick = currentTick;
                        System.out.println("Goat ID " + animalId + " wandered to (" + nextPos.x + "," + nextPos.y + ") at tick " + currentTick);
                    } else {
                        setPosition(new Point(getX(), getY()), 0);
                        System.out.println("Goat ID " + animalId + " cannot wander (cooldown) at tick " + currentTick);
                    }
                } else {
                    setPosition(nextPos, 0); // Stay in place
                }
                break;

        }
    }

    @Override
    public DecisionInfo animalDecisionMaking(World world) {

        Map<Point, List<Object>> scannedNeighbourHoodByGoat = world.scanNeighbour(getX(), getY());

        // 1. Priority: Flee from danger
        Point safe = findRandomSafePos(scannedNeighbourHoodByGoat);
        if (!safe.equals(new Point(getX(), getY()))) {
            return new DecisionInfo(DecisionType.FLEE, safe);
        }

        // 2. Priority: Eat if hungry
        if (isHungry()) {
            for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByGoat.entrySet()) {
                if (entry.getValue().contains("grass")) {
                    return new DecisionInfo(DecisionType.EAT, entry.getKey());
                }
            }
        }

        // 3. Priority: Reproduce (check nearby goats)
        for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByGoat.entrySet()) {
            for (Object obj : entry.getValue()) {
                if (obj instanceof Goat otherGoat && this.willMate(otherGoat, world.getTotalTicks())) {
                    return new DecisionInfo(DecisionType.REPRODUCE, entry.getKey());
                }
            }
        }

        //4. Score tiles for wandering (grass=8 , safe tile preferred, lion=-10)
        Point bestMove = null;
        double bestScore = -1;
        for (Map.Entry<Point, List<Object>> entry : scannedNeighbourHoodByGoat.entrySet()) {
            double score = 0;
            List<Object> objectsAtTile = entry.getValue();
            if (objectsAtTile.contains("grass")) {
                score += 8;
            }
            boolean hasLion = objectsAtTile.stream().anyMatch(obj -> obj instanceof Lion);
            if (hasLion) {
                score -= 10;
            } else {
                score += fleeingPower;
            }
            for (Object obj : objectsAtTile) {
                if (obj instanceof Goat) {
                    score += 6; // Preference to stay near other goats
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMove = entry.getKey();
            }
        }


        // 5. Default: Random move
        Point randomMove = findRandomPos(scannedNeighbourHoodByGoat);
        return new DecisionInfo(DecisionType.WANDER, randomMove);
    }

    public Point findRandomSafePos(Map<Point, List<Object>> neighbourHoodPos) {
        Point lionPos = null;
        // find the position of the lion
        for (Map.Entry<Point, List<Object>> entry : neighbourHoodPos.entrySet()) {
            for (Object obj : entry.getValue()) {
                if (obj instanceof Lion) {
                    lionPos = entry.getKey();
                    break;
                }
            }
            if (lionPos != null) {
                break;
            }
        }

        if (lionPos == null) {
            return new Point(this.getX(), this.getY());
        }

        // find the safest tile
        Point safestTile = null;
        double maxDistance = -1;

        for (Point potentialTile : neighbourHoodPos.keySet()) {
            boolean hasLion = false;
            List<Object> objectsAtTile = neighbourHoodPos.get(potentialTile);
            if (objectsAtTile != null) {
                for (Object obj : objectsAtTile) {
                    if (obj instanceof Lion) {
                        hasLion = true;
                        break;
                    }
                }
            }

            if (!hasLion) {
                double distance = potentialTile.distanceSq(lionPos); // distanceSq is faster than distance
                if (distance > maxDistance) {
                    maxDistance = distance;
                    safestTile = potentialTile;
                }
            }
        }

        return safestTile != null ? safestTile : new Point(this.getX(), this.getY());
    }

    public Point findRandomPos(Map<Point, List<Object>> neighbourHoodPos) {
        List<Point> allTiles = new ArrayList<>(neighbourHoodPos.keySet());
        if (!allTiles.isEmpty()) {
            return allTiles.get(new Random().nextInt(allTiles.size()));
        }
        return new Point(this.getX(), this.getY());
    }

    @Override
    protected int getPregnancyDuration() {
        return 5; // 5 ticks = 5 seconds
    }

    @Override
    public int getReproductionEnergyCost(Gender gender) {
        return gender == Gender.FEMALE ? 7 : 5;

    }

    @Override
    public int moveCooldown(){
        double speed=dna.getTrait("speed",Double.class);
        return (int)Math.max(3,10-speed);

    }

    @Override
    public int getInitialBabyEnergy() {
        return 10;
    }

    @Override
    public Animal createBaby(int x, int y) {
        return new Goat(x, y);
    }

//    @Override
//    public int getReproductionCooldown(Gender gender) {
//        return gender == Gender.FEMALE ? 4 : 2;
//    }

    @Override
    public int getReproductionCooldown(Gender gender) {
        // Strong Males can reproduct soon
        return gender==Gender.FEMALE?12:4-(int)(reproductionPower-5);
    }

    public boolean hasReachedEndOfLife() {
        return this.getAge() > GOAT_MAX_AGE;
    }

    @Override
    public boolean isFertile(int currentTick) {

        boolean sinceLastReproduce = currentTick
                - this.lastReproductionTick >= getReproductionCooldown(this.getGender());
        boolean hasEnergy = this.energyLevel >= getReproductionEnergyCost(this.getGender());
        return sinceLastReproduce && hasEnergy;
    }

}
