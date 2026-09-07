package ca.maximilian.extraction_game.core;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class Constants {

    public static final BossBar INTERMISSION_BOSSBAR = BossBar.bossBar(
            Component.text("Intermission"),
            1,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
    );
}
