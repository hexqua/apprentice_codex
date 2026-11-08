package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowMode;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowShotSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.BowGameTestSupport.assertElementalBowSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.assertTranslatableKey;

final class ElementalBowUseScenarios {
    private ElementalBowUseScenarios() {}

    static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_mana_gate_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow mana gate test could not resolve player mana data");
            magicData.setMana(0.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Elemental Bow should fail to start drawing when mana is insufficient: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow should not enter use state without enough mana");
        });
    }

    static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = ElementalBow.createInsufficientManaMessage(
                    SpellRegistry.FIRE_ARROW_SPELL.get(),
                    null
            );
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.irons_spellbooks.cast_error_mana",
                    "Elemental Bow mana error should use Iron's cast_error_mana key"
            );
        });
    }

    static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_partial_release_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow partial release test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow should start drawing when mana and ammo are available: " + useResult.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 19);
            helper.assertTrue(stack.getDamageValue() == 0, "Elemental Bow should not lose durability before full draw");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should not consume arrows before full draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana before full draw: " + magicData.getMana());
        });
    }

    static void elementalBowDoesNotCastWhenArrowDisappearsDuringDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_lost_during_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow lost-arrow test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow lost-arrow test should start drawing while ammo is available: " + useResult.getResult());
            player.getInventory().setItem(1, ItemStack.EMPTY);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS
            );
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 0,
                    "Elemental Bow should not lose durability when its catalyst arrow disappears during draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana when its catalyst arrow disappears during draw");
        });
    }

    static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start vanilla draw with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow should enter use state for Infinity vanilla draw");
        });
    }

    static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_special_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with only special arrows available: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow vanilla mode should consume the special arrow that vanilla resolution selected");
        });
    }

    static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_only_mode_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Elemental Bow arrow-only mode should fail when only special arrows are available: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow arrow-only mode should not enter use state without normal arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Elemental Bow arrow-only mode should not consume special arrows");
            assertElementalBowSelection(helper, stack, "arrow", null,
                    "Elemental Bow arrow-only mode should keep its selection while empty");
        });
    }

    static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow arrow-only mode should enter use state for Infinity draw");
        });
    }

    static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL,
                    "Elemental Bow magic mode should fail to start without ammo even with Infinity: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow magic mode should not enter use state without ammo");
        });
    }

    static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_empty_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows when Synthesis is enchanted: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 1,
                    "Elemental Bow Synthesis magic shot should still damage the bow after a successful cast");
            helper.assertTrue(magicData.getMana() < initialMana,
                    "Elemental Bow Synthesis magic shot should still consume spell mana");
        });
    }

    static void elementalBowCreativeAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_creative_empty_test");
            player.setGameMode(GameType.CREATIVE);
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows in Creative: " + result.getResult());
            helper.assertTrue(player.isUsingItem(),
                    "Elemental Bow magic mode should enter use state without arrows in Creative");
        });
    }

    static void elementalBowMagicArrowCatalystItemsAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useElementalBowMagicArrowCatalystItemsOverrideForGameTest(
                    List.of("minecraft:spectral_arrow")
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_configured_arrow_test");
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 2));
                player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Elemental Bow configured catalyst test could not resolve player mana data");
                magicData.setMana(250.0F);

                var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction(),
                        "Elemental Bow magic mode should accept a configured special arrow catalyst: " + result.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
                player.stopUsingItem();

                helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                        "Elemental Bow magic mode should consume the configured spectral arrow catalyst");
                helper.assertTrue(player.getInventory().getItem(2).getCount() == 3,
                        "Elemental Bow magic mode should leave unconfigured normal arrows untouched");
            }
        });
    }

    static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_ammo_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis ammo test could not resolve player mana data");
            magicData.setMana(250.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start with Synthesis while arrows are present: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow Synthesis magic shot should not consume arrows even when arrows are available");
        });
    }
}
