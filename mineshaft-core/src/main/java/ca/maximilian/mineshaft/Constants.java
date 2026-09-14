package ca.maximilian.mineshaft;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.DimensionType;

import java.util.UUID;

public class Constants {

    public static final Tag<UUID> PLACED_BY_TAG = Tag.UUID("placed_by");

    public static final Sound ERROR_SOUND = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_BASS,
            Sound.Source.MASTER,
            1.0f,
            0.5f
            );

    public static final Sound SUCCESS_SOUND = Sound.sound(
            SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP,
            Sound.Source.MASTER,
            1.0f,
            1.0f
    );

    public static final Sound SELL_SOUND = Sound.sound(
            SoundEvent.ENTITY_PLAYER_LEVELUP,
            Sound.Source.MASTER,
            1.0f,
            1.0f
    );

    public static final BossBar INTERMISSION_BOSSBAR = BossBar.bossBar(
            Component.text("Intermission"),
            1,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
    );

    public static final RegistryKey<DimensionType> MAIN_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register("extraction:main", DimensionType.builder()
                    .ambientLight(1/15f)
                    .build());
}
