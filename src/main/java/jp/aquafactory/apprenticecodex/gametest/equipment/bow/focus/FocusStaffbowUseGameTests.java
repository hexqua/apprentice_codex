package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.FocusStaffbowTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTranslatableKey;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowUseGameTests {
    private FocusStaffbowUseGameTests() {}

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_offhand_reject_test");
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when used from offhand but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when offhand use is rejected");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_offhand_selection_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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

            var selectionManager = new SpellSelectionManager(player);
            var selection = selectionManager.getSelection();
            helper.assertTrue(selection != null
                            && SpellSelectionManager.OFFHAND.equals(selection.slot),
                    "Focus Staffbow offhand selection test should resolve offhand spell selection but got " + selection);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() != InteractionResult.FAIL,
                    "Focus Staffbow mainhand use should remain available even when selected spell slot is offhand but got "
                            + result.getResult());
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_arrow_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when no catalyst arrow is available but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state without a catalyst arrow");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_base_mana_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should still fail immediately when base mana is insufficient but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when even base mana is missing");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mageLight = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get();
            var linearBuild = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LINEAR_BUILD.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12),
                    "focus_staffbow_precast_power_reject_test");
            helper.assertTrue(mageLight instanceof IChargecastStaffbowIncompatibleSpell
                            && FocusStaffbow.rejectsSpell(mageLight),
                    "Focus Staffbow should reject Mage Light through the pre-cast spell-power marker");
            helper.assertTrue(linearBuild instanceof IChargecastStaffbowIncompatibleSpell
                            && FocusStaffbow.rejectsSpell(linearBuild),
                    "Focus Staffbow should reject Linear Build through the pre-cast spell-power marker");
            assertTranslatableKey(
                    helper,
                    FocusStaffbow.createRejectedSpellMessage(mageLight.getDisplayName(player)),
                    "ui.apprenticecodex.focus_staffbow.reject_spell",
                    "Focus Staffbow should use its permanent rejection message"
            );

            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, mageLight, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should reject Mage Light but got " + result.getResult());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should reject Mage Light before consuming arrows");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedPlayer(
                    helper,
                    new BlockPos(12, 2, 12),
                    "focus_staffbow_creative_interruption_cooldown_test"
            );
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(
                    helper,
                    amplifierStack,
                    jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(),
                    1
            );
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));

            var interruptedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow creative interruption test requires MagicData");
            magicData.setSyncedData(new SyncedSpellData(player));
            magicData.initiateCast(interruptedSpell, 1, 60, CastSource.SPELLBOOK, "gametest");

            var originalCreativeCooldown = ServerConfigs.CREATIVE_COOLDOWN.get();
            InteractionResultHolder<ItemStack> result;
            try {
                ServerConfigs.CREATIVE_COOLDOWN.set(false);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
                result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            } finally {
                ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            }
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should accept input after interrupting a different creative cast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(interruptedSpell),
                    "Focus Staffbow creative interruption should respect disabled creative cooldowns");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_loan_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should reject new casts while borrowed mana remains but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not remain in use state while a loan blocks casting");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_cooldown_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(200.0F);
            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null, "Focus Staffbow cooldown test could not resolve the selected spell");
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should reject use while the selected spell is on cooldown but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state while spell cooldown blocks casting");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = FocusStaffbow.createLoanBlockedMessage(5.1F);
            helper.assertTrue(message.getContents() instanceof TranslatableContents contents
                            && contents.getArgs().length == 1 && contents.getArgs()[0].equals(6),
                    "Loan warning must round remaining mana upward for display");
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.loan_mana",
                    "Focus Staffbow loan block message should use the dedicated translation key"
            );
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_special_arrow_reject_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Focus Staffbow should reject special arrows that are not in arrowCatalystItems but got " + result.getResult());
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Focus Staffbow should not consume an unconfigured special arrow");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_synthesis_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        bowStack.enchant(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS),
                1
        );
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() ->
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow Synthesis path should not require or consume a catalyst arrow")
        );
    }
    @GameTest(template = "gametest/bow_casting")
    public static void focusStaffbowEquipmentUpdatesWithoutStackingAndRestoresAttributes(GameTestHelper helper) {
        try (var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
            var player = fixture.player();
            var damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            var speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
            var power = player.getAttributeValue(AttributeRegistry.SPELL_POWER);
            for (var repeat = 0; repeat < 2; repeat++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()));
                player.doTick();
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - (damage + 3.0D)) < 1.0e-9D,
                        "Equipping Focus Staffbow must apply one attack damage modifier");
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - (speed - 3.0D)) < 1.0e-9D,
                        "Equipping Focus Staffbow must apply one attack speed modifier");
                helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_POWER) - power * 1.1D) < 1.0e-9D,
                        "Equipping Focus Staffbow must apply its base spell power multiplier once");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.doTick();
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - damage) < 1.0e-9D
                                && Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - speed) < 1.0e-9D
                                && Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_POWER) - power) < 1.0e-9D,
                        "Removing Focus Staffbow must restore all watched attributes");
            }
        }
        helper.succeed();
    }
}
