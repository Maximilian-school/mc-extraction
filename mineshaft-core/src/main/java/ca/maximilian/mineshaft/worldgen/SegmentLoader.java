package ca.maximilian.mineshaft.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.minestom.server.instance.block.Block;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SegmentLoader {

    private static final Block DEFAULT_MARKER_BLOCK = Block.BEACON;
    private static final int DEFAULT_MAX_CONSECUTIVE_STRAIGHT = -1;

    public static List<MineshaftSegment> loadSegments(String configPath) {
        List<MineshaftSegment> segments = new ArrayList<>();
        String normalizedConfigPath = configPath.startsWith("/") ? configPath : "/" + configPath;
        try (var stream = SegmentLoader.class.getResourceAsStream(normalizedConfigPath)) {
            if (stream == null) {
                return segments;
            }
            try (var reader = new InputStreamReader(stream)) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);

                int lastSlash = normalizedConfigPath.lastIndexOf('/');
                String basePath = lastSlash != -1 ? normalizedConfigPath.substring(0, lastSlash + 1) : "/";

                var jsonSegments = json.getAsJsonArray("segments");
                for (var elem : jsonSegments) {
                    JsonObject obj = elem.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    boolean isStarting = (obj.has("starting") && obj.get("starting").getAsBoolean())
                            || (obj.has("isStarting") && obj.get("isStarting").getAsBoolean())
                            || id.equalsIgnoreCase("start") || id.equalsIgnoreCase("starting")
                            || id.equalsIgnoreCase("starting_segment") || id.equalsIgnoreCase("starting_room");

                    double weight = -1;
                    if (obj.has("weight")) {
                        weight = obj.get("weight").getAsDouble();
                    }

                    Block markerBlock = DEFAULT_MARKER_BLOCK;
                    if (obj.has("markerBlock")) {
                        String markerBlockStr = obj.get("markerBlock").getAsString();
                        Block parsed = Block.fromKey(markerBlockStr);
                        if (parsed != null) markerBlock = parsed;
                    }

                    int maxConsecutiveStraight = DEFAULT_MAX_CONSECUTIVE_STRAIGHT;
                    if (obj.has("maxConsecutiveStraight")) {
                        maxConsecutiveStraight = obj.get("maxConsecutiveStraight").getAsInt();
                    }

                    int multiTileWidth = 1;
                    int multiTileHeight = 1;
                    int multiTileLayers = 1;
                    if (obj.has("multi_tile")) {
                        JsonObject multiTile = obj.getAsJsonObject("multi_tile");
                        if (multiTile.has("width")) {
                            multiTileWidth = multiTile.get("width").getAsInt();
                        }
                        if (multiTile.has("height")) {
                            multiTileHeight = multiTile.get("height").getAsInt();
                        }
                        if (multiTile.has("layers")) {
                            multiTileLayers = multiTile.get("layers").getAsInt();
                        }
                    }
                    int minSpacing = obj.has("minSpacing") ? obj.get("minSpacing").getAsInt() : 0;

                    // Optional per-segment spawn count limits. -1 means "no limit".
                    int minAppearances = obj.has("minAppearances") ? obj.get("minAppearances").getAsInt() : -1;
                    int maxAppearances = obj.has("maxAppearances") ? obj.get("maxAppearances").getAsInt() : -1;

                    // Optional list of other segment/cluster ids this one may never have a doorway directly into.
                    Set<String> connectBlacklist = new HashSet<>();
                    if (obj.has("blacklist")) {
                        for (var bElem : obj.getAsJsonArray("blacklist")) {
                            connectBlacklist.add(bElem.getAsString());
                        }
                    }

                    // Optional: ids this segment must connect to, directly or indirectly through
                    // the corridor network, for a generated map to be accepted. E.g. a stairwell
                    // requiring at least a tjunction or crossroads somewhere reachable from it:
                    //   "requiresConnection": { "anyOf": ["tjunction", "crossroads"], "maxDistance": -1 }
                    // maxDistance is optional and defaults to -1 (unlimited / indirect is fine);
                    // set it to 1 to require a direct neighbor instead.
                    Set<String> requiredConnectionIds = new HashSet<>();
                    int requiredConnectionMaxDistance = -1;
                    if (obj.has("requiresConnection")) {
                        JsonObject rc = obj.getAsJsonObject("requiresConnection");
                        if (rc.has("anyOf")) {
                            for (var rElem : rc.getAsJsonArray("anyOf")) {
                                requiredConnectionIds.add(rElem.getAsString());
                            }
                        }
                        if (rc.has("maxDistance")) {
                            requiredConnectionMaxDistance = rc.get("maxDistance").getAsInt();
                        }
                    }

                    JsonElement fileElem = obj.get("file");
                    if (fileElem == null) continue;

                    if (fileElem.isJsonPrimitive()) {
                        String file = fileElem.getAsString();
                        loadAndAddSegment(segments, id, file, markerBlock, isStarting, weight, maxConsecutiveStraight,
                                minSpacing, multiTileWidth, multiTileHeight, multiTileLayers, basePath,
                                minAppearances, maxAppearances, connectBlacklist,
                                requiredConnectionIds, requiredConnectionMaxDistance);
                    } else if (fileElem.isJsonArray()) {
                        JsonArray files = fileElem.getAsJsonArray();
                        for (var fElem : files) {
                            JsonObject fObj = fElem.getAsJsonObject();
                            String file = fObj.get("file").getAsString();
                            double fWeight = fObj.has("weight") ? fObj.get("weight").getAsDouble() : weight;
                            loadAndAddSegment(segments, id, file, markerBlock, isStarting, fWeight, maxConsecutiveStraight,
                                    minSpacing, multiTileWidth, multiTileHeight, multiTileLayers, basePath,
                                    minAppearances, maxAppearances, connectBlacklist,
                                    requiredConnectionIds, requiredConnectionMaxDistance);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mineshaft segments", e);
        }
        return segments;
    }

    public static MineshaftSegment getStartingSegment(List<MineshaftSegment> segments) {
        return segments.stream()
                .filter(MineshaftSegment::isStarting)
                .findFirst()
                .orElseGet(MineshaftSegment::createDefaultStartingSegment);
    }

    public static Block getMarkerBlock(String configPath) {
        String normalizedConfigPath = configPath.startsWith("/") ? configPath : "/" + configPath;
        try (var stream = SegmentLoader.class.getResourceAsStream(normalizedConfigPath)) {
            if (stream == null) return Block.BEACON;
            try (var reader = new InputStreamReader(stream)) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                return getMarkerBlockFromJson(json);
            }
        } catch (Exception e) {
            System.err.println("Failed to read marker block, defaulting to beacon.");
            return Block.BEACON;
        }
    }

    private static Block getMarkerBlockFromJson(JsonObject json) {
        String markerBlockStr = json.has("markerBlock") ? json.get("markerBlock").getAsString() : "minecraft:beacon";
        Block markerBlock = Block.fromKey(markerBlockStr);
        return markerBlock != null ? markerBlock : Block.BEACON;
    }

    private static void loadAndAddSegment(List<MineshaftSegment> segments, String id, String file, Block markerBlock,
                                          boolean isStarting, double weight, int maxConsecutiveStraight, int minSpacing,
                                          int multiTileWidth, int multiTileHeight, int multiTileLayers, String basePath,
                                          int minAppearances, int maxAppearances, Set<String> connectBlacklist,
                                          Set<String> requiredConnectionIds, int requiredConnectionMaxDistance) {
        var schemStream = SegmentLoader.class.getResourceAsStream(basePath + file);
        if (schemStream == null) {
            schemStream = SegmentLoader.class.getResourceAsStream("/mineshaft/" + file);
        }
        if (schemStream == null) {
            schemStream = SegmentLoader.class.getResourceAsStream(file);
        }
        if (schemStream == null) return;
        try {
            byte[] bytes = schemStream.readAllBytes();
            Schematic schematic = SchematicReader.detecting().read(bytes);
            MineshaftSegment segment = new MineshaftSegment(id, schematic, markerBlock, isStarting, weight,
                    maxConsecutiveStraight, minSpacing, multiTileWidth, multiTileHeight, multiTileLayers,
                    minAppearances, maxAppearances);
            segment.setConnectBlacklist(connectBlacklist);
            segment.setRequiredConnections(requiredConnectionIds, requiredConnectionMaxDistance);
            segments.add(segment);
        } catch (Exception e) {
            System.err.println("Failed to load schematic " + file + ": " + e.getMessage());
        }
    }

}