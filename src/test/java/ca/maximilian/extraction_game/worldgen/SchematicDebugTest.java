package ca.maximilian.extraction_game.worldgen;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchematicDebugTest {

    @BeforeAll
    static void setup() {
        MinecraftServer.init();
    }

    @Test
    void printAllSegmentInfo() throws Exception {
        String[] files = {"straight", "corner", "crossroads", "deadend", "tjunction", "stairwell"};

        for (String name : files) {
            String path = "/extraction_game/" + name + ".nbt";
            var stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.out.println("=== " + name + " === FILE NOT FOUND at " + path);
                continue;
            }
            byte[] bytes = stream.readAllBytes();
            Schematic schematic = SchematicReader.detecting().read(bytes);

            int width = Math.toIntExact(Math.round(schematic.size().x()));
            int height = Math.toIntExact(Math.round(schematic.size().y()));
            int length = Math.toIntExact(Math.round(schematic.size().z()));

            System.out.println("=== " + name + ".nbt ===");
            System.out.println("  Size: " + width + " x " + height + " x " + length + "  (W x H x L)");

            // Offset
            var offset = schematic.offset();
            if (offset != null) {
                System.out.println("  Offset: " + offset);
            } else {
                System.out.println("  Offset: none");
            }

            // Collect blocks on each face
            Map<String, List<String>> faceBlocks = new LinkedHashMap<>();
            faceBlocks.put("NORTH (z=0)", new ArrayList<>());
            faceBlocks.put("SOUTH (z=" + (length - 1) + ")", new ArrayList<>());
            faceBlocks.put("WEST (x=0)", new ArrayList<>());
            faceBlocks.put("EAST (x=" + (width - 1) + ")", new ArrayList<>());

            // Also track beacon positions
            List<Vec> beacons = new ArrayList<>();

            schematic.forEachBlock((pos, block) -> {
                int x = pos.blockX();
                int y = pos.blockY();
                int z = pos.blockZ();

                if (block.compare(Block.BEACON)) {
                    beacons.add(new Vec(x, y, z));
                }

                if (z == 0) faceBlocks.get("NORTH (z=0)").add(x + "," + y + " = " + block.name());
                if (z == length - 1) faceBlocks.get("SOUTH (z=" + (length - 1) + ")").add(x + "," + y + " = " + block.name());
                if (x == 0) faceBlocks.get("WEST (x=0)").add(y + "," + z + " = " + block.name());
                if (x == width - 1) faceBlocks.get("EAST (x=" + (width - 1) + ")").add(y + "," + z + " = " + block.name());
            });

            System.out.println("  Beacons (marker blocks): " + beacons);
            System.out.println("  Connectors (via MineshaftSegment):");

            var seg = new MineshaftSegment(name, schematic, Block.BEACON);
            for (var c : seg.getConnectors()) {
                System.out.println("    " + c.direction() + " at " + c.position());
            }

            for (var entry : faceBlocks.entrySet()) {
                System.out.println("  " + entry.getKey() + ":");
                for (String b : entry.getValue()) {
                    System.out.println("    " + b);
                }
            }
            System.out.println();
        }
    }
}
