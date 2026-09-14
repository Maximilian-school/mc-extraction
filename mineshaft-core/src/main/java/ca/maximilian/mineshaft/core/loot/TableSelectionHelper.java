package ca.maximilian.mineshaft.core.loot;

import ca.maximilian.mineshaft.core.loot.tables.CommonLootTable;
import ca.maximilian.mineshaft.core.loot.tables.RareLootTable;
import ca.maximilian.mineshaft.core.loot.tables.UncommonLootTable;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.BlockVec;

import java.util.List;

public class TableSelectionHelper {

    public static final List<LootTable> LOOT_TABLES = List.of(
            new CommonLootTable(),
            new UncommonLootTable(),
            new RareLootTable()
    );

    public static LootTable getTable(String chestName, BlockVec blockPos) {
        Key tableKey = parseName(chestName);

        for (LootTable lootTable : LOOT_TABLES) {
            if (lootTable.id().value().equals(tableKey.value())) {
                return lootTable;
            }
        }

        throw new IllegalArgumentException("Table %s not found, %s".formatted(chestName, blockPos));
    }

    private static Key parseName(String string) {
        if (!string.startsWith("loot_table=")) throw new IllegalArgumentException("Invalid chest name: " + string);

        String[] keyBeforeParse = string.split("=")[1].split(":");

        return Key.key(keyBeforeParse[0], keyBeforeParse[1]);
    }
}
