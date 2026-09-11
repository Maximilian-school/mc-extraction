package ca.maximilian.extraction_game.core.utils;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;

import java.time.Duration;

public class DropItem {
    public static void dropItem(Instance instance, Point pos, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(ItemPrices.withPrices(itemStack));
        itemEntity.setPickupDelay(Duration.ofMillis(500));
        itemEntity.setInstance(instance, pos.add(0.5, 0.5, 0.5));
    }
}
