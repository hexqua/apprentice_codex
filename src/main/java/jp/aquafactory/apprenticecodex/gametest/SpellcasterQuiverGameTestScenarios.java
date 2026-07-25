package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.item.ammo.BowAmmoConsumptionNotification;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverPickupEvent;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import static jp.aquafactory.apprenticecodex.gametest.BowGameTestSupport.*;

final class SpellcasterQuiverGameTestScenarios {
    private SpellcasterQuiverGameTestScenarios() {
    }

    static void spellcasterQuiverUsesBackSlotAndCapsStoredArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            helper.assertTrue(quiverStack.is(CURIOS_BACK),
                    "Spellcaster Quiver should be tagged for the Curios back slot");

            var firstInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 300));
            var secondInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 300));
            helper.assertTrue(firstInsert == 300, "Spellcaster Quiver should store the full first stack");
            helper.assertTrue(secondInsert == 212,
                    "Spellcaster Quiver should stop at 512 arrows but inserted " + secondInsert);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 512,
                    "Spellcaster Quiver should cap total storage at 512");

            var removalOrderQuiver = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.ARROW, 32));
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.SPECTRAL_ARROW, 7));
            var removed = SpellcasterQuiver.removeOneStack(removalOrderQuiver);
            helper.assertTrue(removed.is(Items.SPECTRAL_ARROW) && removed.getCount() == 7,
                    "Spellcaster Quiver should remove the smallest stored arrow stack first");
        });
    }

    static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_pickup_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var itemEntity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.ARROW, 12));
            SpellcasterQuiverPickupEvent.onEntityItemPickup(new EntityItemPickupEvent(player, itemEntity));

            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 12,
                    "Equipped Spellcaster Quiver should auto-store picked up arrows");
            helper.assertTrue(itemEntity.isRemoved(),
                    "Spellcaster Quiver pickup handling should finish the ItemEntity when all arrows were stored");
        });
    }

    static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(bowStack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start drawing when only the equipped Spellcaster Quiver provides arrows");

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Elemental Bow should consume the equipped Spellcaster Quiver arrow first");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }

    static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_selection_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 3));
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var normalView = findElementalBowSelectionView(player, bowStack, "arrow", null);
            helper.assertTrue(normalView != null, "Elemental Bow should expose normal arrow selection");
            helper.assertTrue(normalView != null && "3".equals(normalView.badgeText()),
                    "Elemental Bow selection badge should count normal arrows stored in Spellcaster Quiver");

            var view = findElementalBowSelectionView(player, bowStack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));
            helper.assertTrue(view != null, "Elemental Bow should expose spectral arrow selection from Spellcaster Quiver contents");
            helper.assertTrue(view != null && "2".equals(view.badgeText()),
                    "Elemental Bow selection badge should count Spellcaster Quiver arrows");
        });
    }

    static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_quiver_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when Spellcaster Quiver provides arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Vanilla Bow should consume the Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Vanilla Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }

    static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_held_special_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when only the held special arrow should be selected: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Vanilla Bow should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Vanilla Bow should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }

    static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_normal_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when normal arrows exist outside the quiver: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Vanilla Bow should consume the lone normal arrow before more numerous Spellcaster Quiver special arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow should not consume Spellcaster Quiver special arrows while a normal arrow existed");
        });
    }

    static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_infinity_quiver_test");
            var bowStack = new ItemStack(Items.BOW);
            bowStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow Infinity fallback should stop at normal arrow mode and leave Spellcaster Quiver special arrows untouched");
        });
    }

    static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_held_special_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing when a held special arrow exists: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Elemental Bow vanilla mode should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Elemental Bow vanilla mode should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }

    static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            bowStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Elemental Bow vanilla mode Infinity fallback should leave Spellcaster Quiver special arrows untouched");
        });
    }

    static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while a bow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once bow use ends");
        });
    }

    static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_focus_staffbow_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.startUsingItem(InteractionHand.MAIN_HAND);

            helper.assertTrue(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should activate while the item is being held");
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while Focus Staffbow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should stop once use ends");
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once Focus Staffbow use ends");
        });
    }

    static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start when Spellcaster Quiver holds the catalyst arrow but got " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration());
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 1,
                    "Focus Staffbow should consume the equipped Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Focus Staffbow should leave loose inventory arrows untouched while the quiver still has arrows");
        });
    }

    static void bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_ammo_notification_count_test");
            var healingArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 4), Potions.HEALING);
            var poisonArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 3), Potions.POISON);
            player.getInventory().setItem(1, healingArrow.copy());
            player.setItemInHand(InteractionHand.OFF_HAND, poisonArrow.copy());

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(
                    quiverStack,
                    PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 2), Potions.HEALING)
            );
            SpellcasterQuiver.store(
                    quiverStack,
                    PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 5), Potions.POISON)
            );
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var packet = BowAmmoConsumptionNotification.createPacket(
                    player,
                    ItemRegistry.ELEMENTAL_BOW.getId(),
                    healingArrow
            );
            helper.assertTrue(packet.sourceId().equals(ItemRegistry.ELEMENTAL_BOW.getId().toString()),
                    "Bow ammo notification should preserve the source weapon id");
            helper.assertTrue(ItemStack.isSameItemSameTags(packet.iconStack(), healingArrow)
                            && packet.iconStack().getCount() == 1,
                    "Bow ammo notification should preserve the consumed tipped arrow as a single icon");
            helper.assertTrue(packet.remainingCount() == 6L,
                    "Bow ammo notification should total matching inventory and quiver arrows while excluding different potion NBT");
        });
    }

    static void focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_ammo_result_test");
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var consumed = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.ARROW_CATALYST
            );
            helper.assertTrue(consumed.successful() && consumed.consumedArrow()
                            && consumed.consumedStack().is(Items.ARROW)
                            && consumed.consumedStack().getCount() == 1,
                    "Focus Staffbow ammo result should preserve the arrow that was actually consumed");
            helper.assertTrue(BowAmmoConsumptionNotification.countRemaining(player, consumed.consumedStack()) == 4L,
                    "Focus Staffbow notification count should include the remaining quiver and inventory arrows");

            var bypassed = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.BYPASS
            );
            helper.assertTrue(bypassed.successful() && !bypassed.consumedArrow(),
                    "Focus Staffbow creative, Synthesis, and disabled-requirement bypasses should not report arrow consumption");

            var rejected = BowCastAmmoResolver.consumeFocusStaffbowAmmoWithResult(
                    player,
                    BowCastAmmoResolver.FocusStaffbowAmmoRoute.NONE
            );
            helper.assertTrue(!rejected.successful() && !rejected.consumedArrow(),
                    "Focus Staffbow missing-ammo results should fail without reporting arrow consumption");
        });
    }
}
