package ca.maximilian.mineshaft.core.utils;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Arrays;
import java.util.List;

public class NbtUtil {

    public static List<BinaryTag> createSignLines(Component... lines) {
        return Arrays.stream(lines)
                .map(comp -> (BinaryTag) StringBinaryTag.stringBinaryTag(GsonComponentSerializer.gson().serialize(comp)))
                .toList();
    }
}
