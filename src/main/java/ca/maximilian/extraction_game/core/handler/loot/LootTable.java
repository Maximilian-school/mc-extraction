package ca.maximilian.extraction_game.core.handler.loot;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public interface LootTable {
    @NotNull Key id();
    @NotNull Component displayFriendlyName();

    List<LootEntry> entries();

    /**
     * Number of loot stacks to roll.
     * Override this per loot table.
     */
    default int rollCount() {
        return 1;
    }

    /**
     * Rolls loot from this table.
     */
    default List<ItemStack> roll() {
        int rollCount = rollCount();

        if (rollCount <= 0 || entries().isEmpty()) {
            return List.of();
        }

        List<ItemStack> loot = new ArrayList<>(rollCount);

        for (int i = 0; i < rollCount; i++) {
            ItemStack stack = rollSingleItem();

            if (!stack.isAir()) {
                loot.add(stack);
            }
        }

        return loot;
    }

    /**
     * Rolls a single item from this table.
     */
    default ItemStack rollSingleItem() {
        if (entries().isEmpty()) {
            return ItemStack.AIR;
        }

        int totalWeight = entries().stream()
                .mapToInt(LootEntry::weight)
                .sum();

        if (totalWeight <= 0) {
            return ItemStack.AIR;
        }

        int randomRoll = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        for (LootEntry entry : entries()) {
            currentWeight += entry.weight();

            if (randomRoll < currentWeight) {
                return entry.itemStack();
            }
        }

        return ItemStack.AIR;
    }
}