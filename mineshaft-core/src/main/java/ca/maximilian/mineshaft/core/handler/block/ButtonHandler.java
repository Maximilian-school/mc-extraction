package ca.maximilian.mineshaft.core.handler.block;

import ca.maximilian.mineshaft.core.RunningGame;
import ca.maximilian.mineshaft.core.utils.NbtUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ButtonHandler implements BlockHandler {

    public static final Tag<Boolean> SELL_BUTTON_TAG = Tag.Boolean("sell_button");
    public static final Tag<Boolean> SELL_RAIL_TAG = Tag.Boolean("sell_rail");

    public static List<Point> findBlocksInRadius(Instance instance, Point center, int radius, Predicate<Block> condition) {
        List<Point> locations = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Point targetPoint = center.add(x, y, z);
                    Block block = instance.getBlock(targetPoint);

                    if (condition.test(block)) {
                        locations.add(targetPoint);
                    }
                }
            }
        }
        return locations;
    }

    public static List<Point> findRailsInRadius(Instance instance, Point center, boolean withTag) {
        int radius = 1;

        return findBlocksInRadius(instance, center, radius, block -> {
            if (block.name().contains("rail")) {
                if (withTag && block.getTag(SELL_RAIL_TAG)) {
                    return true;
                } else if (!withTag) {
                    return true;
                }
            }
            return false;
        });
    }

    public static List<Point> findSignsInRadius(Instance instance, Point center) {
        int radius = 1;

        return findBlocksInRadius(instance, center, radius, block -> block.name().contains("sign"));
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

                for (Point point : findSignsInRadius(instance, placement.getBlockPosition())) {

                    ListBinaryTag messagesList = ListBinaryTag.listBinaryTag(
                            BinaryTagTypes.STRING,
                            NbtUtil.createSignLines(
                                    Component.text("Press to sell!").color(NamedTextColor.GREEN),
                                    Component.text(""),
                                    Component.text(""),
                                    Component.text("--->").color(NamedTextColor.GREEN)
                            )
                    );

                    CompoundBinaryTag frontText = CompoundBinaryTag.builder()
                            .put("messages", messagesList)
                            .putBoolean("has_glowing_text", true)
                            .build();

                    CompoundBinaryTag rootTag = CompoundBinaryTag.builder()
                            .put("front_text", frontText)
                            .putBoolean("is_waxed", false)
                            .build();

                    instance.setBlock(point, instance.getBlock(point).withNbt(rootTag));
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
