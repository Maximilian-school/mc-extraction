package ca.maximilian.extraction_game.core.handler.block;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.utils.ItemPrices;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Tool;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockValidator {

    public record BlockProperties(ItemStack itemStack, boolean onlyBreakableIfPlaced) {}

    public static final Map<Integer, BlockProperties> BLOCKS = new HashMap<>(Map.of(
            // LOOT
            Block.COAL_BLOCK.id(), new BlockProperties(ItemStack.of(Material.COAL).withAmount(9), false),

            // NATURAL
            Block.STONE.id(), new BlockProperties(ItemStack.of(Material.COBBLESTONE), true),
            Block.COBBLESTONE.id(), new BlockProperties(ItemStack.of(Material.COBBLESTONE), false)
    ));

    public static @Nullable ItemStack getItem(Block block) {
        if (!BLOCKS.containsKey(block.id())) {
            return null;
        }
        return ItemPrices.withPrices(BLOCKS.get(block.id()).itemStack());
    }

    public static boolean hasItem(Block block) {
        return BLOCKS.containsKey(block.id());
    }

    /**
     * Checks your custom placement tracking logic to determine if the block is eligible to be broken.
     */
    public static boolean canBeBroken(Block block) {
        if (!hasItem(block)) {
            return false;
        }

        UUID placerUuid = block.getTag(Constants.PLACED_BY_TAG);
        boolean wasPlaced = placerUuid != null;
        BlockProperties blockProperties = BLOCKS.get(block.id());

        return !blockProperties.onlyBreakableIfPlaced() || wasPlaced;
    }

    /**
     * Enforces vanilla tool-matching rules alongside your custom extraction validation.
     */
    public static boolean canHarvestBlockLoot(Player player, Block block) {
        if (!canBeBroken(block)) {
            return false;
        }

        if (!block.requiresTool()) {
            return true;
        }

        ItemStack heldItem = player.getItemInMainHand();
        Tool tool = heldItem.get(DataComponents.TOOL);

        if (tool == null) {
            return false;
        }

        return tool.isCorrectForDrops(block.registryKey());
    }
}
