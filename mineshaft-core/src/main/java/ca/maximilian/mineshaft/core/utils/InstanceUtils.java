package ca.maximilian.mineshaft.core.utils;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;

public class InstanceUtils {
    public static boolean isValidSpawnLight(Instance instance, Point point) {
        var chunk = instance.getChunkAt(point);
        if (chunk == null) return false;

        int blockX = point.blockX();
        int blockY = point.blockY();
        int blockZ = point.blockZ();

        int blockLight = instance.getBlockLight(blockX, blockY, blockZ);

        return blockLight <= 7;
    }
}
