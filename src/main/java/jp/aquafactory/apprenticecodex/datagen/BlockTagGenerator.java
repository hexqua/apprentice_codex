package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class BlockTagGenerator extends BlockTagsProvider {
    public BlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BlockRegistry.PERSONAL_SHELF_CHEST.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BlockRegistry.ARCANUM_IN_A_JAR.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BlockRegistry.ESSENCE_SMOKER.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(BlockRegistry.APPRENTICE_DESK.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(BlockRegistry.SPELLCASTER_WORKBENCH.get());

        // 恵みの雨で効果のあるブロック.
        tag(TagRegistry.Blocks.CAN_RECEIVE_GRACED_RAIN).add(
                Blocks.NETHER_WART,
                Blocks.SUGAR_CANE,
                Blocks.CACTUS
        );

        // TinyLumberjack の強制原木判定.
        tag(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LOGS);

        // TinyLumberjack の強制葉っぱ判定.
        tag(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LEAVES).add(
                Blocks.NETHER_WART_BLOCK,
                Blocks.WARPED_WART_BLOCK,
                Blocks.SHROOMLIGHT
        );
    }
}
