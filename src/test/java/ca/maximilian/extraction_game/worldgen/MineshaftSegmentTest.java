package ca.maximilian.extraction_game.worldgen;

import ca.maximilian.extraction_game.ExtractionGame;
import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MineshaftSegmentTest {

    @BeforeAll
    static void setup() {
        MinecraftServer.init();
    }

    @Test
    void singleConnectorSegmentsRequire256BlocksFromWorldOrigin() {
        var schematic = SchematicBuilder.builder(new Vec(5, 5, 5)).build();
        var west = new Connector(Direction.WEST, new Vec(0, 1, 2));
        var east = new Connector(Direction.EAST, new Vec(4, 1, 2));
        var start = new MineshaftSegment("start", schematic, Set.of(west, east), true);
        // Deliberately use a name without "deadend": connector count controls eligibility.
        var end = new MineshaftSegment("terminal", schematic, Set.of(west));
        var segments = java.util.List.of(start, end);

        var inside = new WaveFunctionCollapse(1, 3, 1, 255, segments, start, new java.util.Random(1));
        assertThrows(IllegalStateException.class, () -> inside.solve3D(1));

        var boundary = new WaveFunctionCollapse(1, 3, 1, 256, segments, start, new java.util.Random(1));
        var solution = boundary.solve3D(1);
        assertSame(end, solution[0][0][0].segment());
        assertSame(end, solution[0][2][0].segment());

        var offset = new WaveFunctionCollapse(1, 3, 1, 180, segments, start, new java.util.Random(1));
        assertThrows(IllegalStateException.class, () -> offset.solve3D(1, new Vec(0, 200, 180)));
        var offsetSolution = offset.solve3D(1, new Vec(0, 0, 183));
        assertSame(end, offsetSolution[0][0][0].segment());
        assertSame(end, offsetSolution[0][2][0].segment());
    }

    @Test
    void largeMineshaftHasLongConnectedRoutes() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var start = SegmentLoader.getStartingSegment(segments);
        int size = MineshaftGenerator.gridSizeForBlocks(MineshaftGenerator.DEFAULT_SIZE_BLOCKS, start.getWidth());
        assertEquals(25, size);
        assertTrue(size * start.getWidth() <= 128);
        int center = size / 2;
        int floors = 2;

        for (long seed : new long[]{42, 12345, 99999}) {
            var wfc = new WaveFunctionCollapse(floors, size, size, start.getWidth(), segments,
                    start, new java.util.Random(seed));
            var grid = wfc.solve3D();
            assertEquals(floors, grid.length);
            for (var layer : grid) {
                for (var row : layer) {
                    for (var tile : row) {
                        if (tile.segment() != null) {
                            assertNotEquals(1, tile.segment().getConnectors().size(),
                                    "This entire map is within the 256-block exclusion radius");
                        }
                    }
                }
            }
        }
    }

    private static void assertAllFloorsReachable(WfcTile[][][] grid, int center) {
        record Cell(int floor, int x, int z) {}
        var visited = new java.util.HashSet<Cell>();
        var queue = new java.util.ArrayDeque<Cell>();
        queue.add(new Cell(0, center, center));
        var floors = new java.util.HashSet<Integer>();
        while (!queue.isEmpty()) {
            var cell = queue.remove();
            if (!visited.add(cell)) continue;
            floors.add(cell.floor());
            var tile = grid[cell.floor()][cell.x()][cell.z()];
            for (Direction dir : tile.openSockets()) {
                int dx = dir == Direction.EAST ? 1 : dir == Direction.WEST ? -1 : 0;
                int dz = dir == Direction.SOUTH ? 1 : dir == Direction.NORTH ? -1 : 0;
                int x = cell.x() + dx;
                int z = cell.z() + dz;
                assertTrue(x >= 0 && x < grid[0].length && z >= 0 && z < grid[0][0].length);
                assertTrue(grid[cell.floor()][x][z].isOpen(dir.opposite()));
                queue.add(new Cell(cell.floor(), x, z));
            }
            if (tile.isVerticalBottom()) {
                assertTrue(cell.floor() + 1 < grid.length);
                assertTrue(WfcTile.isVerticalCompatible(tile, grid[cell.floor() + 1][cell.x()][cell.z()]));
                queue.add(new Cell(cell.floor() + 1, cell.x(), cell.z()));
            }
            if (tile.isVerticalTop()) {
                assertTrue(cell.floor() > 0);
                assertTrue(WfcTile.isVerticalCompatible(grid[cell.floor() - 1][cell.x()][cell.z()], tile));
                queue.add(new Cell(cell.floor() - 1, cell.x(), cell.z()));
            }
        }
        assertEquals(grid.length, floors.size(), "Every floor must be reachable from spawn via stairs");
    }

    @Test
    void generationRunsOnWorkerAndCompletesAfterPasting() throws Exception {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var workerName = new java.util.concurrent.atomic.AtomicReference<String>();
        instance.eventNode().addListener(net.minestom.server.event.instance.InstanceBlockUpdateEvent.class, event -> {
            if (workerName.compareAndSet(null, Thread.currentThread().getName())) {
                entered.countDown();
                try {
                    if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Test did not release generation worker");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        });
        var future = MineshaftGenerator.generateAsync(instance, Pos.ZERO,
                MineshaftGenerator.DEFAULT_CONFIG_PATH, 35, 1, new java.util.Random(42));
        try {
            assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(workerName.get().startsWith("mineshaft-worker-"));
            assertFalse(future.isDone(), "Map must not be published before pasting completes");
        } finally {
            release.countDown();
        }
        future.get(30, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(Block.COBBLESTONE, instance.getBlock(0, 0, 0));
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }

    @Test
    void generationFailuresReachTheCaller() {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        try {
            var future = MineshaftGenerator.generateAsync(instance, Pos.ZERO,
                    "/missing-config.json", 256, 4, new java.util.Random(42));
            assertThrows(java.util.concurrent.CompletionException.class, future::join);
        } finally {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }
    }

    @Test
    void testSegmentDistribution() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var seed = SegmentLoader.getStartingSegment(segments);
        int width = 9;
        int height = 9;
        int cellStep = 5;

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int trials = 50;
        int mazesWithStairwells = 0;

        for (int i = 0; i < trials; i++) {
            WaveFunctionCollapse wfc = new WaveFunctionCollapse(2, width, height, cellStep, segments, seed, new java.util.Random(i * 997L + 1));
            WfcTile[][][] sol = wfc.solve3D();
            boolean hasStair = false;
            for (int l = 0; l < 2; l++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < height; z++) {
                        WfcTile t = sol[l][x][z];
                        String id = t.isEmpty() ? "empty" : (t.segment() != null ? t.segment().getId() : "null");
                        if (t.isVerticalBottom()) {
                            id = "stairwell_bottom";
                            hasStair = true;
                        }
                        if (t.isVerticalTop()) {
                            id = "stairwell_top";
                        }
                        counts.put(id, counts.getOrDefault(id, 0) + 1);
                    }
                }
            }
            if (hasStair) mazesWithStairwells++;
        }

        System.out.println("[DEBUG_LOG] Mazes with stairwells: " + mazesWithStairwells + " / " + trials);
        counts.forEach((k, v) -> System.out.println("[DEBUG_LOG]   " + k + ": " + v));
    }

    @Test
    void testStartingSegmentHasSeveralConnectors() {
        MineshaftSegment startingSegment = MineshaftSegment.createDefaultStartingSegment();

        assertTrue(startingSegment.isStarting());
        assertTrue(startingSegment.isStartingSegment());
        assertEquals("starting_room", startingSegment.getId());

        var connectors = startingSegment.getConnectors();
        assertNotNull(connectors);
        // Has several connector points (NORTH, SOUTH, EAST, WEST)
        assertEquals(4, connectors.size());

        boolean hasNorth = connectors.stream().anyMatch(c -> c.direction() == Direction.NORTH);
        boolean hasSouth = connectors.stream().anyMatch(c -> c.direction() == Direction.SOUTH);
        boolean hasEast = connectors.stream().anyMatch(c -> c.direction() == Direction.EAST);
        boolean hasWest = connectors.stream().anyMatch(c -> c.direction() == Direction.WEST);

        assertTrue(hasNorth, "Should have a NORTH connector");
        assertTrue(hasSouth, "Should have a SOUTH connector");
        assertTrue(hasEast, "Should have an EAST connector");
        assertTrue(hasWest, "Should have a WEST connector");
    }

    @Test
    void testSegmentConnection() {
        MineshaftSegment start = MineshaftSegment.createDefaultStartingSegment();

        // Create a corridor segment that connects from SOUTH
        var builder = SchematicBuilder.builder(new Vec(5, 5, 10));
        builder.block(2, 1, 0, Block.BEACON); // NORTH connector
        builder.block(2, 1, 9, Block.BEACON); // SOUTH connector
        MineshaftSegment corridor = new MineshaftSegment("corridor", builder.build(), Block.BEACON);

        assertTrue(start.canConnect(corridor, Direction.SOUTH));
        assertTrue(start.canConnect(corridor, Direction.NORTH));
        assertFalse(start.canConnect(corridor, Direction.EAST));
    }

    @Test
    void testDirectionRotation() {
        assertEquals(Direction.NORTH, Direction.NORTH.rotate(Rotation.NONE));
        assertEquals(Direction.EAST, Direction.NORTH.rotate(Rotation.CLOCKWISE_90));
        assertEquals(Direction.SOUTH, Direction.NORTH.rotate(Rotation.CLOCKWISE_180));
        assertEquals(Direction.WEST, Direction.NORTH.rotate(Rotation.CLOCKWISE_270));

        assertEquals(Direction.SOUTH, Direction.EAST.rotate(Rotation.CLOCKWISE_90));
        assertEquals(Direction.WEST, Direction.SOUTH.rotate(Rotation.CLOCKWISE_90));
        assertEquals(Direction.NORTH, Direction.WEST.rotate(Rotation.CLOCKWISE_90));
    }

    @Test
    void testSegmentConnectorRotation() {
        // Create a segment with a single NORTH connector at (2, 1, 0) in size 5x5x10
        var builder = SchematicBuilder.builder(new Vec(5, 5, 10));
        builder.block(2, 1, 0, Block.BEACON);
        MineshaftSegment segment = new MineshaftSegment("one_way", builder.build(), Block.BEACON);

        Set<Connector> unrotated = segment.getRotatedConnectors(Rotation.NONE);
        assertEquals(1, unrotated.size());
        Connector c0 = unrotated.iterator().next();
        assertEquals(Direction.NORTH, c0.direction());
        assertEquals(new Vec(2, 1, 0), c0.position());

        Set<Connector> rotated90 = segment.getRotatedConnectors(Rotation.CLOCKWISE_90);
        assertEquals(1, rotated90.size());
        Connector c90 = rotated90.iterator().next();
        assertEquals(Direction.EAST, c90.direction());
        // (2, 1, 0) rotated 90 CW around (0,0,0) becomes (-z, y, x) = (0, 1, 2)
        assertEquals(new Vec(0, 1, 2), c90.position());

        Set<Connector> rotated180 = segment.getRotatedConnectors(Rotation.CLOCKWISE_180);
        assertEquals(1, rotated180.size());
        Connector c180 = rotated180.iterator().next();
        assertEquals(Direction.SOUTH, c180.direction());
        // (-x, y, -z) = (-2, 1, 0)
        assertEquals(new Vec(-2, 1, 0), c180.position());

        Set<Connector> rotated270 = segment.getRotatedConnectors(Rotation.CLOCKWISE_270);
        assertEquals(1, rotated270.size());
        Connector c270 = rotated270.iterator().next();
        assertEquals(Direction.WEST, c270.direction());
        // (z, y, -x) = (0, 1, -2)
        assertEquals(new Vec(0, 1, -2), c270.position());
    }

    @Test
    void testBoundingBoxCollision() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 10, 5, 10);
        BoundingBox box2 = new BoundingBox(5, 0, 5, 15, 5, 15);
        BoundingBox box3 = new BoundingBox(20, 0, 20, 30, 5, 30);

        assertTrue(box1.intersects(box2));
        assertTrue(box2.intersects(box1));
        assertFalse(box1.intersects(box3));
        assertFalse(box3.intersects(box1));

        // Touching on the boundary
        BoundingBox boxTouching = new BoundingBox(11, 0, 0, 20, 5, 10);
        assertFalse(box1.intersects(boxTouching));
    }

    @Test
    void testSegmentBoundingBoxCalculation() {
        var builder = SchematicBuilder.builder(new Vec(5, 4, 10));
        MineshaftSegment segment = new MineshaftSegment("test_box", builder.build(), (Block) null);

        Point basePos = new Vec(100, 50, 100);

        BoundingBox boxNone = segment.getBoundingBox(basePos, Rotation.NONE);
        assertEquals(100, boxNone.minX());
        assertEquals(104, boxNone.maxX());
        assertEquals(50, boxNone.minY());
        assertEquals(53, boxNone.maxY());
        assertEquals(100, boxNone.minZ());
        assertEquals(109, boxNone.maxZ());

        // 90 deg rotation: x ranges -9..0, z ranges 0..4
        BoundingBox box90 = segment.getBoundingBox(basePos, Rotation.CLOCKWISE_90);
        assertEquals(100 - 9, box90.minX());
        assertEquals(100, box90.maxX());
        assertEquals(50, box90.minY());
        assertEquals(53, box90.maxY());
        assertEquals(100, box90.minZ());
        assertEquals(104, box90.maxZ());
    }

    @Test
    void testBoundingBoxVolumeOverlap() {
        BoundingBox box1 = new BoundingBox(0, 0, 0, 5, 5, 5);
        // Adjacent box touching at face (x=5)
        BoundingBox boxAdjacent = new BoundingBox(5, 0, 0, 10, 5, 5);
        assertTrue(box1.intersects(boxAdjacent));
        assertFalse(box1.overlapsVolume(boxAdjacent));

        // Overlapping box
        BoundingBox boxOverlapping = new BoundingBox(4, 0, 0, 9, 5, 5);
        assertTrue(box1.intersects(boxOverlapping));
        assertTrue(box1.overlapsVolume(boxOverlapping));
    }

    @Test
    void testLoadExtractionGameConfig() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        assertFalse(segments.isEmpty(), "Segments should be loaded from /extraction_game/config.json");
        assertEquals(7, segments.size());
        for (var s : segments) {
            assertFalse(s.getConnectors().isEmpty(), "Segment " + s.getId() + " should have connectors");
        }
        var stairwell = segments.stream().filter(s -> s.getId().equals("stairwell")).findFirst().orElse(null);
        assertNotNull(stairwell, "Stairwell segment should be present");
        assertEquals(5, stairwell.getWidth());
        assertEquals(9, stairwell.getHeight());
        assertEquals(5, stairwell.getLength());
        assertEquals(2, stairwell.getConnectors().size());
    }

    @Test
    void testStairwellWfc3DGeneration() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var seed = SegmentLoader.getStartingSegment(segments);
        int layers = 2;
        int width = 7;
        int height = 7;
        int cellStep = 5;
        WaveFunctionCollapse wfc = new WaveFunctionCollapse(layers, width, height, cellStep, segments, seed, new java.util.Random(42));

        WfcTile[][][] grid = wfc.solve3D();
        assertNotNull(grid);
        assertEquals(layers, grid.length);
        assertEquals(width, grid[0].length);
        assertEquals(height, grid[0][0].length);

        // Verify vertical coupling of stairwell
        int stairwellCountLayer0 = 0;
        int stairwellCountLayer1 = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                WfcTile l0 = grid[0][x][z];
                WfcTile l1 = grid[1][x][z];

                if (l0.isVerticalBottom()) {
                    stairwellCountLayer0++;
                    assertTrue(l1.isVerticalTop(), "Layer 1 must have STAIRWELL_TOP at (" + x + "," + z + ")");
                    assertEquals(l0.rotation(), l1.rotation(), "Rotation must match between bottom and top");
                }
                if (l1.isVerticalTop()) {
                    stairwellCountLayer1++;
                    assertTrue(l0.isVerticalBottom(), "Layer 0 must have STAIRWELL_BOTTOM at (" + x + "," + z + ")");
                }
            }
        }
        assertEquals(stairwellCountLayer0, stairwellCountLayer1);

        // Verify socket compatibility across all layers
        for (int l = 0; l < layers; l++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    WfcTile curr = grid[l][x][z];

                    if (x + 1 < width) {
                        WfcTile eastNeighbor = grid[l][x + 1][z];
                        assertEquals(curr.isOpen(Direction.EAST), eastNeighbor.isOpen(Direction.WEST));
                    } else {
                        assertFalse(curr.isOpen(Direction.EAST));
                    }

                    if (x - 1 >= 0) {
                        WfcTile westNeighbor = grid[l][x - 1][z];
                        assertEquals(curr.isOpen(Direction.WEST), westNeighbor.isOpen(Direction.EAST));
                    } else {
                        assertFalse(curr.isOpen(Direction.WEST));
                    }

                    if (z + 1 < height) {
                        WfcTile southNeighbor = grid[l][x][z + 1];
                        assertEquals(curr.isOpen(Direction.SOUTH), southNeighbor.isOpen(Direction.NORTH));
                    } else {
                        assertFalse(curr.isOpen(Direction.SOUTH));
                    }

                    if (z - 1 >= 0) {
                        WfcTile northNeighbor = grid[l][x][z - 1];
                        assertEquals(curr.isOpen(Direction.NORTH), northNeighbor.isOpen(Direction.SOUTH));
                    } else {
                        assertFalse(curr.isOpen(Direction.NORTH));
                    }
                }
            }
        }
    }

    @Test
    void testStairwellPlacedInInstance() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var seed = SegmentLoader.getStartingSegment(segments);
        int width = 7;
        int height = 7;
        int cellStep = 5;

        var instance = net.minestom.server.MinecraftServer.getInstanceManager().createInstanceContainer();
        WaveFunctionCollapse wfc = new WaveFunctionCollapse(2, width, height, cellStep, segments, seed, new java.util.Random(12345));
        wfc.generateIntoInstance(instance, new Pos(0, 0, 0), Block.BEACON);

        assertNotNull(instance);
        // Floor 0 floor at (0, 0, 0)
        assertEquals(Block.COBBLESTONE, instance.getBlock(new Pos(0, 0, 0)));
        // Floor 0 ceiling at (0, 4, 0)
        assertEquals(Block.OAK_PLANKS, instance.getBlock(new Pos(0, 4, 0)));
    }

    @Test
    void testWfcGeneratesValidMazeGrid() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var seed = SegmentLoader.getStartingSegment(segments);
        int cellStep = MineshaftSegment.getMaxCellStep(segments);
        int width = 7;
        int height = 7;
        WaveFunctionCollapse wfc = new WaveFunctionCollapse(width, height, cellStep, segments, seed, new java.util.Random(12345));

        WfcTile[][] grid = wfc.solve();
        assertNotNull(grid);
        assertEquals(width, grid.length);
        assertEquals(height, grid[0].length);

        // Center cell is the starting segment
        int centerX = width / 2;
        int centerZ = height / 2;
        assertEquals("starting_room", grid[centerX][centerZ].segment().getId());

        // Count non-empty segments
        int nonEmptyCount = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (grid[x][z].segment() != null) {
                    nonEmptyCount++;
                }
            }
        }
        assertTrue(nonEmptyCount > 1, "Should generate a maze with multiple segments, got: " + nonEmptyCount);

        // Verify that EVERY adjacent pair satisfies socket constraints (AC-3 / WFC)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                WfcTile curr = grid[x][z];

                // Check East neighbor
                if (x + 1 < width) {
                    WfcTile eastNeighbor = grid[x + 1][z];
                    assertEquals(curr.isOpen(Direction.EAST), eastNeighbor.isOpen(Direction.WEST),
                            "Socket mismatch between (" + x + "," + z + ") EAST and (" + (x + 1) + "," + z + ") WEST");
                } else {
                    assertFalse(curr.isOpen(Direction.EAST), "Outer boundary EAST must be closed at x=" + x);
                }

                // Check West neighbor
                if (x - 1 >= 0) {
                    WfcTile westNeighbor = grid[x - 1][z];
                    assertEquals(curr.isOpen(Direction.WEST), westNeighbor.isOpen(Direction.EAST),
                            "Socket mismatch between (" + x + "," + z + ") WEST and (" + (x - 1) + "," + z + ") EAST");
                } else {
                    assertFalse(curr.isOpen(Direction.WEST), "Outer boundary WEST must be closed at x=" + x);
                }

                // Check South neighbor
                if (z + 1 < height) {
                    WfcTile southNeighbor = grid[x][z + 1];
                    assertEquals(curr.isOpen(Direction.SOUTH), southNeighbor.isOpen(Direction.NORTH),
                            "Socket mismatch between (" + x + "," + z + ") SOUTH and (" + x + "," + (z + 1) + ") NORTH");
                } else {
                    assertFalse(curr.isOpen(Direction.SOUTH), "Outer boundary SOUTH must be closed at z=" + z);
                }

                // Check North neighbor
                if (z - 1 >= 0) {
                    WfcTile northNeighbor = grid[x][z - 1];
                    assertEquals(curr.isOpen(Direction.NORTH), northNeighbor.isOpen(Direction.SOUTH),
                            "Socket mismatch between (" + x + "," + z + ") NORTH and (" + x + "," + (z - 1) + ") SOUTH");
                } else {
                    assertFalse(curr.isOpen(Direction.NORTH), "Outer boundary NORTH must be closed at z=" + z);
                }
            }
        }
    }

    @Test
    void testAdjacentSegmentsHaveNoGaps() {
        var segments = SegmentLoader.loadSegments("/extraction_game/config.json");
        var seed = SegmentLoader.getStartingSegment(segments);
        int width = 7;
        int height = 7;
        int cellStep = 5;

        for (long seedVal : new long[]{42L, 12345L, 99999L}) {
            WaveFunctionCollapse wfc = new WaveFunctionCollapse(width, height, cellStep, segments, seed, new java.util.Random(seedVal));
            var instance = net.minestom.server.MinecraftServer.getInstanceManager().createInstanceContainer();
            wfc.generateIntoInstance(instance, new Pos(0, 0, 0), Block.BEACON);

            assertNotNull(instance);
            // Verify center starting room floor at (0, 0, 0)
            assertNotEquals(Block.AIR, instance.getBlock(new Pos(0, 0, 0)));
            // Verify center room extends to x=2, z=0
            assertNotEquals(Block.AIR, instance.getBlock(new Pos(2, 0, 0)));
            // Verify adjacent cell at relX=1 starts at x=3, z=0 (no gap)
            assertNotEquals(Block.AIR, instance.getBlock(new Pos(3, 0, 0)));
        }
    }

    @Test
    void testMainCreateInstanceGeneratesMineshaft() {
        var instance = ExtractionGame.createInstance();
        assertNotNull(instance);

        // Starting room cobblestone floor should be present at (0, 0, 0)
        Block blockAtOrigin = instance.getBlock(new Pos(0, 0, 0));
        assertEquals(Block.COBBLESTONE, blockAtOrigin);

        // Starting room oak planks ceiling at (0, 4, 0)
        Block ceilingBlock = instance.getBlock(new Pos(0, 4, 0));
        assertEquals(Block.OAK_PLANKS, ceilingBlock);
    }
}
