package jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.backCurioBroomStack;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.installBroomScrolls;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.installBroomUpgrades;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEST_POS;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.equipmentPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnBroom;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.trackEntity;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomCalibrationGameTests {
    private BroomCalibrationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void calibratedBroomsUseBackSlotWithoutQuickEquip(GameTestHelper helper) {
        var player = equipmentPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "broom_curio_contract_test"
        );
        var backContext = new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true);
        var beltContext = new SlotContext(CuriosSlotConstants.BELT, player, 0, false, true);

        for (var broom : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broom);
            helper.assertFalse(stack.is(BowGameTestSupport.CURIOS_BACK),
                    "Broom should not use the unconditional Curios back tag");
            helper.assertFalse(CuriosApi.getItemStackSlots(stack, player).containsKey(CuriosSlotConstants.BACK),
                    "Uncalibrated broom should not expose the Curios back slot");
            helper.assertFalse(broom.canEquip(backContext, stack),
                    "Uncalibrated broom should not be equippable in the Curios back slot");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, new ItemStack(Items.SADDLE)),
                    "Broom should accept a saddle as its Curios calibration");
            helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(stack),
                    "Saddle-calibrated broom should enable Curios support");
            helper.assertTrue(CuriosApi.getItemStackSlots(stack, player).containsKey(CuriosSlotConstants.BACK),
                    "Saddle-calibrated broom should expose the Curios back slot");
            helper.assertTrue(broom.canEquip(backContext, stack),
                    "Saddle-calibrated broom should be equippable in the Curios back slot");
            helper.assertFalse(broom.canEquip(beltContext, stack),
                    "Broom should reject non-back Curios slots");
            helper.assertFalse(broom.canEquipFromUse(backContext, stack),
                    "Broom should preserve its normal right-click placement instead of quick-equipping");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, new ItemStack(Items.SADDLE)),
                    "Broom should reject a duplicate Curios calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, ItemStack.EMPTY),
                    "Broom should allow removing its Curios calibration");
            helper.assertFalse(AbstractBroomItem.isBackCurioEnabled(stack),
                    "Removing the saddle should disable Curios support");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAcceptOneFirewardRingCalibration(GameTestHelper helper) {
        for (var broomItem : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broomItem);
            var firewardRing = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get()
            );
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, firewardRing),
                    "Broom should accept a Fireward Ring calibration");
            helper.assertTrue(AbstractBroomItem.isFirewardEnabled(stack),
                    "Fireward-calibrated broom stack should enable fire immunity");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, firewardRing),
                    "Broom should reject a duplicate Fireward Ring calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, ItemStack.EMPTY),
                    "Broom should allow removing its Fireward Ring calibration");
            helper.assertFalse(AbstractBroomItem.isFirewardEnabled(stack),
                    "Removing the Fireward Ring should disable fire immunity");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAcceptOneOverdriveEngineCalibration(GameTestHelper helper) {
        for (var broomItem : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broomItem);
            var engine = new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, engine),
                    "Broom should accept an Overdrive Broom Engine calibration");
            helper.assertTrue(AbstractBroomItem.isOverdriveEnabled(stack),
                    "Engine-calibrated broom stack should enable Overdrive");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, engine),
                    "Broom should reject a duplicate Overdrive Broom Engine calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, ItemStack.EMPTY),
                    "Broom should allow removing its Overdrive Broom Engine calibration");
            helper.assertFalse(AbstractBroomItem.isOverdriveEnabled(stack),
                    "Removing the Overdrive Broom Engine should disable Overdrive");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyFloatmountAcceptsOneAquaticCalibration(GameTestHelper helper) {
        var floatmountItem = (FloatmountBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get();
        var floatmount = new ItemStack(floatmountItem);
        var heartOfTheSea = new ItemStack(Items.HEART_OF_THE_SEA);
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 0, heartOfTheSea),
                "Floatmount Broom should accept Heart of the Sea as a calibration");
        helper.assertTrue(FloatmountBroomItem.isAquaticCalibrationEnabled(floatmount),
                "Heart of the Sea should enable Floatmount aquatic calibration");
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 1, heartOfTheSea),
                "Floatmount Broom should reject duplicate aquatic calibrations");

        var hoverride = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        hoverride, 0, heartOfTheSea),
                "Hoverride Broom should reject Heart of the Sea calibration");
        CalibrationAdjustmentStorage.set(
                hoverride, 0, AbstractBroomItem.CALIBRATION_ADJUSTMENT_SLOT_COUNT, heartOfTheSea
        );
        helper.assertFalse(FloatmountBroomItem.isAquaticCalibrationEnabled(hoverride),
                "Stored Heart of the Sea data must not enable aquatic calibration for Hoverride Broom");

        var broom = spawnBroom(helper, 1.5D);
        broom.setBroomItemStack(floatmount);
        helper.assertTrue(broom.isAquaticCalibrationEnabled(),
                "Deployed Floatmount Broom should derive aquatic calibration from its stored stack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAllowOnlyOneEquippedAcrossExpandedSlots(GameTestHelper helper) {
        var player = equipmentPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "broom_expanded_back_limit_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for broom test"));
        curiosInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/broom_expanded_back"),
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );

        var floatmount = (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get();
        var hoverride = (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get();
        var equippedStack = backCurioBroomStack(floatmount);
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, equippedStack);
        var currentContext = new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true);
        var secondContext = new SlotContext(CuriosSlotConstants.BACK, player, 1, false, true);

        helper.assertTrue(floatmount.canEquip(currentContext, equippedStack),
                "Equipped broom should remain valid in its current slot");
        helper.assertFalse(floatmount.canEquip(secondContext, backCurioBroomStack(floatmount)),
                "Broom should reject a second copy in an expanded back slot");
        helper.assertFalse(hoverride.canEquip(secondContext, backCurioBroomStack(hoverride)),
                "Floatmount and Hoverride Brooms should be mutually exclusive");
        curiosInventory.setSlotActive(CuriosSlotConstants.BACK, 0, false);
        helper.assertFalse(hoverride.canEquip(secondContext, backCurioBroomStack(hoverride)),
                "Broom exclusion should include inactive Curios slots");

        var quiverPlayer = equipmentPlayer(
                helper,
                new BlockPos(1, 2, 0),
                "broom_quiver_coexistence_test"
        );
        var quiverInventory = CuriosApi.getCuriosInventory(quiverPlayer)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for quiver test"));
        quiverInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/broom_quiver_back"),
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );
        quiverInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get())
        );
        helper.assertTrue(floatmount.canEquip(
                        new SlotContext(CuriosSlotConstants.BACK, quiverPlayer, 1, false, true),
                        backCurioBroomStack(floatmount)
                ),
                "Spellcaster Quiver should not block a broom when another back slot exists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomCalibrationSlotsOnlySelectWhileControllingAndDisabledScrollCanBeRemoved(
            GameTestHelper helper
    ) {
        assertBroomCalibrationSelection(
                helper,
                ItemRegistry.FLOATMOUNT_BROOM.get(),
                EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel()),
                0,
                "Floatmount"
        );
        assertBroomCalibrationSelection(
                helper,
                ItemRegistry.HOVERRIDE_BROOM.get(),
                EntityRegistry.HOVERRIDE_BROOM.get().create(helper.getLevel()),
                3,
                "Hoverride"
        );
        helper.succeed();
    }

    private static void assertBroomCalibrationSelection(
            GameTestHelper helper,
            Item broomItem,
            AbstractBroomEntity broom,
            int xOffset,
            String broomName
    ) {
        helper.assertTrue(broom != null, broomName + " Broom entity should be creatable");
        if (broom == null) {
            return;
        }
        var stack = new ItemStack(broomItem);
        helper.assertTrue(((AbstractBroomItem) broomItem).getCalibrationAdjustmentSlotCount(stack) == 3,
                broomName + " Broom should expose three adjustment slots");
        installBroomScrolls(stack);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == 0,
                broomName + " Broom should start with zero enabled scroll slots");
        helper.assertFalse(AbstractBroomItem.getCalibrationScroll(stack, 2).isEmpty(),
                broomName + " Broom should retain scrolls stored in disabled slots");

        installBroomUpgrades(stack, 3);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == 3,
                broomName + " Broom should enable one scroll slot per upgrade");
        broom.setBroomItemStack(stack);
        var pos = helper.absolutePos(TEST_POS.offset(xOffset, 0, 0));
        broom.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
        trackEntity(helper, broom);
        helper.assertTrue(helper.getLevel().addFreshEntity(broom), "Broom fixture entity must be registered");
        var rider = serverRider(helper, broomName.toLowerCase() + "_calibration_rider");
        helper.assertTrue(rider.startRiding(broom, true), broomName + " rider should mount");

        var selections = new SpellSelectionManager(rider)
                .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT);
        helper.assertTrue(selections.size() == 3,
                broomName + " rider should receive all three enabled broom spells");
        for (var index = 0; index < selections.size(); ++index) {
            helper.assertTrue(selections.get(index).slotIndex == index,
                    broomName + " broom selections should use compact wheel indices");
        }

        var entityStack = broom.getBroomItemStack();
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        entityStack, 2, ItemStack.EMPTY),
                broomName + " Broom should allow removing its third slot upgrade");
        broom.setBroomItemStack(entityStack);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(entityStack) == 2,
                broomName + " Broom should disable the third scroll slot after upgrade removal");
        helper.assertFalse(AbstractBroomItem.getCalibrationScroll(entityStack, 2).isEmpty(),
                broomName + " Broom should retain the disabled third scroll");
        helper.assertTrue(new SpellSelectionManager(rider)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).size() == 2,
                broomName + " rider should only receive enabled broom spells");

        rider.stopRiding();
        helper.assertTrue(new SpellSelectionManager(rider)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                broomName + " broom spells should disappear after dismounting");

        var menu = new SpellCalibrationBenchMenu(0, rider.getInventory());
        menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(entityStack);
        var disabledSlot = menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 2);
        helper.assertFalse(menu.isScrollSlotEnabled(2),
                broomName + " third scroll slot should be disabled in the Calibration Bench");
        helper.assertTrue(disabledSlot.hasItem() && disabledSlot.mayPickup(rider),
                broomName + " disabled scroll should remain removable from the Calibration Bench");
        var extracted = disabledSlot.remove(1);
        disabledSlot.onTake(rider, extracted);
        helper.assertFalse(extracted.isEmpty(), broomName + " disabled scroll extraction should return the scroll");
        helper.assertTrue(AbstractBroomItem.getCalibrationScroll(entityStack, 2).isEmpty(),
                broomName + " extracted disabled scroll should be removed from broom storage");
        broom.discard();
    }
}
