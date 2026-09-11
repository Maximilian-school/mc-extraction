package ca.maximilian.extraction_game.core.utils;

public enum GameStateType {
    INTERMISSION,
    STARTING,
    STARTED,
    ENDING,
    ENDED,

    // NON GAMEPLAY EVENTS
    CLEANED_UP,
    FAILED_TO_GENERATE,
}
