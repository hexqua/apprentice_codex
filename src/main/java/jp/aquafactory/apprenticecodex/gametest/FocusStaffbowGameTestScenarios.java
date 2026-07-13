package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.List;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.continuouscast.ContinuousCastDurationSimulation;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmash;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import static jp.aquafactory.apprenticecodex.gametest.BowGameTestSupport.*;

final class FocusStaffbowGameTestScenarios {
    private FocusStaffbowGameTestScenarios() {
    }

    static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_reject_test");
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when used from offhand but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when offhand use is rejected");
        });
    }

    static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_selection_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow offhand selection test could not resolve player mana data");
            if (magicData != null) {
                magicData.setMana(100.0F);
            }

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var selection = selectionManager.getSelection();
            helper.assertTrue(selection != null && io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(selection.slot),
                    "Focus Staffbow offhand selection test should resolve offhand spell selection but got " + selection);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() != net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow mainhand use should remain available even when selected spell slot is offhand but got "
                            + result.getResult());
        });
    }

    static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_summon_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should enter pending cast for LONG summon spells but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should keep a pending cast state while charging");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                    "Focus Staffbow should expose the summon weapon during pending charge");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should not consume its catalyst arrow before the LONG cast completes");
            helper.assertTrue(player.isUsingItem(), "Focus Staffbow should still be in use while the summon weapon is pending");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow pending state should clear after the charged cast completes");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow charged cast should clear simulated additional cast data after completion");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow should consume exactly one catalyst arrow after the LONG cast completes");
        });
    }

    static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_artisan_radius_test");
        var spell = (ArtisanSmash) jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARTISAN_SMASH.get();
        var spellLevel = 1;
        var magicData = MagicData.getPlayerMagicData(player);
        var baseSpellPower = spell.getSpellPower(spellLevel, player);
        var baseSplashRadius = Math.min(2.0F + baseSpellPower / 600.0F, 8.0F);

        helper.succeedIf(() -> {
            var launcher = spell.onCastNoWeapon(helper.getLevel(), spellLevel, player, magicData);
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellPowerAttribute != null,
                    "Focus Staffbow Artisan Smash test could not resolve spell power attribute");
            var modifier = new AttributeModifier(
                    FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                    "apprenticecodex.focus_staffbow.artisan_smash_radius_test",
                    2.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            var expectedSplashRadius = -1.0F;
            try {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                    spellPowerAttribute.addTransientModifier(modifier);
                }
                expectedSplashRadius = Math.min(2.0F + spell.getSpellPower(spellLevel, player) / 600.0F, 8.0F);
                spell.onCastCompleteWithWeapon(helper.getLevel(), spellLevel, player, magicData, false, launcher);
            } finally {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                }
            }

            var shells = helper.getLevel().getEntitiesOfClass(
                    ArtisanSmashShellEntity.class,
                    new AABB(player.position(), player.position()).inflate(32.0D)
            );
            helper.assertTrue(shells.size() == 1,
                    "Focus Staffbow Artisan Smash test should spawn exactly one shell but got " + shells.size());
            var actualSplashRadius = shells.get(0).getSplashRadius();
            helper.assertTrue(actualSplashRadius > baseSplashRadius + 0.01F,
                    "Artisan Smash splash radius should not stay at the pre-charge value: " + actualSplashRadius);
            helper.assertTrue(Math.abs(actualSplashRadius - expectedSplashRadius) < 0.01F,
                    "Artisan Smash splash radius should use charged spell power. expected="
                            + expectedSplashRadius + ", actual=" + actualSplashRadius);
        });
    }

    static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_cancel_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow cancel test should start a pending cast but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                helper.assertTrue(
                        getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                        "Focus Staffbow cancel test should spawn the summon weapon during pending charge"
                )
        );
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - (jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player) - 1)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow cancel test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should clear the pending state when released before the required charge");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).isEmpty(),
                    "Focus Staffbow should remove the simulated summon weapon when the charge is cancelled");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow should clear simulated additional cast data when the charge is cancelled");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should keep its catalyst arrow when the LONG cast is cancelled early");
        });
    }

    static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_hold_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(10000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous test should store a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous test should keep Iron's casting state active after start");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should consume one catalyst arrow as soon as casting starts");
            helper.assertTrue(player.isUsingItem(),
                    "Focus Staffbow continuous test should keep the player in use state while held");
        });
        helper.runAtTickTime(3, () -> {
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous multiplier test could not resolve spell power attribute");
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            helper.assertTrue(modifier != null && modifier.getAmount() > 0.0D,
                    "Focus Staffbow continuous multiplier should start rising immediately after cast start");
        });
        helper.runAtTickTime(101, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellData != null, "Focus Staffbow continuous duration test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should stay active past the spell's normal duration cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep Iron's casting state active past the normal duration cap");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var elapsedTicks = continuousState.getElapsedTicks(player.level().getGameTime());
            var expectedRemaining = ContinuousCastDurationSimulation.computeRemaining(
                    continuousState.requiredCastTicks, elapsedTicks
            );
            helper.assertTrue(magicData.getCastDuration() == continuousState.requiredCastTicks,
                    "Focus Staffbow continuous cast should expose the spell's base cast duration");
            helper.assertTrue(magicData.getCastDurationRemaining() == expectedRemaining,
                    "Focus Staffbow continuous cast remaining duration should decrease monotonically: "
                            + magicData.getCastDurationRemaining() + " expected " + expectedRemaining);
            helper.assertTrue(magicData.getCastDurationRemaining() < 10,
                    "Focus Staffbow continuous cast should have passed Iron's normal remaining-duration stop window: " + magicData.getCastDurationRemaining());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous midpoint test could not resolve spell power attribute");
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.getAmount();
            helper.assertTrue(Math.abs(actualAmount - (expectedMultiplier - 1.0D)) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should match the fixed early-stage curve: " + actualAmount);
            helper.assertTrue(Math.abs(expectedMultiplier - 1.5D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should reach 1.5x after 100 ticks: " + expectedMultiplier);
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should not keep consuming arrows while the button stays held");
        });
        helper.runAtTickTime(251, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellData != null, "Focus Staffbow continuous cap test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should remain active after reaching the 2x cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep running after reaching the 2x cap while mana remains");
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous cap test could not resolve spell power attribute");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.getAmount();
            helper.assertTrue(Math.abs(expectedMultiplier - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should cap at 2.0x after 250 ticks: " + expectedMultiplier);
            helper.assertTrue(Math.abs(actualAmount - 1.0D) < 1.0e-9D,
                    "Focus Staffbow continuous spell power bonus should stop at +100%: " + actualAmount);
        });
        helper.runAtTickTime(252, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - 251
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast state should clear after releasing the button");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous release should clear Iron's casting state");
        });
    }

    static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_arrow_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when no catalyst arrow is available but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state without a catalyst arrow");
        });
    }

    static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_standard_time_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(300.0F);

        var castTimeReductionAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get());
        helper.assertTrue(castTimeReductionAttribute != null,
                "Focus Staffbow continuous standard time test could not resolve cast time reduction attribute");
        if (castTimeReductionAttribute != null) {
            castTimeReductionAttribute.addPermanentModifier(new AttributeModifier(
                    UUID.fromString("6cc24610-4701-4af1-a197-f1403c48f2fb"),
                    "apprenticecodex.focus_staffbow.continuous_standard_time_test",
                    0.75D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));
        }

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous standard time test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous standard time test should store a CONTINUOUS cast state");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            helper.assertTrue(continuousState.requiredCastTicks == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should ignore cast-time attributes and use the spell's base castTime");
            helper.assertTrue(magicData.getCastDuration() == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should sync Iron's cast duration with the base castTime");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - 2
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous standard time test should clear after release");
        });
    }

    static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(15.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous mana test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous mana test should start with a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous mana test should still be casting immediately after start");
        });
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana stop test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast should stop once it cannot pay the next tick's mana cost");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous mana stop should clear Iron's casting state");
            helper.assertTrue(magicData.getMana() >= 0.0F,
                    "Focus Staffbow continuous mana stop should not drive mana below zero: " + magicData.getMana());
            helper.assertTrue(magicData.getMana() <= 15.0F,
                    "Focus Staffbow continuous mana stop consumed an unexpected amount of mana: " + magicData.getMana());
        });
    }

    static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_instant_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
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
                        bowStack.getUseDuration()
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow instant immediate release should only consume base mana: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow instant cast should consume one catalyst arrow on release");
        });
    }

    static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_short_long_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get();
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
                            == jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.MINIMUM_OVERCHARGE_BASELINE_TICKS,
                    "Focus Staffbow short LONG test should clamp the overcharge baseline to one second");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - spell.getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow short LONG release should stay at base mana within the first second: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow short LONG cast should still consume only one catalyst arrow after completion");
        });
    }

    static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var settings = new jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings(
                    4.0D,
                    3.0D,
                    20,
                    1.0D,
                    0.5D
            );
            var pendingMaxTicks = 20L + 20L * 2L + 20L * 3L;
            var pendingMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computePendingChargeMultiplier(pendingMaxTicks, 20, settings);
            helper.assertTrue(Math.abs(pendingMultiplier - 4.0D) < 1.0e-9D,
                    "Focus Staffbow pending config should reach custom max within the fixed existing time window: "
                            + pendingMultiplier);

            var continuousMidpoint = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(100L, settings);
            helper.assertTrue(Math.abs(continuousMidpoint - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach the midpoint at 100 ticks: " + continuousMidpoint);
            var continuousMax = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeContinuousChargeMultiplier(250L, settings);
            helper.assertTrue(Math.abs(continuousMax - 3.0D) < 1.0e-9D,
                    "Focus Staffbow continuous config should reach custom max at 250 ticks: " + continuousMax);

            var manaCost = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic
                    .computeScaledManaCost(10, 4.0D, settings);
            helper.assertTrue(manaCost == 20,
                    "Focus Staffbow mana config should apply multiplier and exponent before flooring: " + manaCost);
        });
    }

    static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_base_mana_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow mana gate test could not resolve player mana data");
            magicData.setMana(spell.getManaCost(1) - 1.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should still fail immediately when base mana is insufficient but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when even base mana is missing");
        });
    }

    static void focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                true,
                false,
                1.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_arrow_config_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start without arrows when arrow catalysts are disabled but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration()
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow arrow-disabled config should not create or consume arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    false,
                    true,
                    true,
                    1.0D,
                    List.of(),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_config_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get(), 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(1000.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject continuous spells when disabled but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject disabled continuous casts before consuming arrows");
            }
        });
    }

    static void focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                false,
                true,
                1.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_config_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(spell.getManaCost(1));

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan-disabled test should still start when base mana is available but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - 120
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                var spellData = Capabilities.getSpellDataOrNull(player);
                helper.assertTrue(spellData != null, "Focus Staffbow loan-disabled test lost spell data capability");
                helper.assertTrue(spellData != null
                                && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).hasOutstandingLoan(),
                        "Focus Staffbow should not create loan state when loan is disabled");
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject disabled loan before consuming arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowLoanRatioConfigRejectsExcessBorrowing(GameTestHelper helper) {
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        override[0] = useFocusStaffbowConfigOverrideForGameTest(
                true,
                true,
                true,
                0.0D,
                List.of(),
                false,
                List.of()
        );
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_ratio_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(spell.getManaCost(1));

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan-ratio test should still start when base mana is available but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration() - 120
                )
        );
        helper.runAtTickTime(3, () -> {
            try {
                var spellData = Capabilities.getSpellDataOrNull(player);
                helper.assertTrue(spellData != null, "Focus Staffbow loan-ratio test lost spell data capability");
                helper.assertTrue(spellData != null
                                && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).hasOutstandingLoan(),
                        "Focus Staffbow should not create loan state above the configured ratio");
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject loan-ratio overflow before consuming arrows");
                helper.succeed();
            } finally {
                override[0].close();
            }
        });
    }

    static void focusStaffbowSpellDenylistBlocksBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    1.0D,
                    List.of(spell.getSpellId()),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_denylist_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject denylisted spells but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject denylisted spells before consuming arrows");
            }
        });
    }

    static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            try (var ignored = useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    1.0D,
                    List.of(),
                    true,
                    List.of("irons_spellbooks:magic_arrow")
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_allowlist_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Focus Staffbow should reject spells missing from the allowlist but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject allowlist misses before consuming arrows");
            }
        });
    }

    static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_repay_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
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
                        bowStack.getUseDuration() - 60
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
            jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager.tickLoanRepayment(player);
            helper.assertTrue(Math.abs(loanState.remainingLoanMana - (expectedLoanMana - 10.0F)) < 1.0F,
                    "Focus Staffbow loan repay test should consume recovered mana into the debt first but got "
                            + loanState.remainingLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan repay test should keep displayed mana at zero while debt remains: " + magicData.getMana());
            helper.succeed();
        });
    }

    static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_creative_overcharge_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
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
                        bowStack.getUseDuration() - 120
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

    static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan block test could not resolve spell data capability");
            if (spellData != null) {
                spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, state -> state.addLoan(7.0F));
            }

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject new casts while borrowed mana remains but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not remain in use state while a loan blocks casting");
        });
    }

    static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_cooldown_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(200.0F);
            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null, "Focus Staffbow cooldown test could not resolve the selected spell");
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject use while the selected spell is on cooldown but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state while spell cooldown blocks casting");
        });
    }

    static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.FocusStaffbow.createLoanBlockedMessage(5.1F);
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.loan_mana",
                    "Focus Staffbow loan block message should use the dedicated translation key"
            );
        });
    }

    static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.FocusStaffbow.createInsufficientArrowMessage();
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.insufficient_arrow",
                    "Focus Staffbow insufficient arrow message should use the dedicated translation key"
            );
        });
    }

    static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_special_arrow_reject_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject special arrows that are not in arrowCatalystItems but got " + result.getResult());
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Focus Staffbow should not consume an unconfigured special arrow");
        });
    }

    static void focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    List.of("minecraft:spectral_arrow"),
                    3.0D,
                    2.0D,
                    20,
                    2.0D,
                    1.0D,
                    1.0D,
                    List.of(),
                    false,
                    List.of()
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_special_arrow_test");
                var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
                var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var configuredSpecialArrowId = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
                helper.assertTrue(ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds().contains(configuredSpecialArrowId),
                        "Focus Staffbow arrowCatalystItems override should contain " + configuredSpecialArrowId);
                helper.assertTrue(
                        BowCastAmmoResolver.resolveFocusStaffbowAmmoRoute(
                                player,
                                bowStack,
                                true,
                                ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds()
                        ) == BowCastAmmoResolver.FocusStaffbowAmmoRoute.ARROW_CATALYST,
                        "Focus Staffbow should resolve configured special arrow as arrow catalyst"
                );
                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction(),
                        "Focus Staffbow should start when a configured special arrow is available but got " + result.getResult());
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration()
                );
                helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                        "Focus Staffbow should consume the configured special arrow");
            }
        });
    }

    static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_synthesis_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        bowStack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start without arrows when Synthesis is enchanted but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration()
                )
        );
        helper.succeedWhen(() ->
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow Synthesis path should not require or consume a catalyst arrow")
        );
    }

    static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var item = (FocusStaffbow) stack.getItem();
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Focus Staffbow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.SYNTHESIS.get())),
                    "Focus Staffbow should accept Synthesis from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Focus Staffbow should allow Synthesis through anvil merges");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, Enchantments.INFINITY_ARROWS),
                    "Focus Staffbow should reject Infinity at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(Enchantments.INFINITY_ARROWS)),
                    "Focus Staffbow should reject Infinity from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, Enchantments.INFINITY_ARROWS),
                    "Focus Staffbow should reject Infinity through anvil merges");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.TRANSCENDENCE.get()),
                    "Focus Staffbow should reject Transcendence at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.TRANSCENDENCE.get())),
                    "Focus Staffbow should reject Transcendence from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, EnchantmentRegistry.TRANSCENDENCE.get()),
                    "Focus Staffbow should reject Transcendence through anvil merges");

            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = MalumHauntedCompat.getHauntedEnchantment();
            helper.assertTrue(haunted != null, "malum:haunted is not registered");
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, haunted),
                    "Focus Staffbow should allow malum:haunted at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(haunted)),
                    "Focus Staffbow should allow malum:haunted from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, haunted),
                    "Focus Staffbow should allow malum:haunted through anvil merges");

            var animated = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_ANIMATED);
            helper.assertTrue(animated != null, "malum:animated is not registered");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, animated),
                    "Focus Staffbow should keep rejecting malum:animated at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(animated)),
                    "Focus Staffbow should keep rejecting malum:animated from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, animated),
                    "Focus Staffbow should keep rejecting malum:animated through anvil merges");
        });
    }

    static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION
            ) - 3.0D) < 1.0e-9D, "Focus Staffbow attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION
            ) - (-3.0D)) < 1.0e-9D, "Focus Staffbow attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            ) - 0.10D) < 1.0e-9D, "Focus Staffbow spell power regression: " + describeModifiers(modifiers));
        });
    }
}
