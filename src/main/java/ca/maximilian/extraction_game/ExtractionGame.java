package ca.maximilian.extraction_game;

import ca.maximilian.extraction_game.core.handlers.events.EventHandlers;
import ca.maximilian.extraction_game.core.handlers.events.StateChangeEvent;
import ca.maximilian.extraction_game.core.*;
import ca.maximilian.extraction_game.core.handlers.block.BlockHandlers;
import ca.maximilian.extraction_game.core.utils.ChestInventoryManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.time.Duration;

public class ExtractionGame {
    public static GameState GAME_STATE = new GameState(GameStateType.INTERMISSION, Component.text("Server started"));
    public static Instance LOBBY_INSTANCE;
    public static Instance MAIN_INSTANCE;

    public static Cart CART;

    public static ChestInventoryManager chestInventoryManager = new ChestInventoryManager();

    public static Timer INTERMISSION_TIMER = new Timer(Duration.ofSeconds(5));

    static void main() {
        System.setProperty("minestom.registry.unsafe-ops", "true");
        MinecraftServer minecraftServer = MinecraftServer.init();

        BlockHandlers.registerBlockHandlers();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        RegistryKey<DimensionType> lobbyDimension = MinecraftServer.getDimensionTypeRegistry()
                .register("extraction:lobby", DimensionType.builder()
                        .ambientLight(0.0F)
                        .skylight(false)
                        .skybox(DimensionType.Skybox.NONE)
                        .build());

        LOBBY_INSTANCE = instanceManager.createInstanceContainer(lobbyDimension);
        LOBBY_INSTANCE.setChunkSupplier(LightingChunk::new);

        EventHandlers.register();
        CoreLoop.start();

        INTERMISSION_TIMER.start();

        minecraftServer.start("0.0.0.0", 25565);
    }

    public static void setGameState(GameState gameState) {
        GAME_STATE = gameState;

        StateChangeEvent stateChangeEvent = new StateChangeEvent(gameState);

        MinecraftServer.getGlobalEventHandler().call(stateChangeEvent);
    }
}
