package ca.maximilian.mineshaft.core;

import ca.maximilian.mineshaft.Constants;
import ca.maximilian.mineshaft.Mineshaft;
import ca.maximilian.mineshaft.core.gui.ShopViewProvider;
import ca.maximilian.mineshaft.core.handler.Cart;
import ca.maximilian.mineshaft.core.mob.MobEntry;
import ca.maximilian.mineshaft.core.mob.MobPool;
import ca.maximilian.mineshaft.core.utils.GameState;
import ca.maximilian.mineshaft.core.utils.GameStateType;
import ca.maximilian.mineshaft.core.utils.InstanceUtils;
import ca.maximilian.mineshaft.core.utils.Timer;
import ca.maximilian.mineshaft.lobby.Party;
import ca.maximilian.mineshaft.worldgen.MineshaftGenerator;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.monster.skeleton.SkeletonMeta;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class RunningGame {

    public static final Tag<RunningGame> TAG = Tag.Transient("running_game");
    public static final Tag<Integer> MOB_FLOOR_TAG = Tag.Integer("mob_floor");

    private static final Logger LOGGER = LoggerFactory.getLogger(RunningGame.class);

    private final BossBar LOADING_BOSS_BAR = BossBar.bossBar(
            Component.text("Loading"),
            0,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
    );

    // Store event listeners for cleanup to prevent memory leaks
    private final List<Object> registeredListeners = new ArrayList<>();

    @Getter
    private final UUID runningGameUUID;

    @Getter
    private final Party party;

    @Getter
    private Entity shopkeeper;

    @Getter
    @Setter
    private Cart cart;

    @Getter
    private final Instance instance;

    @Getter
    private final List<Gate> gates = new ArrayList<>();

    @Getter
    private GameState gameState = new GameState(GameStateType.COLLAPSING, Component.text("Generating ancient tunnels."));

    // region TIMERS

    @Getter
    public Timer intermissionTimer = new Timer(Duration.ofSeconds(5));

    // endregion

    public RunningGame(Party party) {
        this.party = party;
        this.runningGameUUID = UUID.randomUUID();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer(Constants.MAIN_DIMENSION);
        instanceContainer.setTag(TAG, this);
        instanceContainer.setChunkSupplier(AmbientLightingChunk::new);
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(-64, 318, Block.STONE));

        this.addInstanceEvents(instanceContainer.eventNode());

        onGameStateChange((oldState, newState) -> {
            if (gameState.type() == GameStateType.COLLAPSING
                    || gameState.type() == GameStateType.LIGHTING
                    || gameState.type() == GameStateType.GENERATED) {
                float progress = switch (gameState.type()) {
                    case COLLAPSING -> 0f;
                    case LIGHTING -> 0.5f;
                    default -> 1f;
                };

                if (gameState.type() == GameStateType.GENERATED) {
                    MinecraftServer.getSchedulerManager().buildTask(() -> {
                        setGameState(new GameState(GameStateType.STARTING, Component.text("Started")));
                    }).delay(TaskSchedule.seconds(1)).schedule();
                }

                LOADING_BOSS_BAR.progress(progress);
                LOADING_BOSS_BAR.name(newState.reason());

                for (ExtractionPlayer player : party.getPlayers()) {
                    player.showBossBar(LOADING_BOSS_BAR);
                }
            } else {
                for (ExtractionPlayer player : party.getPlayers()) {
                    player.hideBossBar(LOADING_BOSS_BAR);
                }
            }
        });

        onGameStateChange((oldState, newState) -> {
            if (oldState.type() == GameStateType.GENERATED && gameState.type() == GameStateType.STARTING) {
                for (ExtractionPlayer player : party.getPlayers()) {
                    player.setInstance(instanceContainer, new Pos(0.5, 1, 0.5));
                }
            }
        });

        setGameState(gameState);

        List<CompletableFuture<Chunk>> chunks = new ArrayList<>();
        ChunkRange.chunksInRange(-8, -8, 16, (x, z) -> chunks.add(instanceContainer.loadChunk(x, z)));

        MineshaftGenerator.generateAsync(instanceContainer)
                .thenApply(ignored -> instanceContainer)
                .whenComplete((instance, failure) -> {
                    if (failure != null) {
                        setGameState(new GameState(GameStateType.CRASHED, Component.text("Failed to generate mineshaft")));
                        LOGGER.error("Failed to generate mineshaft", failure);
                        this.getParty().setCrashed();
                        throw new IllegalStateException(failure.getMessage());
                    }

                    CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new))
                            .thenRun(() -> {
                                setGameState(new GameState(GameStateType.LIGHTING,  Component.text("Precomputing lighting!")));

                                LightingChunk.relight(instance, instance.getChunks());

                                setGameState(new GameState(GameStateType.GENERATED, Component.text("Generated mineshaft!")));

                                Cart cart = new Cart();
                                setCart(cart);

                                cart.spawnCart(instance, new Pos(0.5, 1, 0.5));

                                gates.add(new Gate(instance, -7, -7, -3));
                                gates.add(new Gate(instance, -7, 3, 7));
                                gates.add(new Gate(instance, 7, -7, -3));
                                gates.add(new Gate(instance, 7, 3, 7));

                                for (Gate gate : gates) {
                                    gate.close(true);
                                }

                                CombatFeatureSet modernVanilla = CombatFeatures.modernVanilla();
                                instance.eventNode().addChild(modernVanilla.createNode());
                            });
                });

        registeredListeners.add(instanceContainer.eventNode().addListener(InstanceTickEvent.class,  this::tick));

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

    private void addInstanceEvents(EventNode<InstanceEvent> instanceNode) {
        registeredListeners.add(instanceNode.addListener(PickupItemEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                player.getInventory().addItemStack(event.getItemStack());
            } else {
                event.setCancelled(true);
            }
        }));

        registeredListeners.add(instanceNode.addListener(ItemDropEvent.class, event -> {
            if (event.getInstance() == Mineshaft.LOBBY_INSTANCE) {
                event.setCancelled(true);
                return;
            }

            Entity entity = event.getEntity();

            ItemEntity itemEntity = new ItemEntity(event.getItemStack());
            itemEntity.setPickupDelay(Duration.ofMillis(500));
            itemEntity.setInstance(instance, entity.getPosition().add(0, 1, 0));

            Vec direction = entity.getPosition().direction();

            Vec adjustedDirection = direction.add(0, 1, 0);

            itemEntity.setVelocity(adjustedDirection.mul(5));
        }));

        registeredListeners.add(instanceNode.addListener(EntityTickEvent.class, event -> {
            if (event.getEntity() instanceof MobEntry mobEntry) {
//                mobEntry.tick(mobEntry);
            }
        }));
    }

    private final List<BiConsumer<GameState, GameState>> gameStateChangeCallbacks = new ArrayList<>();

    public void onGameStateChange(BiConsumer<GameState, GameState> gameStateChange) {
        gameStateChangeCallbacks.add(gameStateChange);
    }

    public void setGameState(@NonNull GameState gameState) {
        if (gameState.type().equals(GameStateType.CLEANED_UP)) {
            throw new IllegalStateException("Cleaned up");
        }

        GameState oldGameState = this.gameState;
        this.gameState = gameState;

        for (BiConsumer<GameState, GameState> gameStateChangeCallback : gameStateChangeCallbacks) {
            try {
                gameStateChangeCallback.accept(oldGameState, gameState);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cleanupGame() {
        // Clear listener references. In Minestom, event listeners registered to
        // entity/nodes are typically cleaned up automatically when those entities/nodes
        // are destroyed. Since we're about to unregister the instance, all associated
        // listeners should be cleaned up with it.
        registeredListeners.clear();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        cart.despawnCart();

        instanceManager.unregisterInstance(getInstance());

        setGameState(new GameState(GameStateType.CLEANED_UP, null));
    }

    public void spawnShopKeeper(Point spawnPoint) {
        Entity shopkeeper = new Entity(EntityType.SKELETON);
        SkeletonMeta skeletonMeta = (SkeletonMeta) shopkeeper.getEntityMeta();

        shopkeeper.setCustomName(Component.text("Shopkeeper")
                .style(Style.style().decoration(TextDecoration.ITALIC, false).build()));

        String skinTexture = "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2M3MWJhYTU5MTE0NDRkMThjNjc1NzViOWVmNTM0M2E1ODY5Nzc3ZGYwYTA2ZTc0MGZjYWExYTk5Njc5MiJ9fX0";
        String skinSignature = "3c71baa5911444d18c67575b9ef5343a5869777df0a06e740fcaa1a996792";

//      skeletonMeta.setProfile(new ResolvableProfile(new PlayerSkin(skinTexture, skinSignature)));

        shopkeeper.setInstance(this.instance, spawnPoint);

        registeredListeners.add(instance.eventNode().addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getTarget() == shopkeeper) {
                ShopViewProvider myViewProvider = new ShopViewProvider(Mineshaft.getGUI().getViewRegistry());
                myViewProvider.open(event.getPlayer());
            }
        }));

        this.shopkeeper = shopkeeper;
    }

    public void tick(InstanceTickEvent tickEvent) {
        for (Gate gate : gates) {
            gate.update();
        }

        if (this.shopkeeper != null) {

            Pos centre = this.shopkeeper.getPosition();

            List<Player> playersInRange = instance.getPlayers().stream()
                    .filter(player -> player.getInstance() == instance)
                    .filter(player -> player.getPosition().distanceSquared(centre) <= 32)
                    .sorted(Comparator.comparingDouble(p -> p.getPosition().distanceSquared(centre)))
                    .filter(player -> shopkeeper.hasLineOfSight(player))
                    .filter(player -> player.getGameMode().equals(GameMode.SURVIVAL))
                    .toList();

            if (!playersInRange.isEmpty()) {
                Player closestPlayer = playersInRange.getFirst();

                if (closestPlayer != null) {
                    this.shopkeeper.lookAt(closestPlayer);
                }
            }
        }

        Map<Integer, Integer> mobsPerFloor = new HashMap<>();

        for (Entity entity : instance.getEntities()) {
            if (entity instanceof MobEntry) {
                int currentY = (int) entity.getPosition().y();
                int currentFloor = currentY / 5;

                mobsPerFloor.put(currentFloor, mobsPerFloor.getOrDefault(currentFloor, 0) + 1);
            }
        }

        var random = ThreadLocalRandom.current();

        for (var chunk : instance.getChunks()) {
            if (!chunk.isLoaded()) continue;

            int chunkX = chunk.getChunkX();
            int chunkZ = chunk.getChunkZ();

            if (chunkX < -4 || chunkX > 3 || chunkZ < -4 || chunkZ > 3) {
                continue;
            }

            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);

            int y = random.nextInt(-4, 33);

            int floor = y/5;

            int maxMobsForFloor = (floor + 1) * 40;
            int currentMobsOnFloor = mobsPerFloor.getOrDefault(floor, 0);

            if (currentMobsOnFloor >= maxMobsForFloor) {
                continue;
            }

            Pos candidatePos = new Pos(x, y, z);

            if (!InstanceUtils.isValidSpawnLight(instance, candidatePos)) continue;

            Pos groundPos = candidatePos.sub(0, 1, 0);

            boolean isGroundSolid = instance.getBlock(groundPos).solid();
            boolean isSpawnAir = instance.getBlock(candidatePos).air();

            if (isGroundSolid && isSpawnAir) {
                MobEntry spawnedMob = MobPool.getRandomMob(floor);

                int heightOffset = (int) Math.ceil(spawnedMob.getBoundingBox().height()) - 1;

                boolean obstructed = false;

                for (int yOffset = Math.toIntExact((long) candidatePos.y()); yOffset <= heightOffset; yOffset++) {
                    obstructed = instance.getBlock(candidatePos.add(0, yOffset, 0)).solid();
                    if (obstructed) break;
                }

                if (!obstructed) {
                    spawnedMob.setTag(MOB_FLOOR_TAG, floor);

                    spawnedMob.setInstance(instance, candidatePos.add(0.5, 0, 0.5));

                    mobsPerFloor.put(floor, currentMobsOnFloor + 1);

                    break;
                }
            }
        }
    }
}