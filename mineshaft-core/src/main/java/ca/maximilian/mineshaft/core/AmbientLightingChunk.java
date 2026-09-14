package ca.maximilian.mineshaft.core;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.Section;
import net.minestom.server.network.packet.server.play.data.LightData;
import net.minestom.server.world.DimensionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static net.minestom.server.instance.light.LightCompute.EMPTY_CONTENT;

public class AmbientLightingChunk extends LightingChunk {

    public AmbientLightingChunk(Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ);
    }

    protected AmbientLightingChunk(Instance instance, int chunkX, int chunkZ, List<Section> sections) {
        super(instance, chunkX, chunkZ, sections);
    }

    @Override
    protected LightData createLightData(boolean requiredFullChunk) {
        DimensionType dimension = getInstance().getCachedDimensionType();
        float ambient = dimension.ambientLight();
        int minLevel = Math.min(15, Math.max(0, Math.round(ambient * 15f)));
        byte packedMin = (byte) ((minLevel << 4) | minLevel);

        BitSet skyMask = new BitSet();
        BitSet blockMask = new BitSet();
        BitSet emptySkyMask = new BitSet();
        BitSet emptyBlockMask = new BitSet();
        List<byte[]> skyLights = new ArrayList<>();
        List<byte[]> blockLights = new ArrayList<>();

        final boolean hasSky = dimension.hasSkylight();
        int index = 0;

        for (Section section : getSections()) {
            index++; // protocol is 1-based

            // ---- sky light ----
            // Do NOT call requiresUpdate() / relight – just read whatever is already there
            byte[] sky = section.skyLight().array();
            if (hasSky) {
                byte[] raised = applyMinimum(sky, minLevel, packedMin);
                skyLights.add(raised);
                skyMask.set(index);
            } else if (minLevel > 0) {
                // no sky in this dimension, but ambient still needs a value
                skyLights.add(createFilled(packedMin));
                skyMask.set(index);
            } else {
                emptySkyMask.set(index);
            }

            // ---- block light (torches etc. if already computed) ----
            byte[] block = section.blockLight().array();
            byte[] raisedBlock = applyMinimum(block, minLevel, packedMin);
            blockLights.add(raisedBlock);
            blockMask.set(index);
        }

        return new LightData(skyMask, blockMask, emptySkyMask, emptyBlockMask, skyLights, blockLights);
    }

    private static byte[] applyMinimum(byte[] src, int minLevel, byte packedMin) {
        if (minLevel <= 0) {
            if (src == null || src.length == 0 || src == EMPTY_CONTENT) {
                return new byte[0];
            }
            return src.clone();
        }
        if (src == null || src.length == 0 || src == EMPTY_CONTENT) {
            return createFilled(packedMin);
        }
        byte[] result = src.clone();
        for (int i = 0; i < result.length; i++) {
            int b = result[i] & 0xFF;
            int low  = b & 0x0F;
            int high = (b >> 4) & 0x0F;
            if (low  < minLevel) low  = minLevel;
            if (high < minLevel) high = minLevel;
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static byte[] createFilled(byte packed) {
        byte[] arr = new byte[2048];
        Arrays.fill(arr, packed);
        return arr;
    }

    @Override
    public Chunk copy(Instance instance, int chunkX, int chunkZ) {
        assertReadLock();
        List<Section> sections = this.sections.stream().map(Section::clone).toList();
        return new AmbientLightingChunk(instance, chunkX, chunkZ, sections);
    }
}