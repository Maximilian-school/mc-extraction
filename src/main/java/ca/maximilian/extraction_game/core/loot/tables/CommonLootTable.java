package ca.maximilian.extraction_game.core.loot.tables;

import ca.maximilian.extraction_game.core.loot.LootEntry;
import ca.maximilian.extraction_game.core.loot.LootTable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record CommonLootTable() implements LootTable {

    @Override
    public @NotNull Key id() {
        return Key.key("extraction:common");
    }

    @Override
    public @NotNull Component displayFriendlyName() {
        return Component.text("Common");
    }

    @Override
    public List<LootEntry> entries() {
        return List.of(
                new LootEntry(ItemStack.of(Material.ROTTEN_FLESH), 70, 1, 4),
                new LootEntry(ItemStack.of(Material.COAL), 50, 2, 5),
                new LootEntry(ItemStack.of(Material.IRON_INGOT), 10, 1, 1)
        );
    }

    @Override
    public int rollCount() {
        return ThreadLocalRandom.current().nextInt(3, 6);
    }
}