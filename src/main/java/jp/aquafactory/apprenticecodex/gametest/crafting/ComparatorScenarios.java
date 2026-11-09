package jp.aquafactory.apprenticecodex.gametest.crafting;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createFilledSpellcastersFlask;

public final class ComparatorScenarios {
    private ComparatorScenarios() {}

    static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(0, 1, 0), 0, 0, 0);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(1, 1, 0), 3, 0, 3);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(2, 1, 0), ArcanumInAJarBlockEntity.MAX_STORED_PARAMETER, 0, 8);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(3, 1, 0), 0, 5, 0);
        });
    }

    static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertAtelierStationComparatorOutput(helper, new BlockPos(0, 1, 0), 0, false, 0);
            assertAtelierStationComparatorOutput(helper, new BlockPos(1, 1, 0), AtelierStationBlockEntity.MILLIBUCKETS_PER_USE, false, 1);
            assertAtelierStationComparatorOutput(helper, new BlockPos(2, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT / 2, false, 8);
            assertAtelierStationComparatorOutput(helper, new BlockPos(3, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT, false, 15);
            assertAtelierStationComparatorOutput(helper, new BlockPos(4, 1, 0), 0, true, 0);
        });
    }

    public static void placeAndAssertBlockEntity(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            BlockEntityType<?> expectedType
    ) {
        helper.setBlock(pos, block);
        helper.assertBlockPresent(block, pos);

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity != null, "Missing block entity for " + BuiltInRegistries.BLOCK.getKey(block));
        helper.assertTrue(blockEntity.getType() == expectedType,
                "Block entity type mismatch for " + BuiltInRegistries.BLOCK.getKey(block) + ": " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    static void assertArcanumInAJarComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedParameterCount,
            int remainingOperationCount,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ARCANUM_IN_A_JAR.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof ArcanumInAJarBlockEntity,
                "Arcanum in a Jar block entity was not created");

        var tag = new CompoundTag();
        tag.putInt("StoredParameterCount", storedParameterCount);
        tag.putInt("RemainingOperationCount", remainingOperationCount);
        blockEntity.load(tag);

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Arcanum in a Jar should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Arcanum in a Jar comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    static void assertAtelierStationComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedFluidAmount,
            boolean insertInventoryFlask,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ATELIER_STATION.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof AtelierStationBlockEntity,
                "Atelier Station block entity was not created");

        if (blockEntity instanceof AtelierStationBlockEntity atelierStation) {
            if (storedFluidAmount > 0) {
                var tag = new CompoundTag();
                var storedFluidList = new ListTag();
                var storedFluidTag = new CompoundTag();
                var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION),
                        Potions.REGENERATION);
                storedFluidTag.put("Item", storedPotion.save(new CompoundTag()));
                storedFluidTag.putInt("Amount", storedFluidAmount);
                storedFluidList.add(storedFluidTag);
                tag.put("StoredFluids", storedFluidList);
                blockEntity.load(tag);
            }

            if (insertInventoryFlask) {
                var flask = createFilledSpellcastersFlask(
                        PotionUtils.setPotion(new ItemStack(Items.POTION),
                                Potions.REGENERATION),
                        1,
                        0
                );
                atelierStation.getFlaskInventory().setStackInSlot(0, flask);
            }
        }

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Atelier Station should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Atelier Station comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    public static void assertSpellDispenserComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int currentMana,
            boolean insertInventoryScroll,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity,
                "Spell Dispenser block entity was not created");

        if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
            spellDispenser.setCurrentMana(currentMana);
            if (insertInventoryScroll) {
                spellDispenser.getInventory().setStackInSlot(
                        SpellDispenserBlockEntity.SPELL_SLOT_INDEX,
                        createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
                );
            }
        }

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Spell Dispenser should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Spell Dispenser comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }
}
