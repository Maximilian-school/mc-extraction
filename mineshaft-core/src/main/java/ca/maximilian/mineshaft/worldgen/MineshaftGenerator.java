package ca.maximilian.mineshaft.worldgen;

import ca.maximilian.mineshaft.core.handler.BlockHandlers;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MineshaftGenerator {

    public static final String DEFAULT_CONFIG_PATH = "/mineshaft/config.json";
    public static final int DEFAULT_SIZE_BLOCKS = 24*5;
    public static final int DEFAULT_FLOORS = 3;
    private static final ExecutorService GENERATION_WORKERS = Executors.newFixedThreadPool(
            Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 4),
            Thread.ofPlatform().daemon().name("mineshaft-worker-", 0).factory());

    // Shutdown hook to properly terminate the executor service
    private static final Thread SHUTDOWN_HOOK = new Thread(() -> {
        shutdown();
    });

    static {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
    }

    public static void generate(Instance instance, Pos startPos, String configPath, int maxSegments) {
        generate(instance, startPos, configPath, maxSegments, new Random());
    }

    public static void generate(Instance instance, Pos startPos, String configPath, int maxSegments, Random random) {
        if (maxSegments <= 0) throw new IllegalArgumentException("maxSegments must be positive");
        int gridSize = Math.max(7, (int) Math.ceil(Math.sqrt(maxSegments * 1.5)));
        if (gridSize % 2 == 0) gridSize++;
        int requestedGridSize = gridSize;
        CompletableFuture.runAsync(() -> generateOnWorker(instance, startPos, configPath,
                requestedGridSize, false, DEFAULT_FLOORS, random), GENERATION_WORKERS).join();
    }

    public static CompletableFuture<Void> generateAsync(Instance instance) {
        return generateAsync(instance, new Pos(0, 0, 0), DEFAULT_CONFIG_PATH,
                DEFAULT_SIZE_BLOCKS, DEFAULT_FLOORS, new Random());
    }

    public static CompletableFuture<Void> generateAsync(Instance instance, Pos startPos, String configPath,
                                                        int sizeBlocks, int floors, Random random) {
        if (sizeBlocks <= 0 || floors <= 0) {
            throw new IllegalArgumentException("Size and floor count must be positive");
        }
        return CompletableFuture.runAsync(() -> generateOnWorker(instance, startPos, configPath,
                sizeBlocks, true, floors, random), GENERATION_WORKERS);
    }

    private static void generateOnWorker(Instance instance, Pos startPos, String configPath,
                                         int size, boolean sizeInBlocks, int floors, Random random) {
        List<MineshaftSegment> segments = SegmentLoader.loadSegments(configPath);
        if (segments.isEmpty()) throw new IllegalStateException("No segments loaded from config!");
        // No more dedicated starting segment, cell step is just the largest
        // ordinary (non-stairwell) segment footprint.
        int cellStep = 5;
        int gridSize = sizeInBlocks ? gridSizeForBlocks(size, cellStep) : size;
        boolean hasVertical = segments.stream().anyMatch(WaveFunctionCollapse::isStairwellSegment);
        WaveFunctionCollapse wfc = new WaveFunctionCollapse(hasVertical ? Math.max(2, floors) : floors,
                gridSize, gridSize, cellStep, segments, random);
        wfc.generateIntoInstance(instance, startPos, Block.BEACON);
    }

    static int gridSizeForBlocks(int sizeBlocks, int cellStep) {
        if (cellStep <= 0) throw new IllegalArgumentException("Room width must be positive");
        int gridSize = sizeBlocks / cellStep;
        if (gridSize % 2 == 0) gridSize--;
        if (gridSize < 7) throw new IllegalArgumentException("Area must fit at least 7 rooms per side");
        return gridSize;
    }

    private static Point getDirectionOffset(Direction direction) {
        return switch (direction) {
            case NORTH -> new Vec(0, 0, -1);
            case SOUTH -> new Vec(0, 0, 1);
            case EAST -> new Vec(1, 0, 0);
            case WEST -> new Vec(-1, 0, 0);
        };
    }

    public static void pasteSegment(Instance instance, MineshaftSegment segment, Point basePos, Rotation rotation, Block markerBlock) {
        var schematic = segment.getSchematic();
        schematic.forEachBlock(rotation, (offset, block) -> {
            if (markerBlock != null && block.compare(markerBlock)) {
                instance.setBlock(basePos.add(offset.x(), offset.y(), offset.z()), Block.AIR);
                return;
            }

            Point actualPos = basePos.add(offset.x(), offset.y(), offset.z());
            instance.setBlock(actualPos, BlockHandlers.addHandler(block));
        });
    }

    public static void pasteSegment(Instance instance, MineshaftSegment segment, Point basePos, Block markerBlock) {
        pasteSegment(instance, segment, basePos, Rotation.NONE, markerBlock);
    }

    public static void pasteSegment(Instance instance, MineshaftSegment segment, Pos basePos, Block markerBlock) {
        pasteSegment(instance, segment, (Point) basePos, Rotation.NONE, markerBlock);
    }

    public static void pasteSegment(Instance instance, MineshaftSegment segment, Point basePos) {
        pasteSegment(instance, segment, basePos, Rotation.NONE, Block.BEACON);
    }

    public static void pasteSegment(Instance instance, MineshaftSegment segment, Pos basePos) {
        pasteSegment(instance, segment, (Point) basePos, Rotation.NONE, Block.BEACON);
    }

    private record OpenSocket(Point basePos, MineshaftSegment parentSegment, Connector connector, Rotation rotation) {}

    /**
     * Shuts down the executor service. Should be called when the application is shutting down
     * to prevent thread leaks.
     */
    public static void shutdown() {
        GENERATION_WORKERS.shutdown();
        try {
            if (!GENERATION_WORKERS.awaitTermination(5, TimeUnit.SECONDS)) {
                GENERATION_WORKERS.shutdownNow();
            }
        } catch (InterruptedException e) {
            GENERATION_WORKERS.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private record PlacementCandidate(
            MineshaftSegment segment,
            Rotation rotation,
            Connector connector,
            Point targetPos,
            BoundingBox boundingBox
    ) {}
}