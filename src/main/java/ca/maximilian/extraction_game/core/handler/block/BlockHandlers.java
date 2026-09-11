package ca.maximilian.extraction_game.core.handler.block;

import ca.maximilian.extraction_game.core.utils.DropItem;
import ca.maximilian.extraction_game.core.utils.ItemPrices;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.block.BlockBreakCalculation;

import java.time.Duration;

public class BlockHandlers {
    public static void registerBlockHandlers() {
        MinecraftServer.getBlockManager().registerHandler("minecraft:chest", ChestHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:rail", RailHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:oak_button", ButtonHandler::new);
    }

    public static Block addHandler(Block block) {
        BlockManager blockManager = MinecraftServer.getBlockManager();

        if (block.name().contains("rail")) return block.withHandler(blockManager.getHandler("minecraft:rail"));
        if (block.name().contains("button")) return block.withHandler(blockManager.getHandler("minecraft:oak_button"));

        return block;
    }

    public static void breakBlockPostCart(PlayerBlockBreakEvent event) {
        if (event.isCancelled()) return;
        int ticksToBreak = BlockBreakCalculation.breakTicks(event.getBlock(), event.getPlayer());
        if (ticksToBreak == -1) return;
        if (!(event.getBlock().handler() instanceof OreBlockHandler)) {
            boolean canBreak = BlockValidator.canHarvestBlockLoot(event.getPlayer(), event.getBlock());
            if (canBreak) {
                ItemStack itemStack = BlockValidator.getItem(event.getBlock());

                if (itemStack != null) {
                    DropItem.dropItem(event.getInstance(), event.getBlockPosition(), itemStack);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    public static void startDiggingPostCart(PlayerStartDiggingEvent event) {
        if (event.isCancelled()) return;
    }
}
