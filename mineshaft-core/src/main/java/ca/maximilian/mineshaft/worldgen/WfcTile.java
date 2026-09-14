package ca.maximilian.mineshaft.worldgen;

import net.hollowcube.schem.util.Rotation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public record WfcTile(
        String id,
        MineshaftSegment segment,
        Cluster cluster,
        Rotation rotation,
        Set<Direction> openSockets,
        double weight,
        TileType type,
        int footprintOffsetX,
        int footprintOffsetZ,
        int footprintOffsetLayer
) {
    public enum TileType {
        STANDARD,
        EMPTY,
        STAIRWELL_BOTTOM,
        STAIRWELL_TOP,
        MULTI_TILE_PART
    }

    public boolean isOpen(Direction direction) {
        return openSockets.contains(direction);
    }

    public boolean isEmpty() {
        return type == TileType.EMPTY;
    }

    public boolean isVerticalBottom() {
        return type == TileType.STAIRWELL_BOTTOM;
    }

    public boolean isVerticalTop() {
        return type == TileType.STAIRWELL_TOP;
    }

    public boolean isMultiTilePart() {
        return type == TileType.MULTI_TILE_PART;
    }

    /**
     * True only for the single footprint cell (0,0,0 offset) in a cluster
     * placement that should actually paint the schematic. All other cells
     * occupied by the same placement are non-painting "filler" reservations.
     */
    public boolean isFootprintAnchor() {
        return type == TileType.MULTI_TILE_PART
                && footprintOffsetX == 0 && footprintOffsetZ == 0 && footprintOffsetLayer == 0;
    }

    public boolean isHorizontalCompatible(WfcTile neighborTile, Direction towardsNeighbor) {
        if (neighborTile == null) {
            return !isOpen(towardsNeighbor);
        }
        Direction incoming = towardsNeighbor.opposite();
        return this.isOpen(towardsNeighbor) == neighborTile.isOpen(incoming);
    }

    public boolean isCompatible(WfcTile neighborTile, Direction towardsNeighbor) {
        return isHorizontalCompatible(neighborTile, towardsNeighbor);
    }

    public static boolean isVerticalCompatible(WfcTile bottomTile, WfcTile topTile) {
        if (bottomTile.type == TileType.STAIRWELL_BOTTOM) {
            return topTile.type == TileType.STAIRWELL_TOP
                    && bottomTile.segment() != null && bottomTile.segment() == topTile.segment()
                    && bottomTile.rotation() == topTile.rotation();
        } else {
            return topTile.type != TileType.STAIRWELL_TOP;
        }
    }

    public static WfcTile createEmptyTile(double weight) {
        return new WfcTile("empty", null, null, Rotation.NONE, EnumSet.noneOf(Direction.class), weight, TileType.EMPTY, 0, 0, 0);
    }

    public static WfcTile fromSegment(MineshaftSegment segment, Rotation rotation, double weight) {
        Set<Direction> openDirs = EnumSet.noneOf(Direction.class);
        for (Connector c : segment.getRotatedConnectors(rotation)) {
            openDirs.add(c.direction());
        }
        String tileId = segment.getId() + "_" + rotation.name();
        return new WfcTile(tileId, segment, null, rotation, openDirs, weight, TileType.STANDARD, 0, 0, 0);
    }

    /**
     * The anchor tile for a cluster placement: only exposes sockets that
     * physically sit on footprint cell (0,0) of the rotated schematic.
     * Sockets on other footprint cells are exposed via createClusterFiller
     * tiles once the placement is actually reserved.
     */
    public static WfcTile fromCluster(Cluster cluster, Rotation rotation, double weight, int cellStep) {
        Map<Cluster.FootprintCell, Set<Direction>> sockets = cluster.getFootprintSockets(rotation, cellStep);
        Set<Direction> anchorSockets = sockets.getOrDefault(new Cluster.FootprintCell(0, 0), EnumSet.noneOf(Direction.class));
        String tileId = cluster.getId() + "_" + rotation.name();
        return new WfcTile(tileId, null, cluster, rotation, anchorSockets, weight, TileType.MULTI_TILE_PART, 0, 0, 0);
    }

    /**
     * A reserved-but-non-painting cell belonging to an already-placed cluster.
     * fx/fz/layerOffset identify which footprint cell this is, relative to the
     * anchor at (0,0,0), so generateIntoInstance can skip painting it (only the
     * anchor paints the whole schematic in one pass).
     */
    public static WfcTile createClusterFiller(Cluster cluster, Rotation rotation, Set<Direction> openSockets, int fx, int fz, int layerOffset) {
        String tileId = cluster.getId() + "_" + rotation.name() + "_fill_" + fx + "_" + fz + "_" + layerOffset;
        return new WfcTile(tileId, null, cluster, rotation, openSockets, 0.0, TileType.MULTI_TILE_PART, fx, fz, layerOffset);
    }

    public static WfcTile createStairwellBottom(MineshaftSegment segment, Rotation rotation, double weight) {
        Set<Direction> openDirs = EnumSet.noneOf(Direction.class);
        for (Connector c : segment.getRotatedConnectors(rotation)) {
            if (c.position().y() < segment.getHeight() / 2.0) {
                openDirs.add(c.direction());
            }
        }
        if (openDirs.isEmpty()) {
            openDirs.add(Direction.NORTH.rotate(rotation));
        }
        String tileId = segment.getId() + "_bottom_" + rotation.name();
        return new WfcTile(tileId, segment, null, rotation, openDirs, weight, TileType.STAIRWELL_BOTTOM, 0, 0, 0);
    }

    public static WfcTile createStairwellTop(MineshaftSegment segment, Rotation rotation, double weight) {
        Set<Direction> openDirs = EnumSet.noneOf(Direction.class);
        for (Connector c : segment.getRotatedConnectors(rotation)) {
            if (c.position().y() >= segment.getHeight() / 2.0) {
                openDirs.add(c.direction());
            }
        }
        if (openDirs.isEmpty()) {
            openDirs.add(Direction.NORTH.rotate(rotation));
        }
        String tileId = segment.getId() + "_top_" + rotation.name();
        return new WfcTile(tileId, segment, null, rotation, openDirs, weight, TileType.STAIRWELL_TOP, 0, 0, 0);
    }
}