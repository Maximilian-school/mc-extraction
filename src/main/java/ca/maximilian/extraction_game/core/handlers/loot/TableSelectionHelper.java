package ca.maximilian.extraction_game.core.handlers.loot;

import ca.maximilian.extraction_game.core.handlers.loot.tables.CommonLootTable;
import ca.maximilian.extraction_game.core.handlers.loot.tables.UncommonLootTable;
import net.kyori.adventure.key.Key;

import java.util.List;

public class TableSelectionHelper {

    public static final List<LootTable> LOOT_TABLES = List.of(
            new CommonLootTable(),
            new UncommonLootTable()
    );

    public static LootTable getTable(String chestName) {
        Key tableKey = parseName(chestName);

        for (LootTable lootTable : LOOT_TABLES) {
            if (lootTable.id().value().equals(tableKey.value())) {
                return lootTable;
            }
        }

        throw new IllegalArgumentException("Table " + chestName + " not found");
    }

    private static Key parseName(String string) {
        if (!string.startsWith("loot_table=")) throw new IllegalArgumentException("Invalid chest name: " + string);

        String[] keyBeforeParse = string.split("=")[1].split(":");

        return Key.key(keyBeforeParse[0], keyBeforeParse[1]);
    }
}
