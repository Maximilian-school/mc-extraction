package ca.maximilian.extraction_game.core.handler.block;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

public class ButtonHandler implements BlockHandler {

    @Getter
    private static Point sellButtonPos;

    @Override
    public void onPlace(Placement placement) {
        Instance instance = placement.getInstance();
        Point blockBelow = placement.getBlockPosition().add(0, -1, 0);

        if (instance.getBlock(blockBelow).id() == Block.BEDROCK.id()) {
            instance.setBlock(blockBelow, Block.STONE);
            sellButtonPos = placement.getBlockPosition();
        }
    }

    @Override
    public Key getKey() {
        return Key.key("extraction:button_handler");
    }
}
