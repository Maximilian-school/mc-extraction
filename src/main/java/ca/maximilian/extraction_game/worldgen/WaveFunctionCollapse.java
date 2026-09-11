package ca.maximilian.extraction_game.worldgen;

import ca.maximilian.extraction_game.core.handler.block.BlockHandlers;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.CoordinateUtil;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.*;

public class WaveFunctionCollapse {

    private static final int MIN_DEAD_END_DISTANCE = 256;

    private final int layers;
    private final int width;
    private final int height;
    private final int cellStep;
    private final List<WfcTile> prototypes;
    private final List<MineshaftSegment> multiTileSegments;
    private final Random random;
    private final Map<String, int[]> appearanceLimits; // id -> {min, max}, -1 means unset

    public WaveFunctionCollapse(
            int width,
            int height,
            int cellStep,
            List<MineshaftSegment> availableSegments,
            Random random
    ) {
        this(
                hasVerticalSegment(availableSegments) ? 2 : 1,
                width,
                height,
                cellStep,
                availableSegments,
                random
        );
    }

    public WaveFunctionCollapse(
            int layers,
            int width,
            int height,
            int cellStep,
            List<MineshaftSegment> availableSegments,
            Random random
    ) {
        this.layers = Math.max(1, layers);
        this.width = width;
        this.height = height;
        this.cellStep = cellStep;
        this.random = random != null ? random : new Random();
        this.prototypes = new ArrayList<>();
        this.multiTileSegments = new ArrayList<>();
        this.appearanceLimits = new HashMap<>();

        for (MineshaftSegment segment : availableSegments) {
            if (segment.hasMinAppearances() || segment.hasMaxAppearances()) {
                appearanceLimits.put(segment.getId(), new int[]{segment.getMinAppearances(), segment.getMaxAppearances()});
            }
            if (isCenterSegmentId(segment.getId())) {
                // Force-placed once at the true grid center, see seedCenterCluster.
                // Overrides whatever min/max was set in config since it can only ever appear once, right here.
                appearanceLimits.put(segment.getId(), new int[]{1, 1});
            }

            double weight = segment.getWeight() >= 0 ? segment.getWeight() : getSegmentDefaultWeight(segment.getId());

            if (segment.isMultiTile()) {
                Cluster cluster = new Cluster(
                        segment.getId(),
                        segment.getSchematic(),
                        segment.getConnectors(),
                        segment.getMultiTileWidth(),
                        segment.getMultiTileHeight(),
                        segment.getMultiTileLayers(),
                        weight,
                        false,
                        Rotation.NONE,
                        segment.getMinAppearances(),
                        segment.getMaxAppearances()
                );
                multiTileSegments.add(segment);
                for (Rotation rot : Rotation.values()) {
                    prototypes.add(WfcTile.fromCluster(cluster, rot, weight, cellStep));
                }
                continue;
            }

            boolean isStairwell = isStairwellSegment(segment);
            if (isStairwell) {
                for (Rotation rot : Rotation.values()) {
                    prototypes.add(WfcTile.createStairwellBottom(segment, rot, weight));
                    prototypes.add(WfcTile.createStairwellTop(segment, rot, weight));
                }
            } else {
                for (Rotation rot : Rotation.values()) {
                    prototypes.add(WfcTile.fromSegment(segment, rot, weight));
                }
            }
        }

        this.prototypes.add(WfcTile.createEmptyTile(2.0));
    }

    private static boolean hasVerticalSegment(List<MineshaftSegment> segments) {
        return segments.stream().anyMatch(WaveFunctionCollapse::isStairwellSegment);
    }

    public static boolean isStairwellSegment(MineshaftSegment segment) {
        if (segment == null) return false;
        String lower = segment.getId().toLowerCase();
        if (lower.contains("stairwell") || lower.contains("stairs") || lower.contains("stair")) {
            return true;
        }
        return segment.getHeight() > 5;
    }

    /** Matches the special always-centered hub segment, "centre" or "center", case insensitive. */
    private static boolean isCenterSegmentId(String id) {
        if (id == null) return false;
        String lower = id.toLowerCase();
        return lower.equals("centre") || lower.equals("center");
    }

    private double getSegmentDefaultWeight(String id) {
        String lower = id.toLowerCase();
        if (lower.contains("straight")) return 4.0;
        if (lower.contains("corner")) return 4.0;
        if (lower.contains("tjunction") || lower.contains("t_junction")) return 3.5;
        if (lower.contains("crossroads")) return 3.5;
        if (lower.contains("deadend") || lower.contains("dead_end")) return 2.0;
        if (lower.contains("stairwell") || lower.contains("stairs") || lower.contains("stair")) return 4.5;
        return 3.0;
    }

    public int getLayers() {
        return layers;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public WfcTile[][] solve() {
        WfcTile[][][] res = solve3D(100);
        return res[0];
    }

    public WfcTile[][] solve(int maxAttempts) {
        WfcTile[][][] res = solve3D(maxAttempts);
        return res[0];
    }

    public WfcTile[][][] solve3D() {
        return solve3D(100);
    }

    public WfcTile[][][] solve3D(int maxAttempts) {
        return solve3D(maxAttempts, Vec.ZERO);
    }

    public WfcTile[][][] solve3D(int maxAttempts, Point worldStartPos) {
        boolean requiresVertical = layers > 1 && prototypes.stream().anyMatch(WfcTile::isVerticalBottom);
        WfcTile[][][] fallback = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            WfcTile[][][] result = runWfc(worldStartPos, false);
            if (result != null) {
                if (requiresVertical) {
                    int stairCount = 0;
                    for (int l = 0; l < layers - 1; l++) {
                        for (int x = 0; x < width; x++) {
                            for (int z = 0; z < height; z++) {
                                if (result[l][x][z].isVerticalBottom()) stairCount++;
                            }
                        }
                    }
                    if (stairCount > 0) {
                        pruneUnreachable(result);
                        return result;
                    }
                    if (fallback == null) fallback = result;
                } else {
                    pruneUnreachable(result);
                    return result;
                }
            }
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            WfcTile[][][] result = runWfc(worldStartPos, true);
            if (result != null) {
                pruneUnreachable(result);
                return result;
            }
        }

        if (fallback != null) {
            pruneUnreachable(fallback);
            return fallback;
        }

        throw new IllegalStateException("WFC failed even with relaxed boundaries after " + (maxAttempts * 2) + " attempts");
    }

    @SuppressWarnings("unchecked")
    private WfcTile[][][] runWfc(Point worldStartPos, boolean relaxBoundaries) {
        List<WfcTile>[][][] grid = new List[layers][width][height];

        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    List<WfcTile> domain = new ArrayList<>();
                    for (WfcTile tile : prototypes) {
                        if (l == 0 && tile.isVerticalTop()) continue;
                        if (l == layers - 1 && tile.isVerticalBottom()) continue;
                        domain.add(tile);
                    }
                    grid[l][x][z] = domain;
                }
            }
        }

        Queue<CellPos3D> queue = new ArrayDeque<>();
        Map<String, Integer> appearanceCounts = new HashMap<>();

        // Force the centre/center cluster into the true grid center before anything
        // else collapses. This is the anchor the rest of the map grows from, without
        // it the very first cell has no committed neighbor, so chooseWeighted's
        // "no incoming socket" branch coin-flips between empty and non-empty, which
        // is exactly what was producing sparse isolated single tiles.
        seedCenterCluster(grid, queue, appearanceCounts);

        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    if (relaxBoundaries) continue;
                    boolean changed = false;
                    List<WfcTile> current = grid[l][x][z];
                    List<WfcTile> valid = new ArrayList<>();
                    for (WfcTile tile : current) {
                        boolean ok = true;
                        if (z == 0 && tile.isOpen(Direction.NORTH)) ok = false;
                        if (z == height - 1 && tile.isOpen(Direction.SOUTH)) ok = false;
                        if (x == 0 && tile.isOpen(Direction.WEST)) ok = false;
                        if (x == width - 1 && tile.isOpen(Direction.EAST)) ok = false;

                        if (ok) {
                            valid.add(tile);
                        } else {
                            changed = true;
                        }
                    }
                    if (changed) {
                        if (valid.isEmpty()) {
                            return null;
                        }
                        grid[l][x][z] = valid;
                        queue.add(new CellPos3D(l, x, z));
                    }
                }
            }
        }

        if (!propagate(grid, queue)) {
            return null;
        }

        while (true) {
            CellPos3D nextCell = findLowestEntropyCell(grid);
            if (nextCell == null) {
                break;
            }

            List<WfcTile> domain = grid[nextCell.l()][nextCell.x()][nextCell.z()];
            if (domain.isEmpty()) {
                return null;
            }

            List<WfcTile> feasible = new ArrayList<>(domain.size());
            for (WfcTile t : domain) {
                if (isAtMaxAppearances(t, appearanceCounts)) continue;
                if (t.isFootprintAnchor()) {
                    Cluster cluster = t.cluster();
                    int fw = cluster.getFootprintWidth(t.rotation());
                    int fh = cluster.getFootprintHeight(t.rotation());
                    int flayers = cluster.getFootprintLayers();
                    Map<Cluster.FootprintCell, Set<Direction>> sockets = cluster.getFootprintSockets(t.rotation(), cellStep);
                    if (!canPlaceCluster(grid, nextCell.x(), nextCell.z(), nextCell.l(), fw, fh, flayers, sockets)) {
                        continue;
                    }
                }
                feasible.add(t);
            }
            if (feasible.isEmpty()) {
                return null;
            }

            WfcTile chosen = chooseWeighted(feasible, nextCell, grid, appearanceCounts);

            if (chosen.isFootprintAnchor()) {
                Cluster cluster = chosen.cluster();
                Rotation rot = chosen.rotation();
                int fw = cluster.getFootprintWidth(rot);
                int fh = cluster.getFootprintHeight(rot);
                int flayers = cluster.getFootprintLayers();
                Map<Cluster.FootprintCell, Set<Direction>> sockets = cluster.getFootprintSockets(rot, cellStep);
                reserveCluster(grid, queue, cluster, rot, chosen, nextCell.x(), nextCell.z(), nextCell.l(), sockets, fw, fh, flayers);
                recordAppearance(chosen, appearanceCounts);
            } else {
                grid[nextCell.l()][nextCell.x()][nextCell.z()] = new ArrayList<>(List.of(chosen));
                queue.add(nextCell);
                recordAppearance(chosen, appearanceCounts);
            }

            if (!propagate(grid, queue)) {
                return null;
            }
        }

        WfcTile[][][] result = new WfcTile[layers][width][height];
        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    result[l][x][z] = grid[l][x][z].get(0);
                }
            }
        }
        return result;
    }

    /**
     * Finds the prototype anchor tile for the segment whose id is "centre" or
     * "center" and forcibly reserves its footprint dead center in the grid,
     * using the same reservation path a normal cluster placement would use.
     * Returns false (no-op) if no such segment exists in the loaded config,
     * so this is entirely opt in.
     */
    private boolean seedCenterCluster(List<WfcTile>[][][] grid, Queue<CellPos3D> queue, Map<String, Integer> appearanceCounts) {
        WfcTile anchor = prototypes.stream()
                .filter(t -> t.isFootprintAnchor() && t.cluster() != null
                        && isCenterSegmentId(t.cluster().getId()) && t.rotation() == Rotation.NONE)
                .findFirst()
                .orElse(null);
        if (anchor == null) return false;

        Cluster cluster = anchor.cluster();
        Rotation rot = anchor.rotation();
        int fw = cluster.getFootprintWidth(rot);
        int fh = cluster.getFootprintHeight(rot);
        int flayers = cluster.getFootprintLayers();

        if (fw > width || fh > height || flayers > layers) {
            System.err.println("[WFC] centre segment footprint (" + fw + "x" + fh + "x" + flayers
                    + ") does not fit the " + width + "x" + height + "x" + layers + " grid, skipping forced placement");
            return false;
        }

        int anchorX = width / 2 - fw / 2;
        int anchorZ = height / 2 - fh / 2;
        int anchorL = 0;

        Map<Cluster.FootprintCell, Set<Direction>> sockets = cluster.getFootprintSockets(rot, cellStep);
        reserveCluster(grid, queue, cluster, rot, anchor, anchorX, anchorZ, anchorL, sockets, fw, fh, flayers);
        recordAppearance(anchor, appearanceCounts);
        return true;
    }

    private boolean canPlaceCluster(List<WfcTile>[][][] grid, int x, int z, int l, int fw, int fh, int flayers,
                                    Map<Cluster.FootprintCell, Set<Direction>> sockets) {
        if (x + fw > width || z + fh > height || l + flayers > layers) return false;
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                for (int k = 0; k < flayers; k++) {
                    boolean isAnchor = (i == 0 && j == 0 && k == 0);
                    int gx = x + i, gz = z + j, gl = l + k;
                    List<WfcTile> cellDomain = grid[gl][gx][gz];
                    if (!isAnchor && cellDomain.size() == 1) {
                        return false;
                    }
                    Set<Direction> fillerSockets = sockets.getOrDefault(new Cluster.FootprintCell(i, j), EnumSet.noneOf(Direction.class));
                    for (Direction dir : Direction.values()) {
                        int ni = i + getDirX(dir), nj = j + getDirZ(dir);
                        if (ni >= 0 && ni < fw && nj >= 0 && nj < fh) continue;

                        int outX = gx + getDirX(dir), outZ = gz + getDirZ(dir);
                        boolean open = fillerSockets.contains(dir);
                        if (outX < 0 || outX >= width || outZ < 0 || outZ >= height) {
                            if (open) return false;
                            continue;
                        }
                        List<WfcTile> outsideDomain = grid[gl][outX][outZ];
                        if (outsideDomain.size() == 1) {
                            boolean outsideOpen = outsideDomain.get(0).isOpen(dir.opposite());
                            if (outsideOpen != open) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void reserveCluster(List<WfcTile>[][][] grid, Queue<CellPos3D> queue, Cluster cluster, Rotation rot,
                                WfcTile anchorTile, int x, int z, int l,
                                Map<Cluster.FootprintCell, Set<Direction>> sockets, int fw, int fh, int flayers) {
        for (int i = 0; i < fw; i++) {
            for (int j = 0; j < fh; j++) {
                for (int k = 0; k < flayers; k++) {
                    int gx = x + i, gz = z + j, gl = l + k;
                    WfcTile tile = (i == 0 && j == 0 && k == 0)
                            ? anchorTile
                            : WfcTile.createClusterFiller(cluster, rot,
                            sockets.getOrDefault(new Cluster.FootprintCell(i, j), EnumSet.noneOf(Direction.class)),
                            i, j, k);
                    grid[gl][gx][gz] = new ArrayList<>(List.of(tile));
                    queue.add(new CellPos3D(gl, gx, gz));
                }
            }
        }
    }

    private boolean requireConnection(List<WfcTile>[][][] grid, Queue<CellPos3D> queue,
                                      int floor, int x, int z, Direction direction) {
        int nx = x + getDirX(direction);
        int nz = z + getDirZ(direction);
        grid[floor][x][z].removeIf(tile -> !tile.isOpen(direction));
        grid[floor][nx][nz].removeIf(tile -> !tile.isOpen(direction.opposite()));
        if (grid[floor][x][z].isEmpty() || grid[floor][nx][nz].isEmpty()) return false;
        queue.add(new CellPos3D(floor, x, z));
        queue.add(new CellPos3D(floor, nx, nz));
        return true;
    }

    private boolean propagate(List<WfcTile>[][][] grid, Queue<CellPos3D> queue) {
        while (!queue.isEmpty()) {
            CellPos3D curr = queue.poll();
            List<WfcTile> currDomain = grid[curr.l()][curr.x()][curr.z()];

            for (Direction dir : Direction.values()) {
                int nx = curr.x() + getDirX(dir);
                int nz = curr.z() + getDirZ(dir);

                if (nx < 0 || nx >= width || nz < 0 || nz >= height) continue;

                List<WfcTile> neighborDomain = grid[curr.l()][nx][nz];
                List<WfcTile> validNeighbors = new ArrayList<>();
                boolean changed = false;

                for (WfcTile neighborTile : neighborDomain) {
                    boolean compatibleWithAtLeastOne = false;
                    for (WfcTile currTile : currDomain) {
                        if (currTile.isHorizontalCompatible(neighborTile, dir)) {
                            compatibleWithAtLeastOne = true;
                            break;
                        }
                    }

                    if (compatibleWithAtLeastOne) {
                        validNeighbors.add(neighborTile);
                    } else {
                        changed = true;
                    }
                }

                if (changed) {
                    if (validNeighbors.isEmpty()) {
                        return false;
                    }
                    grid[curr.l()][nx][nz] = validNeighbors;
                    queue.add(new CellPos3D(curr.l(), nx, nz));
                }
            }

            if (curr.l() > 0) {
                int belowL = curr.l() - 1;
                List<WfcTile> belowDomain = grid[belowL][curr.x()][curr.z()];
                List<WfcTile> validBelow = new ArrayList<>();
                boolean changed = false;

                for (WfcTile belowTile : belowDomain) {
                    boolean compatibleWithAtLeastOne = false;
                    for (WfcTile currTile : currDomain) {
                        if (WfcTile.isVerticalCompatible(belowTile, currTile)) {
                            compatibleWithAtLeastOne = true;
                            break;
                        }
                    }
                    if (compatibleWithAtLeastOne) {
                        validBelow.add(belowTile);
                    } else {
                        changed = true;
                    }
                }

                if (changed) {
                    if (validBelow.isEmpty()) {
                        return false;
                    }
                    grid[belowL][curr.x()][curr.z()] = validBelow;
                    queue.add(new CellPos3D(belowL, curr.x(), curr.z()));
                }
            }

            if (curr.l() < layers - 1) {
                int aboveL = curr.l() + 1;
                List<WfcTile> aboveDomain = grid[aboveL][curr.x()][curr.z()];
                List<WfcTile> validAbove = new ArrayList<>();
                boolean changed = false;

                for (WfcTile aboveTile : aboveDomain) {
                    boolean compatibleWithAtLeastOne = false;
                    for (WfcTile currTile : currDomain) {
                        if (WfcTile.isVerticalCompatible(currTile, aboveTile)) {
                            compatibleWithAtLeastOne = true;
                            break;
                        }
                    }
                    if (compatibleWithAtLeastOne) {
                        validAbove.add(aboveTile);
                    } else {
                        changed = true;
                    }
                }

                if (changed) {
                    if (validAbove.isEmpty()) {
                        return false;
                    }
                    grid[aboveL][curr.x()][curr.z()] = validAbove;
                    queue.add(new CellPos3D(aboveL, curr.x(), curr.z()));
                }
            }
        }
        return true;
    }

    private CellPos3D findLowestEntropyCell(List<WfcTile>[][][] grid) {
        CellPos3D best = null;
        double minEntropy = Double.MAX_VALUE;

        int centerX = width / 2;
        int centerZ = height / 2;

        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    int size = grid[l][x][z].size();
                    if (size > 1) {
                        double noise = random.nextDouble() * 0.1;

                        double distFromCenter = Math.hypot(x - centerX, z - centerZ) * 0.05 + l * 0.2;
                        boolean adjacentToCollapsed = hasAdjacentOpenSocket(grid, l, x, z);
                        double adjacencyBonus = adjacentToCollapsed ? -0.8 : 0.0;

                        double entropy = size + noise + distFromCenter + adjacencyBonus;
                        if (entropy < minEntropy) {
                            minEntropy = entropy;
                            best = new CellPos3D(l, x, z);
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean hasAdjacentOpenSocket(List<WfcTile>[][][] grid, int l, int x, int z) {
        for (Direction dir : Direction.values()) {
            int nx = x + getDirX(dir);
            int nz = z + getDirZ(dir);
            if (nx >= 0 && nx < width && nz >= 0 && nz < height) {
                if (grid[l][nx][nz].size() == 1 && grid[l][nx][nz].get(0).isOpen(dir.opposite())) {
                    return true;
                }
            }
        }
        if (l > 0 && grid[l - 1][x][z].size() == 1 && grid[l - 1][x][z].get(0).isVerticalBottom()) {
            return true;
        }
        return false;
    }

    private int countVerticalBottomTiles(List<WfcTile>[][][] grid, int floor) {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (grid[floor][x][z].size() == 1 && grid[floor][x][z].get(0).isVerticalBottom()) {
                    if (++count >= 2) return count;
                }
            }
        }
        return count;
    }

    private int countSinceLastSegment(List<WfcTile>[][][] grid, CellPos3D cell, MineshaftSegment segment) {
        int minDistance = Integer.MAX_VALUE;
        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    if (grid[l][x][z].size() == 1 && grid[l][x][z].get(0).segment() == segment) {
                        int dist = Math.abs(x - cell.x()) + Math.abs(z - cell.z()) + Math.abs(l - cell.l());
                        minDistance = Math.min(minDistance, dist);
                    }
                }
            }
        }
        return minDistance == Integer.MAX_VALUE ? Integer.MAX_VALUE : minDistance;
    }

    private int deadEndRepulsionRadius() {
        return Math.max(1, MIN_DEAD_END_DISTANCE / cellStep);
    }

    private static boolean isDeadEndTile(WfcTile tile) {
        if (tile == null || tile.isEmpty()) return false;
        if (tile.isVerticalBottom() || tile.isVerticalTop()) return false;
        if (tile.isMultiTilePart()) return false;
        return tile.openSockets().size() == 1;
    }

    private boolean hasNearbyDeadEnd(List<WfcTile>[][][] grid, CellPos3D cell, int radius) {
        int l = cell.l(), cx = cell.x(), cz = cell.z();
        int minX = Math.max(0, cx - radius), maxX = Math.min(width - 1, cx + radius);
        int minZ = Math.max(0, cz - radius), maxZ = Math.min(height - 1, cz + radius);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x == cx && z == cz) continue;
                List<WfcTile> domain = grid[l][x][z];
                if (domain.size() == 1 && isDeadEndTile(domain.get(0))) {
                    if (Math.abs(x - cx) + Math.abs(z - cz) <= radius) return true;
                }
            }
        }
        return false;
    }

    private String appearanceKey(WfcTile tile) {
        if (tile == null) return null;
        if (tile.isMultiTilePart()) return tile.cluster() != null ? tile.cluster().getId() : null;
        return tile.segment() != null ? tile.segment().getId() : null;
    }

    private boolean isAtMaxAppearances(WfcTile tile, Map<String, Integer> counts) {
        String key = appearanceKey(tile);
        if (key == null) return false;
        int[] limits = appearanceLimits.get(key);
        if (limits == null || limits[1] <= 0) return false;
        return counts.getOrDefault(key, 0) >= limits[1];
    }

    private void recordAppearance(WfcTile tile, Map<String, Integer> counts) {
        String key = appearanceKey(tile);
        if (key == null) return;
        if (!appearanceLimits.containsKey(key)) return;
        counts.merge(key, 1, Integer::sum);
    }

    private double underMinAppearancesBoost(WfcTile tile, Map<String, Integer> appearanceCounts) {
        String key = appearanceKey(tile);
        if (key == null) return 1.0;
        int[] limits = appearanceLimits.get(key);
        if (limits == null || limits[0] <= 0) return 1.0;
        int soFar = appearanceCounts.getOrDefault(key, 0);
        return soFar < limits[0] ? 6.0 : 1.0;
    }

    private WfcTile chooseWeighted(List<WfcTile> domain, CellPos3D cell, List<WfcTile>[][][] grid, Map<String, Integer> appearanceCounts) {
        boolean hasIncomingOpenSocket = hasAdjacentOpenSocket(grid, cell.l(), cell.x(), cell.z());

        List<WfcTile> candidateDomain = domain;
        if (hasIncomingOpenSocket) {
            List<WfcTile> nonEmpty = domain.stream().filter(t -> !t.isEmpty()).toList();
            if (!nonEmpty.isEmpty()) {
                candidateDomain = nonEmpty;
            }
        } else {
            List<WfcTile> emptyList = domain.stream().filter(WfcTile::isEmpty).toList();
            List<WfcTile> nonEmptyList = domain.stream().filter(t -> !t.isEmpty()).toList();

            if (!emptyList.isEmpty() && !nonEmptyList.isEmpty()) {
                if (random.nextDouble() < 0.5) {
                    return emptyList.get(0);
                }
                candidateDomain = nonEmptyList;
            } else if (!emptyList.isEmpty()) {
                return emptyList.get(0);
            }
        }

        int currentStairs = countVerticalBottomTiles(grid, cell.l());
        boolean boostStairs = cell.l() < layers - 1 && currentStairs < 2;

        int consecutiveStraight = countConsecutiveStraight(grid, cell);
        boolean nearbyDeadEnd = hasNearbyDeadEnd(grid, cell, deadEndRepulsionRadius());

        double totalWeight = 0.0;
        for (WfcTile tile : candidateDomain) {
            double w = Math.max(0.01, tile.weight());
            if (!tile.isMultiTilePart()) {
                if (tile.isVerticalBottom() && boostStairs) {
                    w *= 4.0;
                }
                if (isStraightTile(tile) && consecutiveStraight > 0) {
                    int maxCons = tile.segment() != null ? tile.segment().getMaxConsecutiveStraight() : -1;
                    if (maxCons > 0 && consecutiveStraight >= maxCons) {
                        w *= 0.05;
                    }
                }
                if (isDeadEndTile(tile) && nearbyDeadEnd) {
                    w *= 0.03;
                }
            }

            if (tile.segment() != null) {
                int spacing = tile.segment().getMinSpacing();
                if (spacing > 0) {
                    int dist = countSinceLastSegment(grid, cell, tile.segment());
                    if (dist < spacing) w *= 0.01;
                }
            }

            w *= underMinAppearancesBoost(tile, appearanceCounts);

            totalWeight += w;
        }
        double r = random.nextDouble() * totalWeight;
        double count = 0.0;
        for (WfcTile tile : candidateDomain) {
            double w = Math.max(0.01, tile.weight());
            if (!tile.isMultiTilePart()) {
                if (tile.isVerticalBottom() && boostStairs) {
                    w *= 4.0;
                }
                if (isStraightTile(tile) && consecutiveStraight > 0) {
                    int maxCons = tile.segment() != null ? tile.segment().getMaxConsecutiveStraight() : -1;
                    if (maxCons > 0 && consecutiveStraight >= maxCons) {
                        w *= 0.05;
                    }
                }
                if (isDeadEndTile(tile) && nearbyDeadEnd) {
                    w *= 0.03;
                }
            }

            if (tile.segment() != null) {
                int spacing = tile.segment().getMinSpacing();
                if (spacing > 0) {
                    int dist = countSinceLastSegment(grid, cell, tile.segment());
                    if (dist < spacing) w *= 0.01;
                }
            }

            w *= underMinAppearancesBoost(tile, appearanceCounts);

            count += w;
            if (count >= r) {
                return tile;
            }
        }
        return candidateDomain.get(candidateDomain.size() - 1);
    }

    private static int getDirX(Direction dir) {
        return switch (dir) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private static int getDirZ(Direction dir) {
        return switch (dir) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }

    private static boolean isStraightTile(WfcTile tile) {
        if (tile == null || tile.isEmpty() || tile.segment() == null) return false;
        if (tile.isMultiTilePart()) return false;
        if (tile.isVerticalBottom() || tile.isVerticalTop()) return false;
        var sockets = tile.openSockets();
        if (sockets.size() != 2) return false;
        var dirs = new ArrayList<>(sockets);
        return dirs.get(0).opposite() == dirs.get(1);
    }

    private int countConsecutiveStraight(List<WfcTile>[][][] grid, CellPos3D cell) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            int nx = cell.x() + getDirX(dir);
            int nz = cell.z() + getDirZ(dir);
            int localCount = 0;
            while (nx >= 0 && nx < width && nz >= 0 && nz < height) {
                if (grid[cell.l()][nx][nz].size() == 1 && isStraightTile(grid[cell.l()][nx][nz].get(0))) {
                    localCount++;
                    nx += getDirX(dir);
                    nz += getDirZ(dir);
                } else {
                    break;
                }
            }
            count = Math.max(count, localCount);
        }
        return count;
    }

    /**
     * Flood fills outward from the true grid center (where the centre/center
     * cluster is now guaranteed to sit) using the same open socket rules the
     * WFC solver itself uses, then nukes anything the flood never touched.
     * Multi tile clusters are treated as one indivisible blob: if any single
     * cell of a cluster gets reached, the whole footprint survives, and if
     * none of it gets reached, the whole footprint gets vaporized.
     */
    private void pruneUnreachable(WfcTile[][][] grid) {
        int startX = width / 2;
        int startZ = height / 2;
        int startL = 0;

        if (grid[startL][startX][startZ] == null || grid[startL][startX][startZ].isEmpty()) return;

        boolean[][][] visited = new boolean[layers][width][height];
        Deque<CellPos3D> queue = new ArrayDeque<>();

        markReachable(grid, visited, queue, startL, startX, startZ);

        while (!queue.isEmpty()) {
            CellPos3D cur = queue.poll();
            WfcTile curTile = grid[cur.l()][cur.x()][cur.z()];
            if (curTile == null || curTile.isEmpty()) continue;

            for (Direction dir : Direction.values()) {
                int nx = cur.x() + getDirX(dir);
                int nz = cur.z() + getDirZ(dir);
                if (nx < 0 || nx >= width || nz < 0 || nz >= height) continue;
                if (visited[cur.l()][nx][nz]) continue;

                WfcTile neighborTile = grid[cur.l()][nx][nz];
                if (neighborTile == null || neighborTile.isEmpty()) continue;

                if (curTile.isOpen(dir) && neighborTile.isOpen(dir.opposite())) {
                    markReachable(grid, visited, queue, cur.l(), nx, nz);
                }
            }

            if (cur.l() + 1 < layers && !visited[cur.l() + 1][cur.x()][cur.z()]) {
                WfcTile above = grid[cur.l() + 1][cur.x()][cur.z()];
                if (above != null && !above.isEmpty() && WfcTile.isVerticalCompatible(curTile, above)) {
                    markReachable(grid, visited, queue, cur.l() + 1, cur.x(), cur.z());
                }
            }
            if (cur.l() - 1 >= 0 && !visited[cur.l() - 1][cur.x()][cur.z()]) {
                WfcTile below = grid[cur.l() - 1][cur.x()][cur.z()];
                if (below != null && !below.isEmpty() && WfcTile.isVerticalCompatible(below, curTile)) {
                    markReachable(grid, visited, queue, cur.l() - 1, cur.x(), cur.z());
                }
            }
        }

        WfcTile emptyTile = prototypes.stream().filter(WfcTile::isEmpty).findFirst()
                .orElse(WfcTile.createEmptyTile(2.0));

        int removed = 0;
        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    WfcTile tile = grid[l][x][z];
                    if (tile != null && !tile.isEmpty() && !visited[l][x][z]) {
                        grid[l][x][z] = emptyTile;
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            System.out.println("[WFC] pruned " + removed + " unreachable cell(s), they were living a lie");
        }
    }

    private void markReachable(WfcTile[][][] grid, boolean[][][] visited, Deque<CellPos3D> queue, int l, int x, int z) {
        if (visited[l][x][z]) return;
        WfcTile tile = grid[l][x][z];
        visited[l][x][z] = true;
        queue.add(new CellPos3D(l, x, z));

        if (tile != null && tile.isMultiTilePart()) {
            Cluster cluster = tile.cluster();
            Rotation rot = tile.rotation();
            int anchorX = x - tile.footprintOffsetX();
            int anchorZ = z - tile.footprintOffsetZ();
            int anchorL = l - tile.footprintOffsetLayer();
            int fw = cluster.getFootprintWidth(rot);
            int fh = cluster.getFootprintHeight(rot);
            int flayers = cluster.getFootprintLayers();

            for (int i = 0; i < fw; i++) {
                for (int j = 0; j < fh; j++) {
                    for (int k = 0; k < flayers; k++) {
                        int gx = anchorX + i, gz = anchorZ + j, gl = anchorL + k;
                        if (gx < 0 || gx >= width || gz < 0 || gz >= height || gl < 0 || gl >= layers) continue;
                        if (!visited[gl][gx][gz]) {
                            visited[gl][gx][gz] = true;
                            queue.add(new CellPos3D(gl, gx, gz));
                        }
                    }
                }
            }
        }
    }

    public void generateIntoInstance(Instance instance, Point worldStartPos, Block defaultMarkerBlock) {
        WfcTile[][][] solution = solve3D(50, worldStartPos);

        int centerX = width / 2;
        int centerZ = height / 2;
        int layerStepY = 5;

        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    WfcTile tile = solution[l][x][z];
                    if (tile == null || tile.isEmpty()) continue;

                    if (tile.isVerticalTop()) continue;

                    int relX = x - centerX;
                    int relZ = z - centerZ;
                    int worldCenterX = worldStartPos.blockX() + relX * cellStep;
                    int worldCenterZ = worldStartPos.blockZ() + relZ * cellStep;
                    int worldCenterY = worldStartPos.blockY() + l * layerStepY;

                    if (tile.isMultiTilePart()) {
                        if (!tile.isFootprintAnchor()) continue;

                        Cluster cluster = tile.cluster();
                        Rotation rot = tile.rotation();
                        int fw = cluster.getFootprintWidth(rot);
                        int fh = cluster.getFootprintHeight(rot);

                        int[] bounds = cluster.getRotatedBounds(rot);
                        int minRelX = bounds[0], maxRelX = bounds[1];
                        int minRelZ = bounds[2], maxRelZ = bounds[3];
                        int rotWidth = maxRelX - minRelX + 1;
                        int rotLength = maxRelZ - minRelZ + 1;

                        int footprintCenterX = worldCenterX + (fw - 1) * cellStep / 2;
                        int footprintCenterZ = worldCenterZ + (fh - 1) * cellStep / 2;

                        int baseX = footprintCenterX - rotWidth / 2 - minRelX;
                        int baseZ = footprintCenterZ - rotLength / 2 - minRelZ;
                        int baseY = worldCenterY;

                        pasteCluster(instance, cluster, rot, new Vec(baseX, baseY, baseZ), defaultMarkerBlock);
                    } else if (tile.segment() != null) {
                        MineshaftSegment segment = tile.segment();
                        Rotation rot = tile.rotation();

                        Point c1 = CoordinateUtil.rotatePos(new Vec(0, 0, 0), rot);
                        Point c2 = CoordinateUtil.rotatePos(new Vec(segment.getWidth() - 1, 0, 0), rot);
                        Point c3 = CoordinateUtil.rotatePos(new Vec(0, 0, segment.getLength() - 1), rot);
                        Point c4 = CoordinateUtil.rotatePos(new Vec(segment.getWidth() - 1, 0, segment.getLength() - 1), rot);

                        int minRelX = (int) Math.min(Math.min(c1.x(), c2.x()), Math.min(c3.x(), c4.x()));
                        int maxRelX = (int) Math.max(Math.max(c1.x(), c2.x()), Math.max(c3.x(), c4.x()));
                        int minRelZ = (int) Math.min(Math.min(c1.z(), c2.z()), Math.min(c3.z(), c4.z()));
                        int maxRelZ = (int) Math.max(Math.max(c1.z(), c2.z()), Math.max(c3.z(), c4.z()));

                        int rotWidth = maxRelX - minRelX + 1;
                        int rotLength = maxRelZ - minRelZ + 1;

                        int baseX = worldCenterX - rotWidth / 2 - minRelX;
                        int baseY = worldCenterY;
                        int baseZ = worldCenterZ - rotLength / 2 - minRelZ;

                        MineshaftGenerator.pasteSegment(instance, segment, new Vec(baseX, baseY, baseZ), rot,
                                segment.getMarkerBlock() != null ? segment.getMarkerBlock() : defaultMarkerBlock);
                    }
                }
            }
        }
    }

    private void pasteCluster(Instance instance, Cluster cluster, Rotation rotation, Point basePos, Block markerBlock) {
        if (cluster == null || cluster.getSchematic() == null) return;
        Schematic schematic = cluster.getSchematic();
        schematic.forEachBlock(rotation, (offset, block) -> {
            if (markerBlock != null && block.compare(markerBlock)) {
                instance.setBlock(basePos.add(offset.x(), offset.y(), offset.z()), Block.AIR);
                return;
            }
            instance.setBlock(basePos.add(offset.x(), offset.y(), offset.z()), BlockHandlers.addHandler(block));
        });
    }

    private record CellPos3D(int l, int x, int z) {}
}