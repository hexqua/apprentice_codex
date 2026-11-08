package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

import static jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice.ManaPotionTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserAutomationGameTests.createCreateManaPotionFluid;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.*;

final class CreateDispenserAutomationScenarios {
    private CreateDispenserAutomationScenarios() {}

    static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).resolve().orElse(null);
            var manaFluid = createCreateManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            blockEntity.setCurrentMana(850);
            helper.assertTrue(!manaFluid.isEmpty(), "Create potion fluid was not registered");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser rejected a Create regular mana potion fluid");
            helper.assertTrue(blockEntity.getCurrentMana() == 925,
                    "Spell Dispenser did not recover mana from Create potion fluid: " + blockEntity.getCurrentMana());
        });
    }

    static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var externalInventory = new ItemStackHandler(2);
            externalInventory.setStackInSlot(0,
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()));

            var harness = createSpellDispenserMovementHarness(
                    level,
                    castPos,
                    mountedInventory,
                    createSpellDispenserOwnerProfile("spell_dispenser_create_fuel_refill_test"),
                    800,
                    externalInventory,
                    true
            );

            startCreateSpellDispenserMovement(harness);
            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertTrue(externalInventory.getStackInSlot(0).is(Items.GLASS_BOTTLE),
                    "Create-mounted Spell Dispenser did not return a glass bottle to accessible contraption storage");
        });
    }

    static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var externalInventory = new ItemStackHandler(2);
            var manaPotion = createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get());
            externalInventory.setStackInSlot(0, manaPotion.copy());

            var harness = createSpellDispenserMovementHarness(
                    level,
                    castPos,
                    mountedInventory,
                    createSpellDispenserOwnerProfile("spell_dispenser_create_protected_storage_test"),
                    800,
                    externalInventory,
                    false
            );

            startCreateSpellDispenserMovement(harness);
            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertTrue(ItemStack.isSameItemSameTags(externalInventory.getStackInSlot(0), manaPotion),
                    "Create-mounted Spell Dispenser consumed a potion from protected contraption storage");
        });
    }
}
