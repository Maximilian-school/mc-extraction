package ca.maximilian.mineshaft.core.mob.entries;

import ca.maximilian.mineshaft.core.loot.tables.mob.ZombieLootTable;
import ca.maximilian.mineshaft.core.mob.MobEntry;
import net.minestom.server.entity.EntityType;
import net.minestom.server.sound.SoundEvent;

public class Skeleton extends MobEntry {

    public Skeleton() {
        super(EntityType.SKELETON,
                2,
                10,
                new ZombieLootTable(),
                3f,
                SoundEvent.ENTITY_SKELETON_STEP,
                SoundEvent.ENTITY_SKELETON_DEATH
        );
    }
}
