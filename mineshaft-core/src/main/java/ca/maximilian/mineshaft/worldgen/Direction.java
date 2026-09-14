package ca.maximilian.mineshaft.worldgen;

import net.hollowcube.schem.util.Rotation;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public Direction rotate(Rotation rotation) {
        return switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
            case CLOCKWISE_180 -> switch (this) {
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case SOUTH -> NORTH;
                case WEST -> EAST;
            };
            case CLOCKWISE_270 -> switch (this) {
                case NORTH -> WEST;
                case EAST -> NORTH;
                case SOUTH -> EAST;
                case WEST -> SOUTH;
            };
        };
    }
}