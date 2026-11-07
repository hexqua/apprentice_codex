package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.FocusStaffbowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowManaGameTests {
    private FocusStaffbowManaGameTests() {}

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_instant_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow instant base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 40.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow instant test should start charging immediately but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow instant immediate release should only consume base mana: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow instant cast should consume one catalyst arrow on release");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_short_long_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.SLASH_BLADE.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow short LONG base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 60.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow short LONG test should enter pending charge but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow short LONG test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).chargeBaselineTicks
                            == FocusStaffbowChargeLogic.MINIMUM_OVERCHARGE_BASELINE_TICKS,
                    "Focus Staffbow short LONG test should clamp the overcharge baseline to one second");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - spell.getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow short LONG release should stay at base mana within the first second: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow short LONG cast should still consume only one catalyst arrow after completion");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var settings = new FocusStaffbowChargeSettings(
                    4.0D,
                    3.0D,
                    20,
                    1.0D,
                    0.5D
            );
            var pendingMaxTicks = 20L + 20L * 2L + 20L * 3L;
            var pendingMultiplier = FocusStaffbowChargeLogic
                    .computePendingChargeMultiplier(pendingMaxTicks, 20, settings);
            helper.assertTrue(Math.abs(pendingMultiplier - 4.0D) < 1.0e-9D,
                    "Focus Staffbow pending config should reach custom max within the fixed existing time window: "
                            + pendingMultiplier);

            var continuousMidpoint = FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(100L, settings);
            helper.assertTrue(Math.abs(continuousMidpoint - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach the midpoint at 100 ticks: " + continuousMidpoint);
            var continuousMax = FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(250L, settings);
            helper.assertTrue(Math.abs(continuousMax - 3.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach custom max at 250 ticks: " + continuousMax);

            var manaCost = FocusStaffbowChargeLogic
                    .computeScaledManaCost(10, 4.0D, settings);
            helper.assertTrue(manaCost == 20,
                    "Focus Staffbow mana config should apply multiplier and exponent before flooring: " + manaCost);
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_loan_repay_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow loan test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability before release");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).requiredCastTicks == 0,
                    "Focus Staffbow loan test should treat INSTANT spells as zero required cast ticks");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 60
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability after cast");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            var expectedLoanMana = baseManaCost * 3.0F;
            helper.assertTrue(Math.abs(loanState.remainingLoanMana - expectedLoanMana) < 1.0F,
                    "Focus Staffbow loan test should create three base-cost worth of debt at x2 but got "
                            + loanState.remainingLoanMana + " expected " + expectedLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan test should leave current mana at zero after borrowed cast: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow borrowed cast should still consume exactly one catalyst arrow");
            magicData.setMana(10.0F);
            FocusStaffbowCastManager.tickLoanRepayment(player);
            helper.assertTrue(Math.abs(loanState.remainingLoanMana - (expectedLoanMana - 10.0F)) < 1.0F,
                    "Focus Staffbow loan repay test should consume recovered mana into the debt first but got "
                            + loanState.remainingLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan repay test should keep displayed mana at zero while debt remains: " + magicData.getMana());
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_creative_overcharge_test");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow creative overcharge test could not resolve player mana data");
        magicData.setMana(17.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow creative overcharge test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow creative overcharge test lost spell data capability");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            helper.assertFalse(loanState.hasOutstandingLoan(),
                    "Focus Staffbow creative overcharge test should not create loan mana");
            helper.assertTrue(Math.abs(magicData.getMana() - 17.0F) < 1.0e-4F,
                    "Focus Staffbow creative overcharge test should leave mana unchanged but got " + magicData.getMana());
            helper.succeed();
        });
    }
}
