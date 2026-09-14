package ca.maximilian.mineshaft.lobby;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ca.maximilian.mineshaft.Mineshaft.LOBBY_INSTANCE;

public class LobbyInstanceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyInstanceUtils.class);

    public static void placeLobbySchematic() {
        try (InputStream inputStream = LobbyInstanceUtils.class.getClassLoader()
                .getResourceAsStream("mineshaft/lobby.schem")) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Schematic file not found in resources!");
            }

            byte[] schematicData = inputStream.readAllBytes();
            Schematic schematic = SchematicReader.detecting().read(schematicData);

            Pos startPos = new Pos(0, 64, 0);
            Point size = schematic.size();

            int minChunkX = startPos.blockX() >> 4;
            int maxChunkX = (startPos.blockX() + size.blockX()) >> 4;
            int minChunkZ = startPos.blockZ() >> 4;
            int maxChunkZ = (startPos.blockZ() + size.blockZ()) >> 4;

            List<CompletableFuture<Chunk>> chunkFutures = new ArrayList<>();

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    chunkFutures.add(LOBBY_INSTANCE.loadChunk(cx, cz));
                }
            }

            CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                schematic.createBatch().apply(LOBBY_INSTANCE, startPos, (batch) -> {
                    CompletableFuture.allOf(chunkFutures.toArray(CompletableFuture[]::new))
                            .thenRun(() -> LightingChunk.relight(LOBBY_INSTANCE, LOBBY_INSTANCE.getChunks()));
                });
            });

        } catch (IOException e) {
            LOGGER.error("Error while reading schematic file!", e);
        }
    }
}