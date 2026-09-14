/*
 * Copyright (c) Minestom Developers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://apache.org
 *
 *
 * Source can be found at net.minestom.server.entity.ai.goal.RandomStrollGoal
 *
 * File modified by Maximilian (ca.maximilian.extraction_game) to fit project needs.
 */

package ca.maximilian.mineshaft.core.mob;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.GoalSelector;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomStrollGoal extends GoalSelector {

    private final int radius;
    private final List<Vec> closePositions;
    private final Random random = new Random();

    private final long delay;

    private long lastStroll;

    public RandomStrollGoal(EntityCreature entityCreature, int radius, long delay, TemporalUnit timeUnit) {
        super(entityCreature);
        this.radius = radius;
        this.closePositions = getNearbyBlocks(radius);
        this.delay = Duration.of(delay, timeUnit).toNanos();
    }

    @Override
    public boolean shouldStart() {
        return System.nanoTime() - lastStroll >= delay;
    }

    @Override
    public void start() {
        int remainingAttempt = closePositions.size();
        while (remainingAttempt-- > 0) {
            final int index = random.nextInt(closePositions.size());
            final Vec position = closePositions.get(index);

            final var target = entityCreature.getPosition().add(position);
            final boolean result = entityCreature.getNavigator().setPathTo(target);
            if (result) {
                break;
            }
        }
    }

    @Override
    public void tick(long time) {
    }

    @Override
    public boolean shouldEnd() {
        return true;
    }

    @Override
    public void end() {
        this.lastStroll = System.nanoTime();
    }

    public int getRadius() {
        return radius;
    }

    private static List<Vec> getNearbyBlocks(int radius) {
        List<Vec> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocks.add(new Vec(x, y, z));
                }
            }
        }
        return blocks;
    }
}
