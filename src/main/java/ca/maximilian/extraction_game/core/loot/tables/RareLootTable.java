package ca.maximilian.extraction_game.core.loot.tables;

import ca.maximilian.extraction_game.core.loot.LootEntry;
import ca.maximilian.extraction_game.core.loot.LootTable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RareLootTable implements LootTable {
    @Override
    public @NotNull Key id() {
        return Key.key("extraction:rare");
    }

    @Override
    public @NotNull Component displayFriendlyName() {
        return Component.text("Rare");
    }

    @Override
    public List<LootEntry> entries() {
        return List.of(
                new LootEntry(ItemStack.of(Material.COAL), 20, 2, 8),
                new LootEntry(ItemStack.of(Material.IRON_INGOT), 50, 3, 4),
                new LootEntry(ItemStack.of(Material.GOLD_NUGGET), 40, 2, 3)
        );
    }

    @Override
    public int rollCount() {
        return ThreadLocalRandom.current().nextInt(3, 6);
    }
}
