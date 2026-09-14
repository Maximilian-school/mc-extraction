package ca.maximilian.mineshaft.core.event;

import ca.maximilian.mineshaft.Mineshaft;
import ca.maximilian.mineshaft.Constants;
import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;

public class EventHandlers {
    public static <T extends PlayerInstanceEvent & CancellableEvent> void spectatorCancelEvent(T event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    private static void onPlayerConfigure(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        event.setSpawningInstance(Mineshaft.LOBBY_INSTANCE);
        player.setRespawnPoint(new Pos(26.5, 80, 10.5));

        player.setGameMode(GameMode.SURVIVAL);
    }

    private static void onPlayerLeave(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();

        Party party = Matchmaking.getPartyWithPlayer(player);

        if (party != null) {
            party.removePlayer((ExtractionPlayer) player, false);
        }
    }

    public static void register() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, EventHandlers::onPlayerConfigure);

        globalEventHandler.addListener(PlayerBlockPlaceEvent.class, event -> {
            Player player = event.getPlayer();

            Block modifiedBlock = event.getBlock()
                    .withTag(Constants.PLACED_BY_TAG, player.getUuid());

            event.setBlock(modifiedBlock);
        });

        Mineshaft.LOBBY_INSTANCE.eventNode().addListener(PlayerLoadedEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendMessage(Component.text()
                    .append(Component.text("=".repeat(8) + " Mineshaft " + "=".repeat(8) + "\n").color(NamedTextColor.GREEN))
                    .append(Component.text("Welcome to mine extract!\n")).color(NamedTextColor.BLUE)
                    .append(Component.text("We recommend using shaders, especially the following:\n\n")).color(NamedTextColor.BLUE)

                    .append(Component.text("Photon\n").color(NamedTextColor.RED).clickEvent(ClickEvent.openUrl("https://modrinth.com/shader/photon-shader")))
                    .append(Component.text("Bliss\n").color(NamedTextColor.YELLOW).clickEvent(ClickEvent.openUrl("https://modrinth.com/shader/bliss-shader")))
                    .append(Component.text("BSL\n\n").color(NamedTextColor.GOLD).clickEvent(ClickEvent.openUrl("https://modrinth.com/shader/bsl-shaders")))

                    .append(Component.text("=".repeat(27) + "\n").color(NamedTextColor.GREEN))

                    .append(Component.text("Click here to quick play!")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/party quickplay"))
                    )

                    .build()
            );
        });

        globalEventHandler.addListener(InventoryPreClickEvent.class, EventHandlers::spectatorCancelEvent);

        globalEventHandler.addListener(PlayerDisconnectEvent.class, EventHandlers::onPlayerLeave);
    }
}
