package ca.maximilian.extraction_game.core.handler.event;

import ca.maximilian.extraction_game.ExtractionGame;
import ca.maximilian.extraction_game.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.time.Duration;

public class EventHandlers {
    public static <T extends PlayerInstanceEvent & CancellableEvent> void spectatorCancelEvent(T event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    private static void onPlayerConfigure(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        event.setSpawningInstance(ExtractionGame.LOBBY_INSTANCE);
        player.setRespawnPoint(new Pos(26.5, 80, 10.5));

        player.setGameMode(GameMode.SURVIVAL);
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

        globalEventHandler.addListener(PlayerLoadedEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendMessage(Component.text()
                    .append(Component.text("=".repeat(8) + " MineExtract " + "=".repeat(8) + "\n").color(NamedTextColor.GREEN))
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
    }

    public static void registerInstanceEvents(InstanceContainer instanceContainer) {
        EventNode<InstanceEvent> instanceEventEventNode = instanceContainer.eventNode();

        instanceEventEventNode.addListener(PickupItemEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                player.getInventory().addItemStack(event.getItemStack());
            } else {
                event.setCancelled(true);
            }
        });

        instanceEventEventNode.addListener(ItemDropEvent.class, event -> {
            if (event.getInstance() == ExtractionGame.LOBBY_INSTANCE) {
                event.setCancelled(true);
                return;
            }

            Entity entity = event.getEntity();

            ItemEntity itemEntity = new ItemEntity(event.getItemStack());
            itemEntity.setPickupDelay(Duration.ofMillis(500));
            itemEntity.setInstance(instanceContainer, entity.getPosition().add(0, 1, 0));

            Vec direction = entity.getPosition().direction();

            Vec adjustedDirection = direction.add(0, 1, 0);

            itemEntity.setVelocity(adjustedDirection.mul(5));
        });
    }
}
