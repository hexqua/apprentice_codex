package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
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
        tag(BlockTags.MINEABLE_WITH_AXE).add(BlockRegistry.SPELL_CALIBRATION_BENCH.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(
                BlockRegistry.SPELL_DISPENSER.get(),
                BlockRegistry.CREATIVE_SPELL_DISPENSER.get()
        );

        // 恵みの雨で効果のあるブロック.
        tag(TagRegistry.Blocks.CAN_RECEIVE_GRACED_RAIN).add(
                Blocks.NETHER_WART,
                Blocks.SUGAR_CANE,
                Blocks.CACTUS
        );

        // RiftHole でトンネル化させたくないブロックをデータパックから追加する。
        tag(TagRegistry.Blocks.RIFT_HOLE_TUNNEL_DENYLIST);

        // HarvestMoon で収穫させたくないブロックをデータパックから追加する。
        tag(TagRegistry.Blocks.HARVEST_MOON_DENYLIST);

        // 宝占いで探知対象にする候補。単一タグ化して全て「特殊な気配」として扱う。
        tag(TagRegistry.Blocks.TREASURE_DIVINATION_TARGETS)
                .add(
                        Blocks.ANCIENT_DEBRIS,
                        Blocks.SPAWNER,
                        Blocks.TRIAL_SPAWNER
                )
                .addTag(Tags.Blocks.CHESTS)
                .addTag(Tags.Blocks.BARRELS)
                .addTag(BlockTags.SHULKER_BOXES)
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "ores/mithril"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mithril_ore"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "deepslate_mithril_ore"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("lootr", "pots"));

        // TinyLumberjack の強制原木判定.
        tag(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LOGS);

        // TinyLumberjack の強制葉っぱ判定.
        tag(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LEAVES).add(
                Blocks.NETHER_WART_BLOCK,
                Blocks.WARPED_WART_BLOCK,
                Blocks.SHROOMLIGHT
        );

        // MistForm 中だけ霧が抜けても不自然でない薄い/隙間のあるブロックを通過対象にする。
        tag(TagRegistry.Blocks.MIST_FORM_PASSABLE)
                .add(Blocks.IRON_BARS)
                .addTag(BlockTags.FENCES)
                .addTag(BlockTags.FENCE_GATES)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.TRAPDOORS)
                .addTag(BlockTags.LEAVES);

        tag(TagRegistry.Blocks.MIST_FORM_IGNORES_MOVEMENT_RESTRICTION).add(
                Blocks.COBWEB,
                Blocks.POWDER_SNOW,
                Blocks.SWEET_BERRY_BUSH
        );
    }
}
