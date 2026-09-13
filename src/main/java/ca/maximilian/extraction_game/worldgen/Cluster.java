package ca.maximilian.extraction_game.worldgen;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.CoordinateUtil;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import java.util.*;

public class Cluster {
    private final String id;
    private final Schematic schematic;
    private final Set<Connector> connectors;
    private final int multiTileWidth;
    private final int multiTileHeight;
    private final int multiTileLayers;
    private final double weight;
    private final boolean starting;
    private final Rotation defaultRotation;
    private final int minAppearances;
    private final int maxAppearances;
    private final int minSpacing;

    public Cluster(String id, Schematic schematic, Set<Connector> connectors, int multiTileWidth, int multiTileHeight, int multiTileLayers, double weight, boolean starting, Rotation defaultRotation) {
        this(id, schematic, connectors, multiTileWidth, multiTileHeight, multiTileLayers, weight, starting, defaultRotation, -1, -1, 0);
    }

    /** Full constructor, including optional per-cluster spawn count limits (-1 means unset / no limit) and minSpacing (0 means unset). */
    public Cluster(String id, Schematic schematic, Set<Connector> connectors, int multiTileWidth, int multiTileHeight,
                   int multiTileLayers, double weight, boolean starting, Rotation defaultRotation,
                   int minAppearances, int maxAppearances, int minSpacing) {
        this.id = id;
        this.schematic = schematic;
        this.connectors = connectors != null ? connectors : new HashSet<>();
        this.multiTileWidth = multiTileWidth;
        this.multiTileHeight = multiTileHeight;
        this.multiTileLayers = multiTileLayers;
        this.weight = weight;
        this.starting = starting;
        this.defaultRotation = defaultRotation;
        this.minAppearances = minAppearances;
        this.maxAppearances = maxAppearances;
        this.minSpacing = minSpacing;
    }

    public String getId() { return id; }
    public Schematic getSchematic() { return schematic; }
    public Set<Connector> getConnectors() { return connectors; }

    public List<Connector> getRotatedConnectors(Rotation rot) {
        List<Connector> result = new ArrayList<>();
        for (Connector c : connectors) {
            Point newPos = CoordinateUtil.rotatePos(new Vec(c.position().blockX(), c.position().blockY(), c.position().blockZ()), rot);
            Direction newDir = c.direction().rotate(rot);
            result.add(new Connector(newDir, new Vec(newPos.blockX(), newPos.blockY(), newPos.blockZ())));
        }
        return result;
    }

    public int getMultiTileWidth() { return multiTileWidth; }
    public int getMultiTileHeight() { return multiTileHeight; }
    public int getMultiTileLayers() { return multiTileLayers; }
    public double getWeight() { return weight; }
    public boolean isStarting() { return starting; }
    public Rotation getDefaultRotation() { return defaultRotation; }
    public int getMinAppearances() { return minAppearances; }
    public int getMaxAppearances() { return maxAppearances; }
    public boolean hasMinAppearances() { return minAppearances > 0; }
    public boolean hasMaxAppearances() { return maxAppearances > 0; }
    public int getMinSpacing() { return minSpacing; }

    public int getFootprintWidth(Rotation rot) {
        boolean swapped = rot == Rotation.CLOCKWISE_90 || rot == Rotation.CLOCKWISE_270;
        return swapped ? multiTileHeight : multiTileWidth;
    }

    public int getFootprintHeight(Rotation rot) {
        boolean swapped = rot == Rotation.CLOCKWISE_90 || rot == Rotation.CLOCKWISE_270;
        return swapped ? multiTileWidth : multiTileHeight;
    }

    public int getFootprintLayers() { return multiTileLayers; }

    public int[] getRotatedBounds(Rotation rot) {
        int width = Math.toIntExact(Math.round(schematic.size().x()));
        int length = Math.toIntExact(Math.round(schematic.size().z()));
        Point c1 = CoordinateUtil.rotatePos(new Vec(0, 0, 0), rot);
        Point c2 = CoordinateUtil.rotatePos(new Vec(width - 1, 0, 0), rot);
        Point c3 = CoordinateUtil.rotatePos(new Vec(0, 0, length - 1), rot);
        Point c4 = CoordinateUtil.rotatePos(new Vec(width - 1, 0, length - 1), rot);
        int minRelX = (int) Math.min(Math.min(c1.x(), c2.x()), Math.min(c3.x(), c4.x()));
        int maxRelX = (int) Math.max(Math.max(c1.x(), c2.x()), Math.max(c3.x(), c4.x()));
        int minRelZ = (int) Math.min(Math.min(c1.z(), c2.z()), Math.min(c3.z(), c4.z()));
        int maxRelZ = (int) Math.max(Math.max(c1.z(), c2.z()), Math.max(c3.z(), c4.z()));
        return new int[]{minRelX, maxRelX, minRelZ, maxRelZ};
    }

    public int[] getRotatedMinOffset(Rotation rot) {
        int[] b = getRotatedBounds(rot);
        return new int[]{b[0], b[2]};
    }

    public record FootprintCell(int fx, int fz) {}

    public Map<FootprintCell, Set<Direction>> getFootprintSockets(Rotation rot, int cellStep) {
        int fw = getFootprintWidth(rot);
        int fh = getFootprintHeight(rot);
        Map<FootprintCell, Set<Direction>> map = new HashMap<>();
        for (int fx = 0; fx < fw; fx++) {
            for (int fz = 0; fz < fh; fz++) {
                map.put(new FootprintCell(fx, fz), EnumSet.noneOf(Direction.class));
            }
        }

        int[] minOffset = getRotatedMinOffset(rot);
        for (Connector c : getRotatedConnectors(rot)) {
            int relX = c.position().blockX() - minOffset[0];
            int relZ = c.position().blockZ() - minOffset[1];
            int fx = Math.min(fw - 1, Math.max(0, relX / cellStep));
            int fz = Math.min(fh - 1, Math.max(0, relZ / cellStep));
            map.get(new FootprintCell(fx, fz)).add(c.direction());
        }
        return map;
    }

    public List<Vec> getRotatedBlockPositions(Rotation rot) {
        List<Vec> rotated = new ArrayList<>();
        schematic.forEachBlock((pos, block) -> {
            Point newPos = CoordinateUtil.rotatePos(new Vec(pos.blockX(), pos.blockY(), pos.blockZ()), rot);
            rotated.add(new Vec(newPos.blockX(), newPos.blockY(), newPos.blockZ()));
        });
        rotated.sort(Comparator.comparingInt((Point p) -> p.blockX())
                .thenComparingInt(p -> p.blockY())
                .thenComparingInt(p -> p.blockZ()));
        return rotated;
    }
}