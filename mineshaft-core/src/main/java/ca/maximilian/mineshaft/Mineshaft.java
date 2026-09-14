package ca.maximilian.mineshaft;

import ca.maximilian.mineshaft.command.ExtractionCommands;
import ca.maximilian.mineshaft.core.AmbientLightingChunk;
import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.core.gui.Gui;
import ca.maximilian.mineshaft.core.handler.BlockHandlers;
import ca.maximilian.mineshaft.core.event.EventHandlers;
import ca.maximilian.mineshaft.core.utils.ChestInventoryManager;
import ca.maximilian.mineshaft.lobby.LobbyInstanceUtils;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import io.github.togar2.pvp.MinestomPvP;
import lombok.Getter;
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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Mineshaft {

    public static Instance LOBBY_INSTANCE;

    @Getter
    private static Gui GUI;

    public static ChestInventoryManager chestInventoryManager = new ChestInventoryManager();

    public static final Component lobbyCompassName = Component.text("Lobbies").style(
            Style.style()
                    .decoration(TextDecoration.ITALIC, false)
                    .build()
    );

    static void main() {
        if (!isZGCEnabled() && !isEnhancedRedefinitionActive()) {
            System.err.println("Error: This application requires the Z Garbage Collector.");
            System.err.println("Please restart using the flag: -XX:+UseZGC");
            System.err.println("This is because when generating a mine shaft then having to GC would get really laggy, really fast.");
            System.err.println("\nThis can be overridden in debug sessions with JRebel or any JVM with enhanced class redefinition");

            System.exit(1);
        }

        System.setProperty("minestom.registry.unsafe-ops", "true");
        MinecraftServer minecraftServer = MinecraftServer.init();
        ExtractionCommands.init();
        MinestomPvP.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(ExtractionPlayer::new);
        GUI = new Gui();

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

    private static boolean isZGCEnabled() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("zgc") || name.contains("z garbage collector")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnhancedRedefinitionActive() {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : inputArguments) {
            if (arg.equals("-XX:+AllowEnhancedClassRedefinition") ||
                    arg.equals("-XX:+EnhancedClassRedefinition")) {
                return true;
            }
        }

        String vmName = System.getProperty("java.vm.name", "").toLowerCase();
        return vmName.contains("dcevm") || vmName.contains("jetbrains");
    }
}
