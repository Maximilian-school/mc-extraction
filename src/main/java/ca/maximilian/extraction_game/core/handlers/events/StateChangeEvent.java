package ca.maximilian.extraction_game.core.handlers.events;

import ca.maximilian.extraction_game.core.GameState;
import net.minestom.server.event.Event;

public class StateChangeEvent implements Event {

    private final GameState gameState;

    public StateChangeEvent(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }
}
