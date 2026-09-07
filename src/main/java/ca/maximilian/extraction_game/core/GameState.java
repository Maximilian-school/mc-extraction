package ca.maximilian.extraction_game.core;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public record GameState(@NotNull GameStateType type, Component reason) {
}
