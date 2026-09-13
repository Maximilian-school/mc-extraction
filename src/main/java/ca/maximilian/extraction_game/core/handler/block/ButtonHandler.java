package ca.maximilian.extraction_game.core.handler.block;

import ca.maximilian.extraction_game.core.RunningGame;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class ButtonHandler implements BlockHandler {

    public static final Tag<Boolean> SELL_BUTTON_TAG = Tag.Boolean("sell_button");
    public static final Tag<Boolean> SELL_RAIL_TAG = Tag.Boolean("sell_rail");

    public static List<Point> findRailsInRadius(Instance instance, Point center, boolean withTag) {
        List<Point> railLocations = new ArrayList<>();
        int radius = 1;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Point targetPoint = center.add(x, y, z);
                    Block block = instance.getBlock(targetPoint);

                    if (block.name().contains("rail")) {
                        if (withTag && block.getTag(SELL_RAIL_TAG)) {
                            railLocations.add(targetPoint);
                        } else if (!withTag) {
                            railLocations.add(targetPoint);
                        }
                    }
                }
            }
        }
        return railLocations;
    }

    @Override
    public void onPlace(Placement placement) {
        Instance instance = placement.getInstance();
        Point blockBelow = placement.getBlockPosition().add(0, -1, 0);

        switch (instance.getBlock(blockBelow)) {
            case Block b when b.compare(Block.BEDROCK) -> {
                instance.setBlock(blockBelow, Block.STONE);

                instance.setBlock(placement.getBlockPosition(), placement.getBlock()
                        .withTag(SELL_BUTTON_TAG, true)
                );

                for (Point point : findRailsInRadius(instance, blockBelow, false)) {
                    instance.setBlock(point, instance.getBlock(point).withTag(SELL_RAIL_TAG, true));
                }
            }
            case Block b when b.compare(Block.END_STONE) -> {
                instance.setBlock(blockBelow, Block.STONE);
                instance.setBlock(placement.getBlockPosition(), Block.AIR);

                RunningGame runningGame = instance.getTag(RunningGame.TAG);

                if (runningGame != null) {
                    runningGame.spawnShopKeeper(placement.getBlockPosition().add(0.5, 0, 0.5));
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean onInteract(Interaction interaction) {
        if (interaction.getBlock().getTag(SELL_BUTTON_TAG)) {
            RunningGame runningGame = interaction.getInstance().getTag(RunningGame.TAG);

            if (runningGame == null) return true;

            runningGame.getCart().sellAllItems(interaction.getPlayer());
        }

        return true;
    }

    @Override
    public Key getKey() {
        return Key.key("extraction:button_handler");
    }
}
