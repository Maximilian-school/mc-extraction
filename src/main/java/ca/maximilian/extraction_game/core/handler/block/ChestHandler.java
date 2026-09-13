package ca.maximilian.extraction_game.core.handler.block;

import ca.maximilian.extraction_game.ExtractionGame;
import ca.maximilian.extraction_game.core.loot.LootTable;
import ca.maximilian.extraction_game.core.loot.TableSelectionHelper;
import ca.maximilian.extraction_game.core.utils.DropItem;
import ca.maximilian.extraction_game.core.utils.ItemPrices;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.BlockActionPacket;
import net.minestom.server.sound.SoundEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestHandler implements BlockHandler {

    @Override
    public void onPlace(@NonNull Placement placement) {
        LootTable lootTable = TableSelectionHelper.getTable(placement.getBlock().nbt().getString("CustomName"), placement.getBlockPosition().asBlockVec());
        List<ItemStack> loot = lootTable.roll();

        Inventory inventory = ExtractionGame.chestInventoryManager.create(
                placement.getInstance(),
                placement.getBlockPosition(),
                InventoryType.CHEST_3_ROW,
                lootTable.displayFriendlyName().append(Component.text(" Chest"))
        );

        int inventorySize = inventory.getSize();

        List<Integer> availableSlots = new ArrayList<>(inventorySize);
        for (int i = 0; i < inventorySize; i++) {
            availableSlots.add(i);
        }

        Collections.shuffle(availableSlots);

        for (int i = 0; i < loot.size() && i < inventorySize; i++) {
            int randomSlot = availableSlots.get(i);
            ItemStack itemStack = loot.get(i);
            inventory.setItemStack(randomSlot, ItemPrices.withPrices(itemStack));
        }

        placement.getInstance().eventNode().addListener(InventoryCloseEvent.class, event -> {
            if (event.getInventory().equals(inventory)) {

                long activeViewers = inventory.getViewers().stream()
                        .filter(viewer -> viewer.getGameMode() != GameMode.SPECTATOR)
                        .count();

                if (activeViewers <= 1) {
                    BlockActionPacket closePacket = new BlockActionPacket(
                            placement.getBlockPosition(),
                            (byte) 1,
                            (byte) 0,
                            placement.getBlock()
                    );
                    placement.getInstance().sendGroupedPacket(closePacket);

                    Sound closeSound = Sound.sound(SoundEvent.BLOCK_CHEST_CLOSE, Sound.Source.BLOCK, 1.0f, 1.0f);
                    placement.getInstance().playSound(closeSound, placement.getBlockPosition());
                }
            }
        });
    }

    @Override
    public void onDestroy(@NonNull Destroy destroy) {
        Inventory inventory = ExtractionGame.chestInventoryManager.get(destroy.getInstance(), destroy.getBlockPosition());

        ExtractionGame.chestInventoryManager.remove(destroy.getInstance(), destroy.getBlockPosition());


        for (ItemStack itemStack : inventory.getItemStacks()) {
            DropItem.dropItem(destroy.getInstance(), destroy.getBlockPosition(), itemStack);
        }
    }

    @Override
    public boolean onInteract(Interaction interaction) {
        Inventory inventory = ExtractionGame.chestInventoryManager.get(interaction.getInstance(), interaction.getBlockPosition());

        long activeViewers = inventory.getViewers().stream()
                .filter(viewer -> viewer.getGameMode() != GameMode.SPECTATOR)
                .count();

        interaction.getPlayer().openInventory(inventory);

        if (interaction.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            if (activeViewers == 0) {
                BlockActionPacket openPacket = new BlockActionPacket(
                        interaction.getBlockPosition(),
                        (byte) 1,
                        (byte) 1,
                        interaction.getBlock()
                );
                interaction.getInstance().sendGroupedPacket(openPacket);

                Sound openSound = Sound.sound(SoundEvent.BLOCK_CHEST_OPEN, Sound.Source.BLOCK, 1.0f, 1.0f);
                interaction.getInstance().playSound(openSound, interaction.getBlockPosition());
            }
        }

        return true;
    }

    @Override
    public @NonNull Key getKey() {
        return Key.key("extraction:loot_chest");
    }
}