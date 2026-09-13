package ca.maximilian.extraction_game.core.mob.entries;

import ca.maximilian.extraction_game.core.mob.MobEntry;
import ca.maximilian.extraction_game.core.mob.RandomStrollGoal;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.time.TimeUnit;

import java.util.List;

public class Zombie extends MobEntry {

    private Pos lastPosition;
    private double accumulatedDistance = 0.0;
    private static final double DISTANCE_PER_STEP = 2;

    public Zombie() {
        super(EntityType.ZOMBIE, 100);

        addAIGroup(
                List.of(
                        new MeleeAttackGoal(this, 1.6, 6, TimeUnit.SECOND),
                        new RandomStrollGoal(this, 8, 6, TimeUnit.SECOND)
                ),
                List.of(
                        new LastEntityDamagerTarget(this, 24),
                        new ClosestEntityTarget(this, 16, entity -> {
                            if (!(entity instanceof Player player)) return false;

                            var playerPos = player.getPosition();
                            double px = playerPos.x();
                            double py = playerPos.y();
                            double pz = playerPos.z();

                            boolean inExcludedBox = px >= -7 && px <= 7 &&
                                    py >= 1  && py <= 3 &&
                                    pz >= -7 && pz <= 7;

                            if (inExcludedBox) return false;

                            int currentFloor = this.getPosition().blockY() / 5;
                            int playerFloor = playerPos.blockY() / 5;

                            boolean sightline = this.hasLineOfSight(player) || this.getDistance(player) <= 8;

                            return currentFloor == playerFloor || sightline;
                        })

                )
        );

        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.13f);

        this.lastPosition = this.getPosition();
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (this.getInstance() == null) return;

        Pos currentPos = this.getPosition();

        if (this.isOnGround() && lastPosition != null) {
            double dx = currentPos.x() - lastPosition.x();
            double dz = currentPos.z() - lastPosition.z();
            double distanceMoved = Math.sqrt(dx * dx + dz * dz);

            accumulatedDistance += distanceMoved;

            if (accumulatedDistance >= DISTANCE_PER_STEP) {
                playFootstepSound(currentPos);
                accumulatedDistance = 0.0;
            }
        }

        this.lastPosition = currentPos;
    }

    private void playFootstepSound(Pos pos) {
        Block blockBelow = this.getInstance().getBlock(pos.blockX(), pos.blockY() - 1, pos.blockZ());

        SoundEvent stepSoundEvent = getStepSoundForBlock(blockBelow);

        Sound footstep = Sound.sound(
                stepSoundEvent,
                Sound.Source.HOSTILE,
                0.2f,
                1.0f
        );

        this.getInstance().playSound(footstep, pos.x(), pos.y(), pos.z());
    }

    private SoundEvent getStepSoundForBlock(Block block) {
        String namespace = block.key().namespace();

        if (namespace.contains("stone") || namespace.contains("brick") || namespace.contains("concrete")) {
            return SoundEvent.ENTITY_ZOMBIE_STEP;
        } else if (namespace.contains("wood") || namespace.contains("planks")) {
            return SoundEvent.ENTITY_ZOMBIE_STEP;
        } else if (namespace.contains("grass") || namespace.contains("moss")) {
            return SoundEvent.ENTITY_ZOMBIE_STEP;
        } else if (namespace.contains("gravel") || namespace.contains("sand")) {
            return SoundEvent.ENTITY_ZOMBIE_STEP;
        }

        return SoundEvent.ENTITY_ZOMBIE_STEP;
    }
}
