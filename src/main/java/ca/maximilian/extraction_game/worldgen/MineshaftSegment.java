package ca.maximilian.extraction_game.worldgen;

import lombok.Getter;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.schem.util.CoordinateUtil;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import java.util.HashSet;
import java.util.Set;

@Getter
public class MineshaftSegment {
    private final String id;
    private final Schematic schematic;
    private final Set<Connector> connectors;
    private final int width;
    private final int height;
    private final int length;
    private final boolean starting;
    private final double weight;
    private final Block markerBlock;
    private final int maxConsecutiveStraight;
    private final int minSpacing;
    private final int multiTileWidth;
    private final int multiTileHeight;
    private final int multiTileLayers;
    private final int minAppearances;
    private final int maxAppearances;
    /** Ids of other segments/clusters this one is not allowed to have a doorway directly into. Mutable so SegmentLoader can attach it after construction without touching every overload below. */
    private Set<String> connectBlacklist = new HashSet<>();

    public void setConnectBlacklist(Set<String> blacklist) {
        this.connectBlacklist = blacklist != null ? blacklist : new HashSet<>();
    }

    /**
     * Ids this segment must connect to (directly or indirectly through the corridor
     * network) for a generated map to be considered valid. Empty means no requirement.
     * Same mutable-after-construction pattern as connectBlacklist above.
     */
    private Set<String> requiredConnections = new HashSet<>();
    /** -1 or 0 means "anywhere in the reachable network" (indirect is fine). A positive number caps how many rooms away the match may be, so 1 means it must be a direct neighbor. */
    private int requiredConnectionMaxDistance = -1;

    public void setRequiredConnections(Set<String> requiredConnections, int maxDistance) {
        this.requiredConnections = requiredConnections != null ? requiredConnections : new HashSet<>();
        this.requiredConnectionMaxDistance = maxDistance;
    }

    public boolean hasRequiredConnections() { return !requiredConnections.isEmpty(); }

    public MineshaftSegment(String id, Schematic schematic, Block markerBlock) {
        this(id, schematic, markerBlock, false, -1, -1, 0, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Block markerBlock, boolean starting) {
        this(id, schematic, markerBlock, starting, -1, -1, 0, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Block markerBlock, boolean starting, double weight) {
        this(id, schematic, markerBlock, starting, weight, -1, 0, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Block markerBlock, boolean starting, double weight, int maxConsecutiveStraight) {
        this(id, schematic, markerBlock, starting, weight, maxConsecutiveStraight, 0, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Block markerBlock, boolean starting, double weight, int maxConsecutiveStraight, int minSpacing, int multiTileWidth, int multiTileHeight, int multiTileLayers) {
        this(id, schematic, markerBlock, starting, weight, maxConsecutiveStraight, minSpacing, multiTileWidth, multiTileHeight, multiTileLayers, -1, -1);
    }

    /** Full constructor, including optional per segment spawn count limits (-1 means unset / no limit). */
    public MineshaftSegment(String id, Schematic schematic, Block markerBlock, boolean starting, double weight,
                            int maxConsecutiveStraight, int minSpacing, int multiTileWidth, int multiTileHeight,
                            int multiTileLayers, int minAppearances, int maxAppearances) {
        this.id = id;
        this.schematic = schematic;
        this.width = Math.toIntExact(Math.round(schematic.size().x()));
        this.height = Math.toIntExact(Math.round(schematic.size().y()));
        this.length = Math.toIntExact(Math.round(schematic.size().z()));
        this.connectors = new HashSet<>();
        this.starting = starting;
        this.weight = weight;
        this.markerBlock = markerBlock;
        this.maxConsecutiveStraight = maxConsecutiveStraight;
        this.minSpacing = minSpacing;
        this.multiTileWidth = multiTileWidth;
        this.multiTileHeight = multiTileHeight;
        this.multiTileLayers = multiTileLayers;
        this.minAppearances = minAppearances;
        this.maxAppearances = maxAppearances;

        if (markerBlock != null) {
            schematic.forEachBlock((pos, block) -> {
                if (block.compare(markerBlock)) {
                    Direction dir = resolveDirection(pos, width, length);
                    if (dir != null) {
                        connectors.add(new Connector(dir, pos));
                    }
                }
            });
        }

        if (this.connectors.isEmpty()) {
            autoDetectConnectors();
        }
    }

    private void autoDetectConnectors() {
        Set<Point> solidBlocks = new HashSet<>();
        schematic.forEachBlock((pos, block) -> {
            if (block.solid()) {
                solidBlocks.add(pos);
            }
        });

        int centerX = width / 2;
        int centerZ = length / 2;

        if (connectors.stream().noneMatch(c -> c.direction() == Direction.NORTH)) {
            if (isFaceOpen(solidBlocks, centerX, 0, Axis.Z, true)) {
                connectors.add(new Connector(Direction.NORTH, new Vec(centerX, 1, 0)));
            }
        }

        if (connectors.stream().noneMatch(c -> c.direction() == Direction.SOUTH)) {
            if (isFaceOpen(solidBlocks, centerX, length - 1, Axis.Z, true)) {
                connectors.add(new Connector(Direction.SOUTH, new Vec(centerX, 1, length - 1)));
            }
        }

        if (connectors.stream().noneMatch(c -> c.direction() == Direction.WEST)) {
            if (isFaceOpen(solidBlocks, 0, centerZ, Axis.X, false)) {
                connectors.add(new Connector(Direction.WEST, new Vec(0, 1, centerZ)));
            }
        }

        if (connectors.stream().noneMatch(c -> c.direction() == Direction.EAST)) {
            if (isFaceOpen(solidBlocks, width - 1, centerZ, Axis.X, false)) {
                connectors.add(new Connector(Direction.EAST, new Vec(width - 1, 1, centerZ)));
            }
        }
    }

    private enum Axis { X, Z }

    private boolean isFaceOpen(Set<Point> solidBlocks, int faceX, int faceZ, Axis fixedAxis, boolean isZFixed) {
        int needed = 2;
        int consecutive = 0;
        for (int y = 1; y < height - 1; y++) {
            Point p = new Vec(faceX, y, faceZ);
            if (!solidBlocks.contains(p)) {
                consecutive++;
                if (consecutive >= needed) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors) {
        this(id, schematic, connectors, false, -1, -1, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors, boolean starting) {
        this(id, schematic, connectors, starting, -1, -1, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors, boolean starting, double weight) {
        this(id, schematic, connectors, starting, weight, -1, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors, boolean starting, double weight, int maxConsecutiveStraight) {
        this(id, schematic, connectors, starting, weight, maxConsecutiveStraight, 1, 1, 1, -1, -1);
    }

    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors, boolean starting, double weight, int maxConsecutiveStraight, int multiTileWidth, int multiTileHeight, int multiTileLayers) {
        this(id, schematic, connectors, starting, weight, maxConsecutiveStraight, multiTileWidth, multiTileHeight, multiTileLayers, -1, -1);
    }

    /** Full constructor for the pre-built-connectors path, including optional spawn count limits. */
    public MineshaftSegment(String id, Schematic schematic, Set<Connector> connectors, boolean starting, double weight,
                            int maxConsecutiveStraight, int multiTileWidth, int multiTileHeight, int multiTileLayers,
                            int minAppearances, int maxAppearances) {
        this.id = id;
        this.schematic = schematic;
        this.width = Math.toIntExact(Math.round(schematic.size().x()));
        this.height = Math.toIntExact(Math.round(schematic.size().y()));
        this.length = Math.toIntExact(Math.round(schematic.size().z()));
        this.connectors = new HashSet<>(connectors);
        this.starting = starting;
        this.weight = weight;
        this.markerBlock = null;
        this.maxConsecutiveStraight = maxConsecutiveStraight;
        this.minSpacing = 0;
        this.multiTileWidth = multiTileWidth;
        this.multiTileHeight = multiTileHeight;
        this.multiTileLayers = multiTileLayers;
        this.minAppearances = minAppearances;
        this.maxAppearances = maxAppearances;
    }

    private Direction resolveDirection(Point pos, int width, int length) {
        if (pos.blockX() == 0) return Direction.WEST;
        if (pos.blockX() == width - 1) return Direction.EAST;
        if (pos.blockZ() == 0) return Direction.NORTH;
        if (pos.blockZ() == length - 1) return Direction.SOUTH;
        return null;
    }

    public Set<Connector> getRotatedConnectors(Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return connectors;
        }
        Set<Connector> rotated = new HashSet<>();
        for (Connector connector : connectors) {
            Point newPos = CoordinateUtil.rotatePos(connector.position(), rotation);
            Direction newDir = connector.direction().rotate(rotation);
            rotated.add(new Connector(newDir, newPos));
        }
        return rotated;
    }

    public BoundingBox getBoundingBox(Point basePos, Rotation rotation) {
        Point c1 = CoordinateUtil.rotatePos(new Vec(0, 0, 0), rotation);
        Point c2 = CoordinateUtil.rotatePos(new Vec(width - 1, 0, 0), rotation);
        Point c3 = CoordinateUtil.rotatePos(new Vec(0, 0, length - 1), rotation);
        Point c4 = CoordinateUtil.rotatePos(new Vec(width - 1, 0, length - 1), rotation);

        int minRelX = (int) Math.min(Math.min(c1.x(), c2.x()), Math.min(c3.x(), c4.x()));
        int maxRelX = (int) Math.max(Math.max(c1.x(), c2.x()), Math.max(c3.x(), c4.x()));
        int minRelZ = (int) Math.min(Math.min(c1.z(), c2.z()), Math.min(c3.z(), c4.z()));
        int maxRelZ = (int) Math.max(Math.max(c1.z(), c2.z()), Math.max(c3.z(), c4.z()));

        int minX = basePos.blockX() + minRelX;
        int maxX = basePos.blockX() + maxRelX;
        int minY = basePos.blockY();
        int maxY = basePos.blockY() + height - 1;
        int minZ = basePos.blockZ() + minRelZ;
        int maxZ = basePos.blockZ() + maxRelZ;

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean canConnect(MineshaftSegment other, Direction side) {
        return this.connectors.stream().anyMatch(c -> c.direction() == side) &&
                other.connectors.stream().anyMatch(c -> c.direction() == side.opposite());
    }

    public static MineshaftSegment createDefaultStartingSegment() {
        return createDefaultStartingSegment(Block.BEACON);
    }

    public static MineshaftSegment createDefaultStartingSegment(Block markerBlock) {
        int width = 5;
        int height = 5;
        int length = 5;
        var builder = SchematicBuilder.builder(new Vec(width, height, length));

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                builder.block(x, 0, z, Block.COBBLESTONE);
                builder.block(x, height - 1, z, Block.OAK_PLANKS);
            }
        }

        int midX = width / 2;
        int midZ = length / 2;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                builder.block(x, y, 0, (x == midX && y <= 2) ? Block.AIR : Block.COBBLESTONE);
                builder.block(x, y, length - 1, (x == midX && y <= 2) ? Block.AIR : Block.COBBLESTONE);
            }
            for (int z = 0; z < length; z++) {
                builder.block(0, y, z, (z == midZ && y <= 2) ? Block.AIR : Block.COBBLESTONE);
                builder.block(width - 1, y, z, (z == midZ && y <= 2) ? Block.AIR : Block.COBBLESTONE);
            }
        }

        if (markerBlock != null) {
            builder.block(0, 1, midZ, markerBlock);
            builder.block(width - 1, 1, midZ, markerBlock);
            builder.block(midX, 1, 0, markerBlock);
            builder.block(midX, 1, length - 1, markerBlock);
        }

        return new MineshaftSegment("starting_room", builder.build(), markerBlock, true, -1, -1);
    }

    public static int getMaxCellStep(java.util.List<MineshaftSegment> segments) {
        int maxDim = 0;
        for (MineshaftSegment s : segments) {
            if (!WaveFunctionCollapse.isStairwellSegment(s)) {
                maxDim = Math.max(maxDim, Math.max(s.getWidth(), s.getLength()));
            }
        }
        return maxDim > 0 ? maxDim : 5;
    }

    public static int getStandardFloorHeight(java.util.List<MineshaftSegment> segments) {
        return 5;
    }

    public boolean isStartingSegment() { return starting; }

    public boolean isMultiTile() { return multiTileWidth > 1 || multiTileHeight > 1 || multiTileLayers > 1; }

    public boolean hasMinAppearances() { return minAppearances > 0; }
    public boolean hasMaxAppearances() { return maxAppearances > 0; }
}