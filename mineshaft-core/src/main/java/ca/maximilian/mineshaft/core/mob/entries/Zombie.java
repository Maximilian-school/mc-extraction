package ca.maximilian.mineshaft.core.mob.entries;

import ca.maximilian.mineshaft.core.loot.tables.mob.ZombieLootTable;
import ca.maximilian.mineshaft.core.mob.MobEntry;
import ca.maximilian.mineshaft.core.mob.RandomStrollGoal;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.time.TimeUnit;

import java.util.List;

public class Zombie extends MobEntry {

    public Zombie() {
        super(EntityType.ZOMBIE,
                0,
                100,
                new ZombieLootTable(),
                3f,
                SoundEvent.ENTITY_ZOMBIE_STEP,
                SoundEvent.ENTITY_ZOMBIE_DEATH
        );

        addAIGroup(
                List.of(
                        new MeleeAttackGoal(this, 1.6, 20, TimeUnit.SERVER_TICK),
                        new RandomStrollGoal(this, 8, 6, TimeUnit.SECOND)
                ),
                List.of(
                        new LastEntityDamagerTarget(this, 24),
                        new ClosestEntityTarget(this, 16, entity -> {
                            if (!(entity instanceof Player player)) return false;

                            return isValidTarget(player);
                        })
                )
        );

        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.13f);
    }
}