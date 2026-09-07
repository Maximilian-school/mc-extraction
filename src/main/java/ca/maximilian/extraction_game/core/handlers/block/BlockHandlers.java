package ca.maximilian.extraction_game.core.handlers.block;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.time.Duration;

public class BlockHandlers {
    public static void registerBlockHandlers() {
        MinecraftServer.getBlockManager().registerHandler("minecraft:chest", ChestHandler::new);
        MinecraftServer.getBlockManager().registerHandler("minecraft:rail", RailHandler::new);
    }

    public static Block addHandler(Block block) {
        BlockManager blockManager = MinecraftServer.getBlockManager();

        if (block.name().contains("rail")) return block.withHandler(blockManager.getHandler("minecraft:rail"));

        return block;
    }

    public static void breakBlockPostCart(PlayerBlockBreakEvent event) {
        if (!(event.getBlock().handler() instanceof OreBlockHandler) && event.getBlock() != Block.COBWEB) {
            event.setCancelled(true);
        }
    }

    public static void startDiggingPostCart(PlayerStartDiggingEvent event) {
        if (event.isCancelled()) return;
        if (event.getBlock() == Block.COBWEB) {
            event.getInstance().breakBlock(event.getPlayer(), event.getBlockPosition(), event.getBlockFace());

            ItemEntity itemEntity = new ItemEntity(ItemStack.of(Material.STRING));
            itemEntity.setPickupDelay(Duration.ofMillis(500));
            itemEntity.setInstance(event.getInstance(), event.getBlockPosition().add(0.5, 0.5, 0.5));

            event.setCancelled(true);
        }
    }
}
