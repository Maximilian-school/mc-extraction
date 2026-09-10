package ca.maximilian.extraction_game.core;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class Constants {

    public static final BossBar INTERMISSION_BOSSBAR = BossBar.bossBar(
            Component.text("Intermission"),
            1,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
    );

    public static final RegistryKey<DimensionType> MAIN_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register("extraction:lobby", DimensionType.builder()
                    .ambientLight(0.0F)
                    .skylight(false)
                    .skybox(DimensionType.Skybox.NONE)
                    .build());
}
