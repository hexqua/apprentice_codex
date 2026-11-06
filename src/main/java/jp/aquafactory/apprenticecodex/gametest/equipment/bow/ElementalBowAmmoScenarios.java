package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowSelectionTestSupport.assertElementalBowSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowSelectionTestSupport.setElementalBowShotSelection;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ElementalBowAmmoScenarios {
    private ElementalBowAmmoScenarios() {}

    static void elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_infinity_preserve_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity while normal arrows exist: " + result.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow vanilla mode should not consume normal arrows while Infinity is enchanted");
        });
    }

    static void elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_preserve_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity while normal arrows exist: " + result.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow arrow-only mode should not consume normal arrows while Infinity is enchanted");
        });
    }

    static void elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_special_arrow_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing while the selected arrow exists: " + firstUse.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow special mode should consume the selected arrow even with Infinity");

            var secondUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(secondUse.getResult() == InteractionResult.FAIL,
                    "Elemental Bow special mode should fail after the selected arrow runs out: " + secondUse.getResult());
            assertElementalBowSelection(helper, stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"),
                    "Elemental Bow special mode should keep the selected arrow after ammo loss");
        });
    }
}
