package ca.maximilian.extraction_game.core;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.Cart;
import ca.maximilian.extraction_game.core.handler.event.EventHandlers;
import ca.maximilian.extraction_game.core.handler.event.StateChangeEvent;
import ca.maximilian.extraction_game.core.utils.GameState;
import ca.maximilian.extraction_game.core.utils.GameStateType;
import ca.maximilian.extraction_game.core.utils.Timer;
import ca.maximilian.extraction_game.lobby.Party;
import ca.maximilian.extraction_game.worldgen.MineshaftGenerator;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.UUID;

public class RunningGame {

    @Getter
    private final UUID runningGameUUID;

    private final Party party;

    @Getter
    @Setter
    private Cart cart;

    @Getter
    private final Instance instance;

    @Getter
    private GameState gameState = new GameState(GameStateType.INTERMISSION, Component.text("Server started"));

    // region TIMERS

    @Getter
    public Timer intermissionTimer = new Timer(Duration.ofSeconds(5));

    // endregion

    public RunningGame(Party party) {
        this.party = party;
        this.runningGameUUID = UUID.randomUUID();InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer(Constants.MAIN_DIMENSION);
        instanceContainer.setChunkSupplier(LightingChunk::new);
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(-64, 318, Block.STONE));

        EventHandlers.registerInstanceEvents(instanceContainer);

        MineshaftGenerator.generateAsync(instanceContainer)
                .thenApply(ignored -> instanceContainer)
                .whenComplete((instance, failure) -> {
                    if (failure != null) {
                        setGameState(new GameState(GameStateType.FAILED_TO_GENERATE, Component.text("Failed to generate mineshaft")));
                        throw new IllegalStateException(failure.getMessage());
                    }

                    Cart cart = new Cart();
                    setCart(cart);

                    cart.spawnCart(instanceContainer, new Pos(0, 1, 0));
                });

        this.instance = instanceContainer;

        this.registerTimerCallbacks();

        intermissionTimer.start();
    }

    private void registerTimerCallbacks() {
        this.intermissionTimer.onFinish(this::onIntermissionTimerFinished);
    }

    // region TIMER CALLBACKS

    private void onIntermissionTimerFinished() {
        this.setGameState(new GameState(GameStateType.STARTING, Component.text("Game is starting!")));
    }

    // endregion

    public void setGameState(@NonNull GameState gameState) {
        if (gameState.type().equals(GameStateType.CLEANED_UP)) throw new IllegalStateException("Cleaned up");

        this.gameState = gameState;

        StateChangeEvent stateChangeEvent = new StateChangeEvent(gameState, getRunningGameUUID());

        MinecraftServer.getGlobalEventHandler().call(stateChangeEvent);
    }

    public void cleanupGame() {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        instanceManager.unregisterInstance(getInstance());

        setGameState(new GameState(GameStateType.CLEANED_UP, null));
    }
}