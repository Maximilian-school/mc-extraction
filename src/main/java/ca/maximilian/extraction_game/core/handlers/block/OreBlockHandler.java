package ca.maximilian.extraction_game.core.handlers.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class OreBlockHandler implements BlockHandler {

    private final ItemStack itemStack;

    public OreBlockHandler(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void dropItems(BlockVec blockVec,  Instance instance) {
        ItemEntity itemEntity = new ItemEntity(itemStack);

        Vec spawnPosition = blockVec.add(0.5, 0.5, 0.5);

        itemEntity.setInstance(instance, spawnPosition);
    }

    @Override
    public void onDestroy(Destroy destroy) {
        this.dropItems(destroy.getBlockPosition().asBlockVec(), destroy.getInstance());
        destroy.getInstance().setBlock(destroy.getBlockPosition(), Block.STONE);
    }

    @Override
    public @NonNull Key getKey() {
        return Key.key("extraction_game:ore");
    }
}
