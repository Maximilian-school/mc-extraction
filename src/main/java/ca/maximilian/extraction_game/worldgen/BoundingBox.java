package ca.maximilian.extraction_game.worldgen;

import net.minestom.server.coordinate.Point;

public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public boolean intersects(BoundingBox other) {
        return this.minX <= other.maxX && this.maxX >= other.minX &&
               this.minY <= other.maxY && this.maxY >= other.minY &&
               this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    /**
     * Checks if two boxes overlap in volume (interior overlap > 0),
     * meaning touching faces (where max == min) do not count as a collision.
     */
    public boolean overlapsVolume(BoundingBox other) {
        return this.minX < other.maxX && this.maxX > other.minX &&
               this.minY < other.maxY && this.maxY > other.minY &&
               this.minZ < other.maxZ && this.maxZ > other.minZ;
    }

    public boolean contains(Point point) {
        return point.blockX() >= minX && point.blockX() <= maxX &&
               point.blockY() >= minY && point.blockY() <= maxY &&
               point.blockZ() >= minZ && point.blockZ() <= maxZ;
    }
}
