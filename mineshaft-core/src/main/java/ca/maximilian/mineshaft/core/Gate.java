package ca.maximilian.mineshaft.core;

import lombok.Getter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class Gate {

    @Getter
    private final Instance instance;

    @Getter
    private final int z;

    @Getter
    private final int minX;

    @Getter
    private final int maxX;

    @Getter
    private boolean open;

    private int progress = 0;
    private long lastUpdate = 0;

    private static final long ANIMATION_DELAY = 200;

    public Gate(Instance instance, int z, int minX, int maxX) {
        this.instance = instance;
        this.z = z;
        this.minX = minX;
        this.maxX = maxX;

        close(true);
    }

    public void update() {
        if (isOccupied()) {
            open();
        } else {
            close();
        }
    }

    public void open() {
        if (progress >= 3) {
            open = true;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdate < ANIMATION_DELAY) {
            return;
        }

        lastUpdate = now;

        int y = progress + 1;
        setRow(y, Block.AIR);

        progress++;

        if (progress >= 3) {
            open = true;
        }
    }

    public void close() {
        if (progress <= 0) {
            open = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdate < ANIMATION_DELAY) {
            return;
        }

        lastUpdate = now;

        int y = progress;

        Block bars = Block.IRON_BARS
                .withProperty("north", "false")
                .withProperty("south", "false")
                .withProperty("east", "true")
                .withProperty("west", "true");

        setRow(y, bars);

        progress--;

        if (progress <= 0) {
            open = false;
        }
    }

    public void close(boolean skipAnimation) {
        if (!skipAnimation) {
            close();
            return;
        }

        Block bars = Block.IRON_BARS
                .withProperty("north", "false")
                .withProperty("south", "false")
                .withProperty("east", "true")
                .withProperty("west", "true");

        for (int y = 1; y <= 3; y++) {
            setRow(y, bars);
        }

        progress = 0;
        open = false;
        lastUpdate = System.currentTimeMillis();
    }

    private void setRow(int y, Block block) {
        for (int x = minX + 1; x <= maxX - 1; x++) {
            instance.setBlock(new Pos(x, y, z), block);
        }
    }

    private boolean isOccupied() {
        return instance.getPlayers().stream()
                .filter(player -> player.getInstance() == instance)
                .anyMatch(this::isPlayerInside);
    }

    private boolean isPlayerInside(Player player) {
        Pos pos = player.getPosition();

        return pos.x() >= minX
                && pos.x() <= maxX
                && pos.y() >= 0
                && pos.y() <= 4
                && pos.z() >= z - 2
                && pos.z() <= z + 2;
    }
}