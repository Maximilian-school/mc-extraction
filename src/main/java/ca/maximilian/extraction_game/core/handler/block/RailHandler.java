package ca.maximilian.extraction_game.core.handler.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jspecify.annotations.NonNull;

public class RailHandler implements BlockHandler {

    @Override
    public void onPlace(@NonNull Placement placement) {
        Instance instance = placement.getInstance();
        Point pos = placement.getBlockPosition();

        // 1. Update the newly placed rail based on its current neighbors
        updateRailState(instance, pos, placement.getBlock());

        // 2. Alert the 4 adjacent cardinal positions to re-evaluate their shapes
        updateNeighbor(instance, pos.add(0, 0, -1)); // North
        updateNeighbor(instance, pos.add(0, 0, 1));  // South
        updateNeighbor(instance, pos.add(1, 0, 0));  // East
        updateNeighbor(instance, pos.add(-1, 0, 0)); // West
    }

    @Override
    public void onDestroy(@NonNull Destroy destroy) {
        Instance instance = destroy.getInstance();
        Point pos = destroy.getBlockPosition();

        // Alert neighbors that this rail was broken so they can disconnect
        updateNeighbor(instance, pos.add(0, 0, -1)); // North
        updateNeighbor(instance, pos.add(0, 0, 1));  // South
        updateNeighbor(instance, pos.add(1, 0, 0));  // East
        updateNeighbor(instance, pos.add(-1, 0, 0)); // West
    }

    private void updateNeighbor(Instance instance, Point neighborPos) {
        Block neighborBlock = instance.getBlock(neighborPos);
        // Only trigger an update if the neighbor is actually a rail
        if (isRail(neighborBlock)) {
            updateRailState(instance, neighborPos, neighborBlock);
        }
    }

    private void updateRailState(Instance instance, Point pos, Block currentBlock) {
        // Fetch fresh block references directly from the world to avoid stale cache data
        boolean north = isRail(instance.getBlock(pos.add(0, 0, -1)));
        boolean south = isRail(instance.getBlock(pos.add(0, 0, 1)));
        boolean east = isRail(instance.getBlock(pos.add(1, 0, 0)));
        boolean west = isRail(instance.getBlock(pos.add(-1, 0, 0)));

        String shape = "north_south"; // Default fallback shape

        if (east || west) shape = "east_west";
        if (north || south) shape = "north_south";

        // Handle corners
        if (east && north) shape = "north_east";
        if (east && south) shape = "south_east";
        if (west && north) shape = "north_west";
        if (west && south) shape = "south_west";

        Block updatedBlock = currentBlock.withProperty("shape", shape);

        // Only modify if something changed to break potential circular recursion loops
        if (currentBlock.stateId() != updatedBlock.stateId()) {
            instance.setBlock(pos, updatedBlock);
        }
    }

    private boolean isRail(Block block) {
        return block != null && block != Block.AIR && block.name().contains("rail");
    }

    @Override
    public @NonNull Key getKey() {
        return Key.key("extraction:rail");
    }
}
