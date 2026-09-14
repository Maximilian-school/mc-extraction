package ca.maximilian.mineshaft.core.mob;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.core.loot.LootTable;
import ca.maximilian.mineshaft.core.utils.DropItem;
import lombok.Getter;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class MobEntry extends EntityCreature {

    @Getter
    private final EntityType entityType;

    @Getter
    private final LootTable lootTable;

    @Getter
    private final int minimumFloor;

    @Getter
    private final double weight;

    @Getter
    final protected float attackDamage = 3.0f;
    @Getter
    final protected SoundEvent deathSound;
    @Getter
    final protected SoundEvent stepSound;

    private Pos lastPosition;
    private double accumulatedDistance = 0.0;
    private static final double DISTANCE_PER_STEP = 2;

    public MobEntry(EntityType entityType,
                    int minimumFloor,
                    double weight,
                    LootTable lootTable,
                    float attackDamage,
                    SoundEvent stepSound,
                    SoundEvent deathSound
                    ) {
        super(entityType);
        this.entityType = entityType;
        this.minimumFloor = minimumFloor;
        this.weight = weight;
        this.lootTable = lootTable;
        this.stepSound = stepSound;
        this.deathSound = deathSound;
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (this.getInstance() == null) return;

        Pos currentPos = this.getPosition();

        if (this.lastPosition == null) {
            this.lastPosition = currentPos;
        }

        if (this.isOnGround()) {
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

    protected SoundEvent getStepSoundForBlock(Block block) {
        return this.stepSound;
    }

    @Override
    public void attack(Entity target, boolean swingHand) {
        super.attack(target, swingHand);
        if (target instanceof ExtractionPlayer player) {
            player.damage(Damage.fromEntity(this, attackDamage));
        }
    }

    @Override
    public void kill() {
        super.kill();
        if (deathSound != null && instance != null) {
            instance.playSound(
                    Sound.sound(
                            deathSound,
                            Sound.Source.HOSTILE,
                            1, ThreadLocalRandom.current().nextFloat(0.8f, 1.2f)
                    ), this.getPosition()
            );

            List<ItemStack> itemsToDrop = this.lootTable.roll();

            for  (ItemStack itemStack : itemsToDrop) {
                DropItem.explodeItems(instance, this.getPosition(), itemStack);
            }
        }
    }

    protected boolean isValidTarget(Player player) {
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
    }
}