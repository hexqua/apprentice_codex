package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class BlockLootTableGenerator extends BlockLootSubProvider {
    private final HolderLookup.Provider registries;

    public BlockLootTableGenerator(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        this.registries = registries;
    }

    @Override
    protected void generate() {
        dropSelf(BlockRegistry.APPRENTICE_DESK.get());
        dropSelf(BlockRegistry.SPELLCASTER_WORKBENCH.get());
        dropSelf(BlockRegistry.ARCANUM_IN_A_JAR.get());
        dropSelf(BlockRegistry.ATELIER_STATION.get());
        dropSelf(BlockRegistry.ESSENCE_SMOKER.get());
        add(BlockRegistry.COMFORT_BERRY_BUSH.get(), this::createComfortBerryBushDrops);
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return List.of(
                BlockRegistry.APPRENTICE_DESK.get(),
                BlockRegistry.SPELLCASTER_WORKBENCH.get(),
                BlockRegistry.ARCANUM_IN_A_JAR.get(),
                BlockRegistry.ATELIER_STATION.get(),
                BlockRegistry.ESSENCE_SMOKER.get(),
                BlockRegistry.COMFORT_BERRY_BUSH.get()
        );
    }

    private LootTable.Builder createComfortBerryBushDrops(Block block) {
        var fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        var immatureCondition = AnyOfCondition.anyOf(
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComfortBerryBushBlock.AGE, 0)),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComfortBerryBushBlock.AGE, 1)),
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComfortBerryBushBlock.AGE, 2))
        );
        var preHarvestCondition = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComfortBerryBushBlock.AGE, 3));
        var matureCondition = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ComfortBerryBushBlock.AGE, ComfortBerryBushBlock.MAX_AGE));

        // stage3 は見た目上の収穫直前段階だが未成熟扱いを維持し、破壊時だけ 2 個固定にする。
        return applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .when(immatureCondition)
                        .add(LootItem.lootTableItem(ItemRegistry.COMFORT_BERRIES.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .when(preHarvestCondition)
                        .add(LootItem.lootTableItem(ItemRegistry.COMFORT_BERRIES.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0f)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .when(matureCondition)
                        .add(LootItem.lootTableItem(ItemRegistry.COMFORT_BERRIES.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 3.0f)))
                                .apply(ApplyBonusCount.addUniformBonusCount(fortune)))));
    }
}
