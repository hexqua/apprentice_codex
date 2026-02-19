package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class BlockLootTableGenerator extends BlockLootSubProvider {
    public BlockLootTableGenerator() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(BlockRegistry.APPRENTICE_DESK.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return List.of(BlockRegistry.APPRENTICE_DESK.get());
    }
}
