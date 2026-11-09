package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.BowGameTestSupport.*;

final class FocusStaffbowConfigScenarios {
    private FocusStaffbowConfigScenarios() {}

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
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
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
                var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, SpellRegistry.FORCE_FIELD.get(), 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(1000.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == InteractionResult.FAIL,
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
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
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
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.MANA_SLASH.get();
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
            var spell = SpellRegistry.MANA_SLASH.get();
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
                var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                        "Focus Staffbow should reject denylisted spells but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject denylisted spells before consuming arrows");
            }
        });
    }

    static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_SLASH.get();
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
                var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

                player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
                player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
                setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                        "Focus Staffbow should reject spells missing from the allowlist but got " + result.getResult());
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                        "Focus Staffbow should reject allowlist misses before consuming arrows");
            }
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
                var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
                var amplifierStack = new ItemStack(amplifierItem);
                amplifierItem.initializeSpellContainer(amplifierStack);
                setSingleUnlockedSpell(helper, amplifierStack, SpellRegistry.MANA_SLASH.get(), 1);

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
}
