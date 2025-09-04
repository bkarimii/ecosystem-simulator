package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import uk.co.cpsd.javaproject1.DecisionInfo.DecisionType;

import java.awt.Point;

public class Goat extends Animal {

    public final int HUNGER_THRESHOLDS = 30;
    public final int GOAT_MAX_AGE = 60;

    public Goat(int x, int y) {
        super(x, y, 40);
        this.setLastReproductionTick(0);
    }

    public void eatGrass() {
        this.energyLevel += 20;
        System.out.println(energyLevel+"<=====Energy level====");
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
        DecisionInfo decisionInfo = animalDecisionMaking(world);

        switch (decisionInfo.type()) {
            case EAT:
                if (world.hasGrass(decisionInfo.nextPos().x, decisionInfo.nextPos().y)) {
                    eatGrass();
                    world.removeGrass(decisionInfo.nextPos().x, decisionInfo.nextPos().y);
                    setPosition(decisionInfo.nextPos(), 1);
                    ;
                }
                break;
            case REPRODUCE:
                Point partnerLocation = decisionInfo.nextPos();
                Animal partnerGoat = world.getAnimalAt(partnerLocation.x, partnerLocation.y);

                if (partnerGoat instanceof Goat otherGoat && this.willMate(otherGoat, world.getTotalTicks())) {
                    Animal babyGoat = this.reproduceWithTwo(otherGoat, world.getTotalTicks());
                    babyAnimalHolder.add(babyGoat);
                }
                break;
            case FLEE:
                Point safeRandomPoint = decisionInfo.nextPos();
                setPosition(safeRandomPoint, 5);
                break;
            case WANDER:
                Point randomMove = decisionInfo.nextPos();
                setPosition(randomMove, 1);
                break;

        }
    }

    @Override
    public DecisionInfo animalDecisionMaking(World world) {

        Map<Point, List<Object>> scanedNeighbourHoodByGoat = world.scanNeighbour(getX(), getY());

        // 1. Priority: Flee from danger
        Point safe = findRandomSafePos(scanedNeighbourHoodByGoat);
        if (!safe.equals(new Point(getX(), getY()))) {
            return new DecisionInfo(DecisionType.FLEE, safe);
        }

        // 2. Priority: Eat if hungry
        if (isHungry()) {
            for (Map.Entry<Point, List<Object>> entry : scanedNeighbourHoodByGoat.entrySet()) {
                if (entry.getValue().contains("grass")) {
                    return new DecisionInfo(DecisionType.EAT, entry.getKey());
                }
            }
        }

        // 3. Priority: Reproduce (check nearby goats)
        for (Map.Entry<Point, List<Object>> entry : scanedNeighbourHoodByGoat.entrySet()) {
            for (Object obj : entry.getValue()) {
                if (obj instanceof Goat otherGoat && this.willMate(otherGoat, world.getTotalTicks())) {
                    return new DecisionInfo(DecisionType.REPRODUCE, entry.getKey());
                }
            }
        }

        // 4. Default: Random move
        Point randomMove = findRandomPos(scanedNeighbourHoodByGoat);
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
    public int getReproductionEnergyCost(Gender gender) {
        return gender == Gender.FEMALE ? 7 : 5;

    }

    @Override
    public int getInitialBabyEnergy() {
        return 10;
    }

    @Override
    public Animal createBaby(int x, int y) {
        return new Goat(x, y);
    }

    @Override
    public int getReproductionCooldown(Gender gender) {
        return gender == Gender.FEMALE ? 4 : 2;
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
