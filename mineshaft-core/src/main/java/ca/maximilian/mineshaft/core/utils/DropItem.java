package ca.maximilian.mineshaft.core.utils;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class DropItem {
    public static void dropItem(Instance instance, Point pos, ItemStack itemStack) {
        dropItem(instance, pos, itemStack, true);
    }

    public static void dropItem(Instance instance, Point pos, ItemStack itemStack, boolean centre) {
        explodeItems(instance, centre ? pos.add(0.5, 0.5, 0.5) : pos, itemStack);
    }

    public static void explodeItems(Instance instance, Point pos, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(ItemPrices.withPrices(itemStack));
        itemEntity.setPickupDelay(Duration.ofMillis(500));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;

        double horizontalSpeed = 5.0;
        double verticalSpeed = 4.0;

        double x = Math.cos(angle) * horizontalSpeed;
        double z = Math.sin(angle) * horizontalSpeed;
        double y = verticalSpeed;

        itemEntity.setVelocity(new Vec(x, y, z));

        itemEntity.setInstance(instance, pos);
    }
}
