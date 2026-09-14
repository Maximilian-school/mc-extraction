package ca.maximilian.mineshaft.core.utils;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public record GameState(@NotNull GameStateType type, Component reason) {
}
