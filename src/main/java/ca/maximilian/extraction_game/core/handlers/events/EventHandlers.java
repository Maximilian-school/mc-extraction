package ca.maximilian.extraction_game.core.handlers.events;

import ca.maximilian.extraction_game.Cart;
import ca.maximilian.extraction_game.ExtractionGame;
import ca.maximilian.extraction_game.core.Constants;
import ca.maximilian.extraction_game.core.GameState;
import ca.maximilian.extraction_game.core.GameStateType;
import ca.maximilian.extraction_game.worldgen.MineshaftGenerator;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
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
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.time.Duration;

public class EventHandlers {

    private static void genericCancelEvent(CancellableEvent event) {
        event.setCancelled(true);
    }

    public static <T extends PlayerInstanceEvent & CancellableEvent> void spectatorCancelEvent(T event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    private static void onIntermissionTimerFinish() {
        ExtractionGame.setGameState(new GameState(GameStateType.STARTING, null));
    }

    private static void onStateChange(StateChangeEvent event) {
        if (event.getGameState().type() == GameStateType.INTERMISSION) {
            Audiences.players().showBossBar(Constants.INTERMISSION_BOSSBAR);
        } else {
            Audiences.players().hideBossBar(Constants.INTERMISSION_BOSSBAR);
        }

        if (event.getGameState().type().equals(GameStateType.STARTING)) {
            InstanceManager instanceManager = MinecraftServer.getInstanceManager();
            InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

            instanceContainer.setChunkSupplier(LightingChunk::new);
            instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(-64, 318, Block.STONE));

            registerInstanceEvents(instanceContainer);

            MineshaftGenerator.generateAsync(instanceContainer)
                    .thenApply(ignored -> instanceContainer)
                    .whenComplete((instance, failure) -> {
                        if (failure != null) onStateChange(event);
                    });

            ExtractionGame.MAIN_INSTANCE = instanceContainer;

            Cart cart = new Cart();
            ExtractionGame.CART = cart;

            cart.spawnCart(instanceContainer, new Pos(0, 1, 0));

            MinecraftServer.getSchedulerManager().buildTask(() -> {
                ExtractionGame.setGameState(new GameState(GameStateType.STARTED, null));
            }).delay(Duration.ofSeconds(1)).schedule();
        }

        if (event.getGameState().type().equals(GameStateType.STARTED)) {
            for (Player player : ExtractionGame.LOBBY_INSTANCE.getPlayers()) {
                player.setInstance(ExtractionGame.MAIN_INSTANCE);
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().addItemStack(ItemStack.of(Material.IRON_PICKAXE));
            }
        }
    }

    private static void onPlayerConfigure(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        event.setSpawningInstance(ExtractionGame.LOBBY_INSTANCE);
        player.setRespawnPoint(new Pos(0, 0, 0));

        player.setGameMode(GameMode.SPECTATOR);
    }

    public static void register() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, EventHandlers::onPlayerConfigure);
        globalEventHandler.addListener(StateChangeEvent.class, EventHandlers::onStateChange);

        globalEventHandler.addListener(InventoryPreClickEvent.class, EventHandlers::spectatorCancelEvent);

        ExtractionGame.INTERMISSION_TIMER.onFinish(EventHandlers::onIntermissionTimerFinish);
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
