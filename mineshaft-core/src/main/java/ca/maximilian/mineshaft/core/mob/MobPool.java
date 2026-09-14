package ca.maximilian.mineshaft.core.mob;

import ca.maximilian.mineshaft.core.mob.entries.CaveSpider;
import ca.maximilian.mineshaft.core.mob.entries.Skeleton;
import ca.maximilian.mineshaft.core.mob.entries.Zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobPool {

    public static List<MobEntry> entries() {
        return List.of(
                new Zombie(),
                new Skeleton(),
                new CaveSpider()
        );
    }

    public static List<MobEntry> getPool(int floor) {
        List<MobEntry> pool = new ArrayList<>();

        for (MobEntry entry : entries()) {
            if (floor >= entry.getMinimumFloor()) {
                pool.add(entry);
            }
        }

        return pool;
    }

    public static MobEntry getRandomMob(int floor) {
        List<MobEntry> pool = getPool(floor);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        return pool.get(random.nextInt(pool.size()));
    }
}
