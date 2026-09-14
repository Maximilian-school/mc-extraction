package ca.maximilian.mineshaft.core.loot.tables;

import ca.maximilian.mineshaft.core.loot.LootEntry;
import ca.maximilian.mineshaft.core.loot.LootTable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record UncommonLootTable() implements LootTable {

    @Override
    public @NotNull Key id() {
        return Key.key("extraction:uncommon");
    }

    @Override
    public @NotNull Component displayFriendlyName() {
        return Component.text("Uncommon");
    }

    @Override
    public List<LootEntry> entries() {
        return List.of(
                new LootEntry(ItemStack.of(Material.ROTTEN_FLESH), 70, 1, 2),
                new LootEntry(ItemStack.of(Material.COAL), 70, 2, 5),
                new LootEntry(ItemStack.of(Material.IRON_INGOT), 20, 1, 1),
                new LootEntry(ItemStack.of(Material.GOLD_NUGGET), 1, 1, 3)
        );
    }

    @Override
    public int rollCount() {
        return ThreadLocalRandom.current().nextInt(3, 8);
    }
}