package ca.maximilian.extraction_game;

import ca.maximilian.extraction_game.command.ExtractionCommands;
import ca.maximilian.extraction_game.core.AmbientLightingChunk;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.core.handler.BlockHandlers;
import ca.maximilian.extraction_game.core.event.EventHandlers;
import ca.maximilian.extraction_game.core.utils.ChestInventoryManager;
import ca.maximilian.extraction_game.lobby.LobbyInstanceUtils;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ExtractionGame {

    public static Instance LOBBY_INSTANCE;

    public static ChestInventoryManager chestInventoryManager = new ChestInventoryManager();

    public static final Component lobbyCompassName = Component.text("Lobbies").style(
            Style.style()
                    .decoration(TextDecoration.ITALIC, false)
                    .build()
    );

    static void main() {
        System.setProperty("minestom.registry.unsafe-ops", "true");
        MinecraftServer minecraftServer = MinecraftServer.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(CustomPlayer::new);
        ExtractionCommands.init();

        BlockHandlers.registerBlockHandlers();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        RegistryKey<DimensionType> lobbyDimension = MinecraftServer.getDimensionTypeRegistry()
                .register("extraction:lobby", DimensionType.builder()
                        .ambientLight(3/15F)
                        .skybox(DimensionType.Skybox.OVERWORLD)
                        .build());

        LOBBY_INSTANCE = instanceManager.createInstanceContainer(lobbyDimension);
        LOBBY_INSTANCE.setChunkSupplier(AmbientLightingChunk::new);
        LobbyInstanceUtils.placeLobbySchematic();

        List<CompletableFuture<Chunk>> chunks = new ArrayList<>();
        ChunkRange.chunksInRange(-8, -8, 16, (x, z) -> chunks.add(LOBBY_INSTANCE.loadChunk(x, z)));

        CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    LightingChunk.relight(LOBBY_INSTANCE, LOBBY_INSTANCE.getChunks());
                });

        EventHandlers.register();

        MinecraftServer.getSchedulerManager().buildTask(Matchmaking::tickParties).repeat(TaskSchedule.tick(1)).schedule();

        /*
         * Forces Constants' static initializer (and the dimension type registration) to run before any player can connect
         */
        Constants.MAIN_DIMENSION.key();

        minecraftServer.start("0.0.0.0", 25565);
    }
}
