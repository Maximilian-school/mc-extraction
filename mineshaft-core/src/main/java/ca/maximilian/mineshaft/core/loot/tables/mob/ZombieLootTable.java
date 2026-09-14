package ca.maximilian.mineshaft.core.loot.tables.mob;

import ca.maximilian.mineshaft.core.loot.LootEntry;
import ca.maximilian.mineshaft.core.loot.LootTable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZombieLootTable implements LootTable {
    @Override
    public @NotNull Key id() {
        return Key.key("zombie");
    }

    @Override
    public @NotNull Component displayFriendlyName() {
        return Component.text("Zombie");
    }

    @Override
    public List<LootEntry> entries() {
        return List.of(
                new LootEntry(ItemStack.of(Material.ROTTEN_FLESH), 100, 1, 3)
        );
    }
}
