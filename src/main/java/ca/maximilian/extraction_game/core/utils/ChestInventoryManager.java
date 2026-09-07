package ca.maximilian.extraction_game.core.utils;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestInventoryManager {

    private record ChestLocation(Instance instance, Point position) {}

    private final Map<ChestLocation, Inventory> inventories = new HashMap<>();

    public Inventory create(
            Instance instance,
            Point position,
            InventoryType type,
            Component title
    ) {
        Inventory inventory = new Inventory(type, title);

        inventories.put(new ChestLocation(instance, position), inventory);

        return inventory;
    }

    public Inventory get(
            Instance instance,
            Point position
    ) {
        return inventories.get(
                new ChestLocation(instance, position)
        );
    }

    public Inventory remove(
            Instance instance,
            Point position
    ) {
        return inventories.remove(
                new ChestLocation(instance, position)
        );
    }

    public boolean contains(
            Instance instance,
            Point position
    ) {
        return inventories.containsKey(
                new ChestLocation(instance, position)
        );
    }
}