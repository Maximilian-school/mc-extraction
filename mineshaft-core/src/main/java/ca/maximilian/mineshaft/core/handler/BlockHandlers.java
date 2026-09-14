package ca.maximilian.mineshaft.core.handler;

import ca.maximilian.mineshaft.core.handler.block.*;
import ca.maximilian.mineshaft.core.utils.DropItem;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.block.BlockBreakCalculation;

public class BlockHandlers {
    public static void registerBlockHandlers() {
        MinecraftServer.getBlockManager().registerHandler("minecraft:chest", ChestHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:rail", RailHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:oak_button", ButtonHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:oak_door", DoorHandler::new);
    }

    public static Block addHandler(Block block) {
        BlockManager blockManager = MinecraftServer.getBlockManager();

        if (block.name().contains("rail")) return block.withHandler(blockManager.getHandler("minecraft:rail"));
        if (block.name().contains("button")) return block.withHandler(blockManager.getHandler("minecraft:oak_button"));
        if (block.name().contains("door")) return block.withHandler(blockManager.getHandler("minecraft:oak_door"));

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
//                event.setCancelled(true);
            }
        }
    }

    public static void startDiggingPostCart(PlayerStartDiggingEvent event) {
        if (event.isCancelled()) return;
    }
}
