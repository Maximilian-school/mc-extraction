package ca.maximilian.mineshaft.core.mob.entries;

import ca.maximilian.mineshaft.core.loot.tables.mob.ZombieLootTable;
import ca.maximilian.mineshaft.core.mob.MobEntry;
import net.minestom.server.entity.EntityType;
import net.minestom.server.sound.SoundEvent;

public class CaveSpider extends MobEntry {

    public CaveSpider() {
        super(EntityType.CAVE_SPIDER,
                2,
                10,
                new ZombieLootTable(),
                3f,
                SoundEvent.ENTITY_SPIDER_STEP,
                SoundEvent.ENTITY_SPIDER_DEATH
        );
    }
}
