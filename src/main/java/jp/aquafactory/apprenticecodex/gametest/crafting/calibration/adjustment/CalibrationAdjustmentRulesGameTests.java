package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.adjustment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.crafting.calibration.support.CalibrationTargetCases.adjustmentCases;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CalibrationAdjustmentRulesGameTests {
    private CalibrationAdjustmentRulesGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void declaredTargetsStoreAdjustmentsAndRejectInvalidWrites(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lookup = helper.getLevel().registryAccess();
            for (var testCase : adjustmentCases()) {
                var stack = testCase.targetStack();
                var context = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                var target = (SpellCalibrationAdjustmentTarget) stack.getItem();
                var adjustment = new ItemStack(testCase.adjustment(), 4);
                adjustment.set(DataComponents.CUSTOM_NAME, Component.literal("Adjustment " + context));
                var lastSlot = testCase.slots() - 1;
                helper.assertTrue(target.trySetCalibrationAdjustment(stack, lastSlot, adjustment, lookup),
                        "Last supported slot should accept its representative adjustment: " + context);
                helper.assertTrue(ItemStack.matches(adjustment.copyWithCount(1),
                                target.getCalibrationAdjustment(stack, lastSlot, lookup)),
                        "Accepted adjustment should be stored with components and normalized count: " + context);
                var snapshot = stack.copy();
                helper.assertFalse(target.trySetCalibrationAdjustment(stack, lastSlot, new ItemStack(Items.DIRT), lookup),
                        "Unrelated adjustment should be rejected: " + context);
                for (var invalidSlot : new int[]{-1, testCase.slots()}) {
                    helper.assertFalse(target.trySetCalibrationAdjustment(stack, invalidSlot, adjustment, lookup),
                            "Out-of-range write should be rejected: " + context + "/" + invalidSlot);
                    helper.assertTrue(target.getCalibrationAdjustment(stack, invalidSlot, lookup).isEmpty(),
                            "Out-of-range read should be empty: " + context + "/" + invalidSlot);
                }
                helper.assertTrue(ItemStack.matches(snapshot, stack),
                        "Rejected writes should preserve the complete target: " + context);
                helper.assertTrue(target.trySetCalibrationAdjustment(stack, lastSlot, ItemStack.EMPTY, lookup),
                        "Stored adjustment should remain removable: " + context);
                helper.assertTrue(target.getCalibrationAdjustment(stack, lastSlot, lookup).isEmpty(),
                        "Removal should clear the selected slot: " + context);
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void displayedAdjustmentCandidatesCanActuallyBeStored(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lookup = helper.getLevel().registryAccess();
            for (var testCase : adjustmentCases()) {
                var pristine = testCase.targetStack();
                var target = (SpellCalibrationAdjustmentTarget) pristine.getItem();
                for (var rule : target.getCalibrationAdjustmentProfile(pristine).rules()) {
                    var candidates = rule.collectDisplayCandidates();
                    var context = BuiltInRegistries.ITEM.getKey(pristine.getItem()) + "/" + rule.displayId();
                    helper.assertFalse(candidates.isEmpty(), "Displayed rule should offer a candidate: " + context);
                    for (var candidate : candidates) {
                        var stack = pristine.copy();
                        helper.assertTrue(target.trySetCalibrationAdjustment(stack, 0, candidate, lookup),
                                "Displayed candidate should pass the actual insertion path: " + context);
                        helper.assertTrue(ItemStack.matches(candidate.copyWithCount(1),
                                        target.getCalibrationAdjustment(stack, 0, lookup)),
                                "Displayed candidate should remain stored: " + context);
                    }
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void duplicateAndMixedAdjustmentsFollowEquipmentRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var autocast = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            autocast, 0, silverRing),
                    "Autocast Amulet should accept its first Silver Ring");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            autocast, 1, silverRing),
                    "Autocast Amulet should reject a duplicate singleton adjustment");

            var slotUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var satellite = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            satellite, 0, slotUpgrade),
                    "Satellite Followcast Amulet should accept a slot upgrade");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            satellite, 1, slotUpgrade),
                    "Repeatable slot upgrades should be accepted more than once");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var iceRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get());
            var bulwark = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            bulwark, 0, fireRune),
                    "Bulwark Greatshield should accept its first school rune");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            bulwark, 1, fireRune),
                    "Bulwark Greatshield should accept a duplicate School ID");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            bulwark, 1, iceRune),
                    "Bulwark Greatshield should accept a different School ID");

            var parrycast = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            parrycast, 0, fireRune),
                    "Parrycast Buckler should reject School Runes");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            parrycast, 0, silverRing),
                    "Parrycast Buckler should accept its first Silver Ring");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            parrycast, 1, silverRing),
                    "Parrycast Buckler should reject a duplicate Silver Ring");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            parrycast, 1, new ItemStack(ItemRegistry.WISDOM_SHARD.get())),
                    "Parrycast Buckler should accept Wisdom Shard with Silver Ring");

        });
    }
}
