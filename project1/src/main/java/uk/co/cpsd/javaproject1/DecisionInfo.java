package uk.co.cpsd.javaproject1;

import java.awt.Point;

public record DecisionInfo(DecisionType type, Point nextPos) {

    public DecisionInfo(DecisionType type, Point nextPos) {
        if (type == null || nextPos == null) {
            throw new IllegalArgumentException("Fields cannot be null");
        }

        this.type = type;
        this.nextPos = new Point(nextPos); // defensive copy
    }

    public Point nextPos() {
        return new Point(nextPos);
    }

    public enum DecisionType {
        EAT, FLEE, REPRODUCE, WANDER
    }
}