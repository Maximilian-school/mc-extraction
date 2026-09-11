package ca.maximilian.extraction_game;

import ca.maximilian.extraction_game.command.ExtractionCommands;
import ca.maximilian.extraction_game.core.CoreLoop;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.core.RunningGame;
import ca.maximilian.extraction_game.core.handler.block.BlockHandlers;
import ca.maximilian.extraction_game.core.handler.event.EventHandlers;
import ca.maximilian.extraction_game.core.utils.ChestInventoryManager;
import ca.maximilian.extraction_game.lobby.LobbyInstanceUtils;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.util.ArrayList;
import java.util.List;

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
                .register("minestom:lobby", DimensionType.builder()
                        .ambientLight(1F)
                        .skybox(DimensionType.Skybox.OVERWORLD)
                        .build());

        LOBBY_INSTANCE = instanceManager.createInstanceContainer(lobbyDimension);
        LobbyInstanceUtils.placeLobbySchematic();
        LOBBY_INSTANCE.setChunkSupplier(LightingChunk::new);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            Party party = Matchmaking.getPartyWithPlayer(event.getPlayer());

            if (party == null) return;

            party.removePlayer((CustomPlayer) event.getPlayer(), false);
        });

        EventHandlers.register();
        CoreLoop.start();

        minecraftServer.start("0.0.0.0", 25565);
    }
}
