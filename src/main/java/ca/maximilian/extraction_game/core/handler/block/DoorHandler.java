package ca.maximilian.extraction_game.core.handler.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.sound.SoundEvent;
import org.jspecify.annotations.NonNull;

public class DoorHandler implements BlockHandler {

    @Override
    public boolean onInteract(@NonNull Interaction interaction) {
        Block clickedBlock = interaction.getBlock();
        Instance instance = interaction.getInstance();
        Point pos = interaction.getBlockPosition();

        String openProp = clickedBlock.getProperty("open");
        if (openProp == null) {
            return false;
        }

        boolean isOpen = Boolean.parseBoolean(openProp);
        String nextState = String.valueOf(!isOpen);

        SoundEvent soundEvent = isOpen ? SoundEvent.BLOCK_WOODEN_DOOR_CLOSE : SoundEvent.BLOCK_WOODEN_DOOR_OPEN;

        Sound doorSound = Sound.sound(soundEvent, Sound.Source.BLOCK, 1.0f, 1.0f);

        instance.playSound(doorSound, pos);

        instance.setBlock(pos, clickedBlock.withProperty("open", nextState));

        String halfProp = clickedBlock.getProperty("half");
        if ("lower".equals(halfProp)) {
            Point upperPos = pos.add(0, 1, 0);
            Block upperBlock = instance.getBlock(upperPos);

            if (upperBlock.id() == clickedBlock.id()) {
                instance.setBlock(upperPos, upperBlock.withProperty("open", nextState));
            }
        } else if ("upper".equals(halfProp)) {
            Point lowerPos = pos.sub(0, 1, 0);
            Block lowerBlock = instance.getBlock(lowerPos);

            if (lowerBlock.id() == clickedBlock.id()) {
                instance.setBlock(lowerPos, lowerBlock.withProperty("open", nextState));
            }
        }

        return true;
    }

    @Override
    public @NonNull Key getKey() {
        return Key.key("extraction:door");
    }
}