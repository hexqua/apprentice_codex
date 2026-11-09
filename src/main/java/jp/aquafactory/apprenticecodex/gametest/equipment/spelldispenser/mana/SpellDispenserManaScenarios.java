package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.mana;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaFluidHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import static jp.aquafactory.apprenticecodex.gametest.crafting.ComparatorScenarios.assertSpellDispenserComparatorOutput;
import static jp.aquafactory.apprenticecodex.gametest.equipment.flask.FlaskScenarios.createFilledSpellcastersFlask;
import static jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice.ManaPotionTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.ManaFluidTestSupport.createIronsManaPotionFluid;

final class SpellDispenserManaScenarios {
    private SpellDispenserManaScenarios() {}

    static void spellDispenserFluidAcceptsOnlyRegularManaPotions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().orElse(null);
            var partialManaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 100);
            var remainingPartialManaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 150);
            var strongerManaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_TWO.get(), 250);
            var splashPotion = PotionUtils.setPotion(
                    new ItemStack(Items.SPLASH_POTION),
                    PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var splashManaFluid = PotionFluid.from(splashPotion);
            var healingFluid = createIronsManaPotionFluid(Potions.HEALING, 250);

            blockEntity.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(healingFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a non-mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(splashManaFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a splash mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(partialManaFluid, IFluidHandler.FluidAction.EXECUTE) == 100,
                    "Spell Dispenser rejected a partial regular mana potion fluid");
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().getAmount() == 100,
                    "Spell Dispenser did not retain a partial mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(strongerManaFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a different mana potion fluid into the same tank");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(remainingPartialManaFluid, IFluidHandler.FluidAction.EXECUTE) == 150,
                    "Spell Dispenser rejected matching mana potion fluid after a partial fill");
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().getAmount() == 250,
                    "Spell Dispenser consumed a potion fluid that should not fit in remaining mana capacity");
        });
    }

    static void spellDispenserFluidConsumesPotionDoseImmediately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            blockEntity.setCurrentMana(850);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser did not accept a regular mana potion fluid");
            helper.assertTrue(blockEntity.getCurrentMana() == 925,
                    "Spell Dispenser did not immediately recover mana from potion fluid: " + blockEntity.getCurrentMana());
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().isEmpty(),
                    "Spell Dispenser left consumed mana potion fluid in the tank");
        });
    }

    static void spellDispenserFluidDrainsUnconsumedPotionOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 500);

            blockEntity.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 500,
                    "Spell Dispenser did not accept unconsumed mana potion fluid");

            var drained = fluidHandler == null ? FluidStack.EMPTY : fluidHandler.drain(500, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drained.getAmount() == 500,
                    "Spell Dispenser did not drain all unconsumed potion fluid: " + drained.getAmount());
            helper.assertTrue(blockEntity.getCurrentMana() == 950,
                    "Spell Dispenser converted internal mana into drained potion fluid");
            helper.assertTrue(fluidHandler == null || fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "Spell Dispenser drained potion fluid after the tank was empty");
        });
    }

    static void spellDispenserFluidPersistsThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = original.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            original.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser did not accept mana potion fluid before NBT round-trip");

            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.load(original.getUpdateTag());
            helper.assertTrue(restored.getStoredManaPotionFluid().getAmount() == 250,
                    "Spell Dispenser did not restore stored mana potion fluid from NBT");
            helper.assertTrue(SpellDispenserManaFluidHelper.isSameFluidAndTags(restored.getStoredManaPotionFluid(), manaFluid),
                    "Spell Dispenser changed stored potion fluid identity during NBT round-trip");
        });
    }

    static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var overflowPotion = createInstantManaPotion(PotionRegistry.INSTANT_MANA_THREE.get());
            var fittingPotion = createInstantManaPotion(PotionRegistry.INSTANT_MANA_TWO.get());
            spellDispenser.setCurrentMana(780);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, overflowPotion.copy());
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, fittingPotion.copy());

            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertTrue(spellDispenser.getCurrentMana() == 930,
                    "Spell Dispenser did not pick the highest refill that still fits: " + spellDispenser.getCurrentMana());
            helper.assertTrue(
                    ItemStack.isSameItemSameTags(
                            spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START),
                            overflowPotion
                    ),
                    "Spell Dispenser consumed a potion that should have overflowed"
            );
            helper.assertTrue(
                    spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1).is(Items.GLASS_BOTTLE),
                    "Spell Dispenser did not leave a glass bottle after consuming a direct mana potion"
            );
        });
    }

    static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var glowEnergyFlask = createFilledSpellcastersFlask(
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    1
            );
            spellDispenser.setCurrentMana(700);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, glowEnergyFlask);

            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertTrue(spellDispenser.getCurrentMana() == 850,
                    "Spell Dispenser did not apply Glow Energy to flask refill mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(
                    spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START).is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spell Dispenser replaced the flask item instead of emptying one dose"
            );
            helper.assertTrue(
                    !SpellcastersFlask.isFilled(spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START)),
                    "Spell Dispenser did not consume exactly one flask dose"
            );
            helper.assertTrue(
                    SpellcastersFlask.getStoredItem(
                            spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START)
                    ).isEmpty(),
                    "Spell Dispenser left StoredItem behind after consuming the last mana flask dose"
            );
        });
    }

    static void spellDispenserComparatorOutputMatchesStoredMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellDispenserComparatorOutput(helper, new BlockPos(0, 1, 0), 0, false, 0);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(1, 1, 0), 1, false, 1);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(2, 1, 0), SpellDispenserManaHelper.MAX_MANA / 2, false, 8);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(3, 1, 0), SpellDispenserManaHelper.MAX_MANA, false, 15);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(4, 1, 0), 0, true, 0);
        });
    }
}
