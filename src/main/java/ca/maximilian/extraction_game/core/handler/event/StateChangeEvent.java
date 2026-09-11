package ca.maximilian.extraction_game.core.handler.event;

import ca.maximilian.extraction_game.core.utils.GameState;
import lombok.Getter;
import net.minestom.server.event.Event;

import java.util.UUID;

public class StateChangeEvent implements Event {

    @Getter
    private final GameState gameState;
    @Getter
    private final UUID runningGameUUID;

    public StateChangeEvent(GameState gameState, UUID runningGameUUID) {
        this.gameState = gameState;
        this.runningGameUUID = runningGameUUID;
    }

}
