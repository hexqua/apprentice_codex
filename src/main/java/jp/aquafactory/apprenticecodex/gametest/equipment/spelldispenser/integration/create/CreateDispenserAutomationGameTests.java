package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserAutomationGameTests {
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("create", "contraption_controlled"));

    private CreateDispenserAutomationGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
                var fluidHandler = blockEntity.getFluidHandler(Direction.WEST);
                var manaFluid = createCreateManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 250);

                blockEntity.setCurrentMana(850);
                helper.assertTrue(!manaFluid.isEmpty(), "Create potion fluid was not registered");
                helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                        "Spell Dispenser rejected a Create regular mana potion fluid");
                helper.assertTrue(blockEntity.getCurrentMana() == 925,
                        "Spell Dispenser did not recover mana from Create potion fluid: " + blockEntity.getCurrentMana());
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
                var externalInventory = new ItemStackHandler(2);
                externalInventory.setStackInSlot(0,
                        createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()));

                var harness = trackHarness(scope, createSpellDispenserMovementHarness(
                        level,
                        castPos,
                        mountedInventory,
                        createSpellDispenserOwnerProfile("spell_dispenser_create_fuel_refill_test"),
                        800,
                        externalInventory,
                        true
                ));

                startCreateSpellDispenserMovement(scope, harness);
                for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }

                helper.assertTrue(ItemStack.matches(externalInventory.getStackInSlot(0), new ItemStack(Items.GLASS_BOTTLE))
                                && externalInventory.getStackInSlot(1).isEmpty() && storedMana(harness) == 875,
                        "Create-mounted Spell Dispenser did not return a glass bottle to accessible contraption storage");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 1, 2));
                var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
                var externalInventory = new ItemStackHandler(2);
                var manaPotion = createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get());
                externalInventory.setStackInSlot(0, manaPotion.copy());

                var harness = trackHarness(scope, createSpellDispenserMovementHarness(
                        level,
                        castPos,
                        mountedInventory,
                        createSpellDispenserOwnerProfile("spell_dispenser_create_protected_storage_test"),
                        800,
                        externalInventory,
                        false
                ));

                startCreateSpellDispenserMovement(scope, harness);
                for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                    tickCreateSpellDispenserMovement(scope, harness);
                }

                helper.assertTrue(ItemStack.matches(externalInventory.getStackInSlot(0), manaPotion)
                                && externalInventory.getStackInSlot(1).isEmpty() && storedMana(harness) == 800,
                        "Create-mounted Spell Dispenser consumed a potion from protected contraption storage");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void dispenserVariantsRemainSelectableByCreateContraptionControls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // Movement harnessはタグによる選択を通らないため、Controlsとの接続条件を両variantで残す。
            for (var item : List.of(ItemRegistry.SPELL_DISPENSER.get(), ItemRegistry.CREATIVE_SPELL_DISPENSER.get())) {
                helper.assertTrue(new ItemStack(item).is(CREATE_CONTRAPTION_CONTROLLED),
                        "Contraption Controls should be able to select dispenser variant: " + BuiltInRegistries.ITEM.getKey(item));
            }
        });
    }


    private static FluidStack createCreateManaPotionFluid(Potion potion, int amountMb) {
        var createPotion = BuiltInRegistries.FLUID.getOptional(
                ResourceLocation.fromNamespaceAndPath("create", "potion")).orElseThrow();

        var fluid = new FluidStack(createPotion, amountMb);
        fluid.set(DataComponents.POTION_CONTENTS, PotionContentsHelper.createPotionStack(Items.POTION, potion).get(DataComponents.POTION_CONTENTS));
        return fluid;
    }

    private static int storedMana(Object harness) {
        var data = (CompoundTag) invokeCreateGameTestHook("copySpellDispenserData", new Class<?>[]{Object.class}, harness);
        return data.getInt("CurrentMana");
    }
}
