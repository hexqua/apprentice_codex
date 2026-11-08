package jp.aquafactory.apprenticecodex.gametest.equipment.revolvercaststaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffPendingAdvance;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertSingleModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenu;

final class RevolvercastStaffScenarios {
    private RevolvercastStaffScenarios() {}

    static void spellCalibrationBenchStoresScrollsOnRevolvercastStaff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolvercast_staff_storage_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var recoveryRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            var firstScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var longScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            var continuousScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());

            helper.assertTrue(menu.getSlot(0).mayPlace(staff),
                    "Revolvercast Staff should be accepted in the Spell Calibration Bench target slot");
            menu.getSlot(0).set(staff);
            helper.assertTrue(menu.isScrollSlotEnabled(0), "Revolvercast Staff scroll slot 0 should be enabled by default");
            helper.assertTrue(menu.isScrollSlotEnabled(3), "Revolvercast Staff scroll slot 3 should be enabled by default");
            helper.assertFalse(menu.isScrollSlotEnabled(4),
                    "Revolvercast Staff scroll slot 4 should be locked before adding an upgrade");
            helper.assertFalse(menu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Revolvercast Staff should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(menu.getSlot(1).mayPlace(lesserUpgrade),
                    "Revolvercast Staff should accept slot upgrade adjustments");
            helper.assertTrue(menu.getSlot(1).mayPlace(recoveryRune),
                    "Revolvercast Staff should accept Recovery Rune adjustments");
            helper.assertTrue(menu.getSlot(1).mayPlace(silverRing),
                    "Revolvercast Staff should accept Silver Ring adjustments");
            helper.assertFalse(menu.getSlot(1).mayPlace(enchantedBook),
                    "Revolvercast Staff should reject enchantment book adjustments");

            menu.getSlot(1).set(recoveryRune);
            helper.assertTrue(RevolvercastStaff.hasRecoveryRune(staff),
                    "Revolvercast Staff should enter skip mode after storing a Recovery Rune");
            helper.assertFalse(menu.getSlot(2).mayPlace(recoveryRune),
                    "Revolvercast Staff should reject a duplicate Recovery Rune adjustment");

            menu.getSlot(2).set(lesserUpgrade);
            helper.assertTrue(menu.isScrollSlotEnabled(5),
                    "One lesser slot upgrade should unlock six Revolvercast Staff scroll slots");
            helper.assertFalse(menu.isScrollSlotEnabled(6),
                    "One lesser slot upgrade should not unlock the seventh Revolvercast Staff scroll slot");
            helper.assertTrue(menu.getSlot(9).mayPlace(firstScroll.copy()),
                    "Newly unlocked Revolvercast Staff scroll slot should accept scrolls");
            helper.assertTrue(menu.getSlot(9).mayPlace(longScroll.copy()),
                    "Revolvercast Staff should accept long scroll storage so Silver Ring can enable it later");
            helper.assertFalse(menu.getSlot(9).mayPlace(continuousScroll),
                    "Revolvercast Staff should reject unsupported continuous scrolls in the Spell Calibration Bench");
            menu.getSlot(9).set(firstScroll);
            helper.assertFalse(RevolvercastStaff.getCalibrationScroll(staff, 5).isEmpty(),
                    "Scroll should be stored on the Revolvercast Staff NBT");
        });
    }

    static void revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            RevolvercastStaff.normalizeSelectedScrollIndex(staff);
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Empty Revolvercast Staff should not expose a spell container");

            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
            RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
            RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));

            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "First enabled Revolvercast Staff scroll should become selected");
            helper.assertFalse(ISpellContainer.isSpellContainer(staff), "Revolver must not project a host container");
            helper.assertTrue(RevolvercastStaff.getSelectedSpellData(staff).getSpell() == magicMissile,
                    "Initial Revolvercast Staff selected spell mismatch");

            helper.assertTrue(RevolvercastStaff.advanceToNextValidScrollIndex(staff),
                    "Revolvercast Staff should advance to the next valid scroll");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff should skip empty scroll slots while advancing");
            helper.assertTrue(RevolvercastStaff.getSelectedSpellData(staff).getSpell() == heal,
                    "Advanced Revolvercast Staff selected spell mismatch");

            RevolvercastStaff.setCalibrationScroll(staff, 3, createSpellScroll(fireball));
            RevolvercastStaff.setCalibrationScroll(staff, 2, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 3,
                    "Invalid Revolvercast Staff index should normalize to the next valid scroll");
            helper.assertTrue(RevolvercastStaff.getSelectedSpellData(staff).getSpell() == fireball,
                    "Normalized Revolvercast Staff selected spell mismatch");

            RevolvercastStaff.setCalibrationScroll(staff, 3, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Removing the selected last scroll should wrap to the first valid scroll");
            RevolvercastStaff.setCalibrationScroll(staff, 0, ItemStack.EMPTY);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == -1,
                    "Removing every Revolvercast Staff scroll should clear the selected index");
            helper.assertFalse(ISpellContainer.isSpellContainer(staff),
                    "Removing every Revolvercast Staff scroll should clear the exposed spell container");
        });
    }

    static void revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
            RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
            RevolvercastStaff.setSelectedScrollIndex(staff, 0);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "revolvercast_staff_cooldown_failure_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Revolvercast Staff cooldown failure test could not resolve player magic data");
            magicData.setMana(1000.0F);
            MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);

            helper.assertFalse(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Revolvercast Staff should fail to swing-cast a spell that is on cooldown");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Normal Revolvercast Staff mode should stay on a failed cooldown spell");

            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get())
            );
            helper.assertFalse(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Skip mode Revolvercast Staff should still fail to cast a spell that is on cooldown");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Skip mode Revolvercast Staff should advance after a cooldown failure");
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());
        });
    }

    static void revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(fireball));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_success_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff pending success test could not resolve player magic data");
        magicData.setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            helper.assertTrue(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Revolvercast Staff should successfully initiate the selected spell");
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should not advance in the same tick as cast initiation");

            RevolvercastStaffPendingAdvance.tickPlayer(player);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should wait until the next game tick before advancing");
        });

        helper.runAtTickTime(3, () -> {
            RevolvercastStaffPendingAdvance.tickPlayer(player);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff should advance after the successful cast completes and a tick passes");
            helper.succeed();
        });
    }

    static void revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var fireball = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(fireball));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_unrelated_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff unrelated completion test could not resolve player magic data");

        RevolvercastStaffPendingAdvance.reserve(player, InteractionHand.MAIN_HAND, staff, fireball, 0);
        var unrelatedMagicData = new MagicData();
        unrelatedMagicData.setPlayerCastingItem(new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()));
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, fireball, unrelatedMagicData, false);
        magicData.setPlayerCastingItem(staff);
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, fireball, magicData, false);

        helper.runAtTickTime(2, () -> {
            RevolvercastStaffPendingAdvance.tickPlayer(player);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 2,
                    "Revolvercast Staff pending advance should survive unrelated RemoteOwner cast completion");
            helper.succeed();
        });
    }

    static void revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(staff, 0, createSpellScroll(magicMissile));
        RevolvercastStaff.setCalibrationScroll(staff, 2, createSpellScroll(heal));
        RevolvercastStaff.setSelectedScrollIndex(staff, 0);

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "revolvercast_staff_pending_cancel_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Revolvercast Staff pending cancel test could not resolve player magic data");

        RevolvercastStaffPendingAdvance.reserve(player, InteractionHand.MAIN_HAND, staff, magicMissile, 0);
        magicData.setPlayerCastingItem(staff);
        RevolvercastStaffPendingAdvance.onServerCastComplete(player, magicMissile, magicData, true);

        helper.runAtTickTime(2, () -> {
            RevolvercastStaffPendingAdvance.tickPlayer(player);
            helper.assertTrue(RevolvercastStaff.getSelectedScrollIndex(staff) == 0,
                    "Revolvercast Staff should discard pending advancement when the cast is cancelled");
            helper.succeed();
        });
    }

    static void revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var staff = (RevolvercastStaff) stack.getItem();
            var modifiers = toModifierMultimap(staff.getDefaultAttributeModifiers(stack));
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.10D,
                    "Revolvercast Staff general spell power modifier changed"
            );
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
            );
            modifiers = toModifierMultimap(staff.getDefaultAttributeModifiers(stack));
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.FIRE_SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.15D,
                    "Fire rune should add Revolvercast Staff fire spell power"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Fire-tuned Revolvercast Staff should retain reduced general spell power"
            );
            helper.assertTrue(
                    SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Revolvercast Staff should reject Arcane Anvil spell imbuing"
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = SpellRegistry.ARCANE_BLAST.get();
            helper.assertTrue(RevolvercastStaff.canSwingCastSpell(stack, magicMissile),
                    "Revolvercast Staff should accept instant spells by default");
            helper.assertFalse(RevolvercastStaff.canSwingCastSpell(stack, heal),
                    "Revolvercast Staff should reject long spells without Silver Ring");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            helper.assertTrue(RevolvercastStaff.canSwingCastSpell(stack, heal),
                    "Silver Ring should enable Revolvercast Staff long swing-cast support");
            helper.assertFalse(RevolvercastStaff.canSwingCastSpell(stack, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()),
                    "Revolvercast Staff should reject continuous spells");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "revolvercast_staff_cooldown_mode_test");
            var longSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var currentCooldown = 80;
            var resolvedCooldown = staff.resolveSwingcastCooldownTicks(player, stack, longSpell, currentCooldown);
            helper.assertTrue(resolvedCooldown == currentCooldown + longSpell.getEffectiveCastTime(1, player),
                    "Revolvercast Staff long spell cooldown should be extended like Diamond Swingcast Staff");
        });
    }
}
