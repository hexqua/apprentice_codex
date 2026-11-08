package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.FocusStaffbowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowContinuousGameTests {
    private FocusStaffbowContinuousGameTests() {}

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_continuous_hold_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.FORCE_FIELD.get();
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
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous multiplier test could not resolve spell power attribute");
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            helper.assertTrue(modifier != null && modifier.amount() > 0.0D,
                    "Focus Staffbow continuous multiplier should start rising immediately after cast start");
        });
        helper.runAtTickTime(101, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous duration test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should stay active past the spell's normal duration cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep Iron's casting state active past the normal duration cap");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var elapsedTicks = continuousState.getElapsedTicks(player.level().getGameTime());
            var expectedRemaining = continuousState.requiredCastTicks - (int) elapsedTicks;
            helper.assertTrue(magicData.getCastDuration() == continuousState.requiredCastTicks,
                    "Focus Staffbow continuous cast should expose the spell's base cast duration");
            helper.assertTrue(magicData.getCastDurationRemaining() == expectedRemaining,
                    "Focus Staffbow continuous cast remaining duration should decrease monotonically: "
                            + magicData.getCastDurationRemaining() + " expected " + expectedRemaining);
            helper.assertTrue(magicData.getCastDurationRemaining() < 10,
                    "Focus Staffbow continuous cast should have passed Iron's normal remaining-duration stop window: " + magicData.getCastDurationRemaining());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous midpoint test could not resolve spell power attribute");
            var expectedMultiplier = 1.5D;
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(actualAmount - (expectedMultiplier - 1.0D)) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should match the fixed early-stage curve: " + actualAmount);
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should not keep consuming arrows while the button stays held");
        });
        helper.runAtTickTime(251, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous cap test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should remain active after reaching the 2x cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep running after reaching the 2x cap while mana remains");
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous cap test could not resolve spell power attribute");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var expectedMultiplier = 2.0D;
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(actualAmount - 1.0D) < 1.0e-9D,
                    "Focus Staffbow continuous spell power bonus should stop at +100%: " + actualAmount);
        });
        helper.runAtTickTime(252, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 251
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

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_continuous_standard_time_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(300.0F);

        var castTimeReductionAttribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
        helper.assertTrue(castTimeReductionAttribute != null,
                "Focus Staffbow continuous standard time test could not resolve cast time reduction attribute");
        if (castTimeReductionAttribute != null) {
            castTimeReductionAttribute.addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "focus_staffbow_continuous_standard_time_test"),
                    0.75D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
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
                        bowStack.getUseDuration(player) - 2
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

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_continuous_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, SpellRegistry.FORCE_FIELD.get(), 1);

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

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowCreativeContinuousReleaseSkipsCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_creative_continuous_cooldown_test");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow creative continuous cooldown test requires MagicData");
        magicData.setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow creative continuous cooldown test should start casting");
        });
        helper.runAtTickTime(3, () -> {
            var originalCreativeCooldown = ServerConfigs.CREATIVE_COOLDOWN.get();
            try {
                ServerConfigs.CREATIVE_COOLDOWN.set(false);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 2
                );
            } finally {
                ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            }
        });
        helper.runAtTickTime(4, () -> {
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Focus Staffbow creative CONTINUOUS release should respect disabled creative cooldowns");
            helper.succeed();
        });
    }
}
