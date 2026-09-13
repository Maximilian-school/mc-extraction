package ca.maximilian.extraction_game.core.mob;

import lombok.Getter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;

public class MobEntry extends EntityCreature {

    @Getter
    private final EntityType entityType;

    @Getter
    private final int minimumFloor;

    @Getter
    private final double weight;

    public MobEntry(EntityType entityType, int minimumFloor, double weight) {
        super(entityType);

        this.entityType = entityType;
        this.minimumFloor = minimumFloor;
        this.weight = weight;
    }

    public MobEntry(EntityType entityType, double weight) {
        super(entityType);

        this.entityType = entityType;
        this.minimumFloor = 0;
        this.weight = weight;
    }

    @Override
    public void update(long time) {
        super.update(time);

        Pos position = getPosition();
        double px = position.x();
        double py = position.y();
        double pz = position.z();

        boolean inExcludedBox = px >= -7 && px <= 7 &&
                py >= 1  && py <= 3 &&
                pz >= -7 && pz <= 7;

        if (inExcludedBox) this.kill();
    }
}
