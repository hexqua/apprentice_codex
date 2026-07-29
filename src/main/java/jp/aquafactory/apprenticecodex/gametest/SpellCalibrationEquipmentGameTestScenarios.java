package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.MALUM_REPLENISHING;

final class SpellCalibrationEquipmentGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private SpellCalibrationEquipmentGameTestScenarios() {
    }

    static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.PHOTON_SIPHON.get();
            var stack = createInitializedPresetStack(item);
            var spellContainer = ISpellContainer.get(stack);

            helper.assertFalse(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Photon Siphon should not block external imbue as a UniqueItem");
            helper.assertTrue(spellContainer != null, "Photon Siphon default spell container is null");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon should still start with locked Mana Charge");
        });
    }
    static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "photon_siphon_calibration_repair_test");
            var item = (PhotonSiphon) ItemRegistry.PHOTON_SIPHON.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            var legacyReplacementStack = createInitializedPresetStack(item);
            applyLegacyLockedReplacement(helper, legacyReplacementStack, replacementSpell, 1);
            createSpellCalibrationBenchMenuWithTarget(player, legacyReplacementStack);
            var repairedReplacementContainer = ISpellContainer.get(legacyReplacementStack);
            helper.assertTrue(repairedReplacementContainer != null,
                    "Photon Siphon repaired replacement spell container is null");
            assertSpellData(helper, repairedReplacementContainer, 0, replacementSpell, 1, false,
                    "Photon Siphon Calibration Bench repair should unlock legacy non-default replacement spells");

            var defaultStack = createInitializedPresetStack(item);
            createSpellCalibrationBenchMenuWithTarget(player, defaultStack);
            var defaultContainer = ISpellContainer.get(defaultStack);
            helper.assertTrue(defaultContainer != null, "Photon Siphon default spell container is null after Calibration Bench check");
            assertSpellData(helper, defaultContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon Calibration Bench repair should not unlock the default Mana Charge");
        });
    }
    static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_target_slots_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);

            var emptyAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet);
            helper.assertTrue(emptyAmuletMenu.hasCalibrationTarget(),
                    "Empty Autocast Amulet should be accepted by Spell Calibration Bench");
            helper.assertTrue(emptyAmuletMenu.hasAutocastAmulet(),
                    "Autocast Amulet should be treated as a stored adjustment target");
            helper.assertTrue(emptyAmuletMenu.isAdjustmentSlotEnabled(0),
                    "Autocast Amulet should expose adjustment slots");
            helper.assertFalse(emptyAmuletMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Autocast Amulet should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(emptyAmuletMenu.getScrollItem(0).isEmpty(),
                    "Empty Autocast Amulet should not expose a scroll");

            var imbuedAmulet = new ItemStack(autocastAmulet);
            AutocastAmulet.setCalibrationScroll(imbuedAmulet, 0, createSpellScroll(SpellRegistry.SENSE_EVIL.get()));
            var imbuedAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedAmulet);
            helper.assertTrue(imbuedAmuletMenu.getScrollItem(0)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Imbued Autocast Amulet should expose a removable scroll");
            var longAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(longAmulet);
            var longAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, longAmulet);
            helper.assertTrue(longAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANTIS_LEAP.get())),
                    "Autocast Amulet should accept long scrolls even before Silver Ring adjustment");
            longAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(createSpellScroll(SpellRegistry.MANTIS_LEAP.get()));
            helper.assertTrue(longAmuletMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Autocast Amulet should warn that long spells cannot auto-cast before Silver Ring adjustment");
            longAmuletMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                    .set(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertFalse(longAmuletMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Autocast Amulet should clear the long spell warning after Silver Ring adjustment");

            var unsupportedArmor = createInitializedPresetStack(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
            var unsupportedArmorMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(unsupportedArmor),
                    "Apprentice Mage Torso should be accepted by Spell Calibration Bench because it shows Can be Imbued");

            var imbuedUnsupportedArmor = createInitializedPresetStack(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
            setSingleUnlockedSpell(helper, imbuedUnsupportedArmor,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
            helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(imbuedUnsupportedArmor),
                    "Imbued Apprentice Mage Torso should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            var imbuedUnsupportedArmorMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedUnsupportedArmor);
            helper.assertFalse(imbuedUnsupportedArmorMenu.hasOperationalImbueTarget(),
                    "Apprentice Mage Torso should remain unsupported by Calibration Bench operations");
            helper.assertTrue(imbuedUnsupportedArmorMenu.hasTargetSpellAt(0),
                    "Imbued Apprentice Mage Torso spell should be visible for unsupported slot hints");
            helper.assertTrue(imbuedUnsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .getItem().isEmpty(),
                    "Unsupported Calibration Bench targets should not expose a real removable scroll");
            helper.assertTrue(imbuedUnsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .remove(1).isEmpty(),
                    "Unsupported Calibration Bench targets should not allow scroll extraction");

            var emptyEnchantressRobe = new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get());
            helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            helper.assertTrue(SpellCalibrationImbueHelper.isSupportedTarget(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should support Calibration Bench operations when tag-allowed");

            helper.assertFalse(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get())),
                    "Enchantress Hat should not be accepted by Spell Calibration Bench");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var healScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            var suitHoodMenu = createSpellCalibrationBenchMenuWithTarget(player, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
            helper.assertTrue(suitHoodMenu.hasMagiAgentSuit(),
                    "Magi Agent Suit hood should be accepted by Spell Calibration Bench");
            helper.assertTrue(suitHoodMenu.isAdjustmentSlotEnabled(0),
                    "Magi Agent Suit should enable its first adjustment slot");
            helper.assertFalse(suitHoodMenu.isAdjustmentSlotEnabled(1),
                    "Magi Agent Suit should not enable more than one adjustment slot");
            helper.assertTrue(suitHoodMenu.getEnabledScrollSlotCount() == 0,
                    "Magi Agent Suit non-chest pieces should not expose scroll slots");
            helper.assertTrue(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START).mayPlace(fireRune),
                    "Magi Agent Suit should accept school runes in the first adjustment slot");
            helper.assertFalse(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START + 1).mayPlace(fireRune),
                    "Magi Agent Suit should reject school runes outside the first adjustment slot");
            helper.assertFalse(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                    "Magi Agent Suit non-chest pieces should reject scroll placement");

            var mithrilFreecastStaffMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get())
            );
            helper.assertTrue(mithrilFreecastStaffMenu.hasMithrilFreecastStaff(),
                    "Mithril Freecast Staff should be accepted by Spell Calibration Bench as an adjustment target");
            helper.assertTrue(mithrilFreecastStaffMenu.isAdjustmentSlotEnabled(0),
                    "Mithril Freecast Staff should expose adjustment slots");
            helper.assertTrue(mithrilFreecastStaffMenu.getEnabledScrollSlotCount() == 0,
                    "Mithril Freecast Staff should not expose scroll slots at the Spell Calibration Bench");
            helper.assertTrue(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .mayPlace(fireRune),
                    "Mithril Freecast Staff should accept school runes in adjustment slots");
            helper.assertTrue(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .mayPlace(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())),
                    "Mithril Freecast Staff should accept Silver Ring adjustments");
            helper.assertFalse(mithrilFreecastStaffMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                    "Mithril Freecast Staff should reject scroll placement");

            var gauntletWithFreecastAdjustmentMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get())
            );
            var playerInventoryMenuSlot = SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START
                    + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
            player.getInventory().setItem(9, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()));
            var quickMovedFreecastStaff = gauntletWithFreecastAdjustmentMenu.quickMoveStack(player, playerInventoryMenuSlot);
            helper.assertTrue(quickMovedFreecastStaff.is(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    "Shift-clicked Mithril Freecast Staff should move while Scrollcaster Gauntlet is the target");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(),
                    "Shift-clicked Mithril Freecast Staff should leave the player inventory");
            helper.assertTrue(gauntletWithFreecastAdjustmentMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                            .getItem()
                            .is(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    "Shift-clicked Mithril Freecast Staff should enter a Scrollcaster Gauntlet adjustment slot");

            var suitCoat = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get());
            var suitCoatMenu = createSpellCalibrationBenchMenuWithTarget(player, suitCoat);
            helper.assertTrue(suitCoatMenu.getEnabledScrollSlotCount() == 1,
                    "Magi Agent Suit coat should expose one scroll slot");
            suitCoatMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START).set(fireRune.copy());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(suitCoat, 0)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    "Magi Agent Suit coat should store a school rune through the Spell Calibration Bench");
            suitCoatMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(healScroll.copy());
            assertStackHasSpell(helper, suitCoat, io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(), 1,
                    "Magi Agent Suit coat should accept scroll imbue at the Spell Calibration Bench");

            var presetStaffMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    createInitializedPresetStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get())
            );
            helper.assertTrue(presetStaffMenu.getScrollItem(0).isEmpty(),
                    "Copper Swingcast Staff preset spell should not expose a removable scroll");
            helper.assertTrue(presetStaffMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())),
                    "Copper Swingcast Staff preset slot should accept a replacement scroll");
            var uninitializedPresetStaff = new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
            ISpellContainer.remove(uninitializedPresetStaff);
            helper.assertFalse(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                    "Prepared Copper Swingcast Staff test stack should not have spell_container");
            helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(uninitializedPresetStaff),
                    "Uninitialized preset spell containers should be accepted by Spell Calibration Bench");
            createSpellCalibrationBenchMenuWithTarget(player, uninitializedPresetStaff);
            helper.assertTrue(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                    "Spell Calibration Bench should initialize accepted preset spell containers");

            var spellcastersFlaskMenu = createSpellcasterWorkbenchMenuWithSingleInput(
                    player,
                    new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get())
            );
            var spellcastersFlaskResult = spellcastersFlaskMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
            helper.assertTrue(spellcastersFlaskResult.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask should keep the Workbench particle toggle result");
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(spellcastersFlaskResult),
                    "Spellcaster's Flask Workbench result should toggle particles off from the default state");

            var alchemistsFlask = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var defaultAlchemistsFlask = new ItemStack(alchemistsFlask);
            alchemistsFlask.initializeSpellContainer(defaultAlchemistsFlask);
            var alchemistsFlaskMenu = createSpellCalibrationBenchMenuWithTarget(player, defaultAlchemistsFlask);
            helper.assertTrue(alchemistsFlaskMenu.getScrollItem(0).isEmpty(),
                    "Alchemist's Flask preset Extract should not expose a removable scroll");

            var satelliteFollowcastMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
            );
            helper.assertTrue(satelliteFollowcastMenu.hasSatelliteFollowcastAmulet(),
                    "Satellite Followcast Amulet should be treated as a stored adjustment target");
            helper.assertTrue(satelliteFollowcastMenu.isAdjustmentSlotEnabled(0),
                    "Satellite Followcast Amulet should expose adjustment slots");
            helper.assertFalse(satelliteFollowcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Satellite Followcast Amulet should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(satelliteFollowcastMenu.getEnabledScrollSlotCount() == SatelliteFollowcastAmulet.MIN_SPELL_SLOTS,
                    "Satellite Followcast Amulet should start with one enabled scroll slot");
            helper.assertTrue(satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())),
                    "Satellite Followcast Amulet should accept profiled continuous scrolls even before Silver Ring adjustment");
            satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()));
            helper.assertTrue(satelliteFollowcastMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Satellite Followcast Amulet should warn that continuous spells cannot followcast before Silver Ring adjustment");
            satelliteFollowcastMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START)
                    .set(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertFalse(satelliteFollowcastMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Satellite Followcast Amulet should clear the continuous spell warning after Silver Ring adjustment");

            var fourSlotSatellite = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            for (var slot = 0; slot < SatelliteFollowcastAmulet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
                SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        fourSlotSatellite,
                        slot,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
                );
            }
            var fourSlotSatelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, fourSlotSatellite);
            helper.assertTrue(fourSlotSatelliteMenu.getEnabledScrollSlotCount() == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                    "Satellite Followcast Amulet should expose four scroll slots after three slot upgrades");

            var smashcastMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get())
            );
            helper.assertFalse(smashcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Smashcast Scepter should expose Calibration Bench spell restriction tooltip lines");
            helper.assertFalse(smashcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANA_CHARGE.get())),
                    "Smashcast Scepter should reject CONTINUOUS scrolls in the Spell Calibration Bench");
        });
    }

    static void spellCalibrationAdjustmentProfilesEnforceDeclaredRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targets = new ItemStack[]{
                    new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.AUTOCAST_AMULET.get()),
                    new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()),
                    new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get()),
                    new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get()),
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())
            };
            var expectedSlotCounts = new int[]{3, 3, 3, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1};
            var representativeAdjustments = new ItemStack[]{
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    new ItemStack(ItemRegistry.WISDOM_SHARD.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(ItemRegistry.WISDOM_SHARD.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())
            };
            for (var index = 0; index < targets.length; ++index) {
                var stack = targets[index];
                helper.assertTrue(stack.getItem() instanceof SpellCalibrationAdjustmentTarget,
                        "Every declared calibration target should implement SpellCalibrationAdjustmentTarget: " + index);
                var target = (SpellCalibrationAdjustmentTarget) stack.getItem();
                helper.assertTrue(target.getCalibrationAdjustmentSlotCount(stack) == expectedSlotCounts[index],
                        "Calibration target should expose its declared adjustment slot count: " + index);
                helper.assertFalse(target.getCalibrationAdjustmentProfile(stack).rules().isEmpty(),
                        "Calibration target should expose at least one adjustment rule: " + index);
                helper.assertTrue(target.canPlaceCalibrationAdjustment(stack, 0, representativeAdjustments[index]),
                        "Calibration target should accept its representative adjustment: " + index);
                helper.assertFalse(target.canPlaceCalibrationAdjustment(stack, 0, new ItemStack(Items.DIRT)),
                        "Calibration target should reject an unrelated item: " + index);
            }

            var gauntlet = targets[0];
            var gauntletProfile = ((SpellCalibrationAdjustmentTarget) gauntlet.getItem())
                    .getCalibrationAdjustmentProfile(gauntlet);
            helper.assertTrue(gauntletProfile.rules().size() == 4,
                    "Scrollcaster Gauntlet should expose all four hint groups from its rules");
            helper.assertTrue(gauntletProfile.rules().get(0).hint() instanceof CalibrationAdjustmentHint.TaggedItems,
                    "Scrollcaster Gauntlet slot upgrades should use a generated tag hint");
            helper.assertTrue(gauntletProfile.rules().get(3).hint() instanceof CalibrationAdjustmentHint.Translatable,
                    "Scrollcaster Gauntlet school runes should use a translated category hint");

            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var autocast = targets[4];
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            autocast, 0, silverRing),
                    "Autocast Amulet should accept its first Silver Ring");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            autocast, 1, silverRing),
                    "Autocast Amulet should reject a duplicate singleton adjustment");

            var slotUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var satellite = targets[5];
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            satellite, 0, slotUpgrade),
                    "Satellite Followcast Amulet should accept a slot upgrade");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            satellite, 1, slotUpgrade),
                    "Repeatable slot upgrades should be accepted more than once");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var iceRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get());
            var bulwark = targets[6];
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            bulwark, 0, fireRune),
                    "Bulwark Greatshield should accept its first school rune");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            bulwark, 1, fireRune),
                    "Bulwark Greatshield should accept a duplicate School ID");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            bulwark, 1, iceRune),
                    "Bulwark Greatshield should accept a different School ID");

            var parrycast = targets[7];
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            parrycast, 0, fireRune),
                    "Parrycast Buckler should reject School Runes");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            parrycast, 0, silverRing),
                    "Parrycast Buckler should accept its first Silver Ring");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            parrycast, 1, silverRing),
                    "Parrycast Buckler should reject a duplicate Silver Ring");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            parrycast, 1, new ItemStack(ItemRegistry.WISDOM_SHARD.get())),
                    "Parrycast Buckler should accept Wisdom Shard with Silver Ring");

            var sharpnessBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(sharpnessBook,
                    new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            var anotherSharpnessBook = sharpnessBook.copy();
            var unbreakingBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(unbreakingBook,
                    new EnchantmentInstance(Enchantments.UNBREAKING, 1));
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            gauntlet, 0, sharpnessBook),
                    "Scrollcaster Gauntlet should accept a supported enchantment book");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            gauntlet, 1, anotherSharpnessBook),
                    "Scrollcaster Gauntlet should reject the same first enchantment twice");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            gauntlet, 1, unbreakingBook),
                    "Scrollcaster Gauntlet should accept a different first enchantment");
            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                var replenishing = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_REPLENISHING);
                helper.assertTrue(replenishing != null, "Malum Replenishing is not registered");
                var replenishingBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(replenishingBook, new EnchantmentInstance(replenishing, 2));
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                gauntlet, 2, replenishingBook),
                        "Scrollcaster Gauntlet should accept a Replenishing enchanted book");
                helper.assertTrue(gauntlet.getEnchantmentLevel(replenishing) == 2,
                        "Scrollcaster Gauntlet should transfer Replenishing II from its calibration book");
            }

            var calibration = autocast.getTagElement("SpellCalibration");
            helper.assertTrue(calibration != null, "Legacy duplicate test requires calibration NBT");
            var adjustments = calibration.getList("Adjustments", Tag.TAG_COMPOUND);
            var duplicate = ((CompoundTag) adjustments.get(0).copy());
            duplicate.putInt("Slot", 1);
            adjustments.add(duplicate);
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(autocast, 1).isEmpty(),
                    "Legacy duplicate adjustment should remain readable");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            autocast, 0, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the first slot");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            autocast, 1, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the second slot");
        });
    }

    static void spellCalibrationBenchImbueStatesSeparateInsertionFromCurrentUsability(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_calibration_imbue_state_test");
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var wisdomShard = new ItemStack(ItemRegistry.WISDOM_SHARD.get());
            var longScroll = createSpellScroll(SpellRegistry.MANTIS_LEAP.get());
            var continuousScroll = createSpellScroll(SpellRegistry.MANA_CHARGE.get());
            var profiledContinuousScroll = createSpellScroll(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()
            );

            var autocast = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var autocastMenu = createSpellCalibrationBenchMenuWithTarget(player, autocast);
            assertCalibrationImbueState(helper, autocast, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Autocast Amulet should accept long spells with a warning before an enabling adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(autocast, 0, wisdomShard);
            assertCalibrationImbueState(helper, autocast, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Wisdom Shard runtime profiles should not change Calibration Bench insertion state");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(autocast, 1, silverRing);
            assertCalibrationImbueState(helper, autocast, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Autocast Amulet should mark long spells usable after an enabling adjustment");
            autocastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(longScroll.copy());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(autocast, 1, ItemStack.EMPTY);
            helper.assertFalse(AutocastAmulet.getCalibrationScroll(autocast, 0).isEmpty(),
                    "Removing an enabling adjustment should keep an already inserted scroll");
            helper.assertTrue(autocastMenu.shouldRenderMismatchCastConditionWarning(0),
                    "Removing an enabling adjustment should restore the configured-spell warning");

            var satellite = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            createSpellCalibrationBenchMenuWithTarget(player, satellite);
            assertCalibrationImbueState(helper, satellite, 0, profiledContinuousScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Satellite Followcast Amulet should accept continuous spells with a warning before an enabling adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(satellite, 0, silverRing);
            assertCalibrationImbueState(helper, satellite, 0, profiledContinuousScroll,
                    SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Satellite Followcast Amulet should mark continuous spells usable after an enabling adjustment");

            var revolver = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            createSpellCalibrationBenchMenuWithTarget(player, revolver);
            assertCalibrationImbueState(helper, revolver, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Revolvercast Staff should accept long spells with a warning before an enabling adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(revolver, 0, silverRing);
            assertCalibrationImbueState(helper, revolver, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Revolvercast Staff should mark long spells usable after an enabling adjustment");

            var parrycast = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            createSpellCalibrationBenchMenuWithTarget(player, parrycast);
            assertCalibrationImbueState(helper, parrycast, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Parrycast Buckler should accept long spells with a warning before an enabling adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(parrycast, 0, silverRing);
            assertCalibrationImbueState(helper, parrycast, 0, longScroll,
                    SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Parrycast Buckler should mark long spells usable after an enabling adjustment");

            var reflectcast = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            createSpellCalibrationBenchMenuWithTarget(player, reflectcast);
            assertCalibrationImbueState(helper, reflectcast, 0, profiledContinuousScroll,
                    SpellCalibrationImbueState.ACCEPTED_CURRENTLY_UNUSABLE,
                    "Reflectcast Shield should accept continuous spells with a warning before an enabling adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(reflectcast, 0, silverRing);
            assertCalibrationImbueState(helper, reflectcast, 0, profiledContinuousScroll,
                    SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Reflectcast Shield should mark continuous spells usable after an enabling adjustment");

            var rejectedAutocast = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var rejectedAutocastMenu = createSpellCalibrationBenchMenuWithTarget(player, rejectedAutocast);
            assertCalibrationImbueState(helper, rejectedAutocast, 0, continuousScroll,
                    SpellCalibrationImbueState.REJECTED,
                    "Autocast Amulet should reject permanently unsupported continuous spells");
            rejectedAutocastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(continuousScroll.copy());
            helper.assertTrue(AutocastAmulet.getCalibrationScroll(rejectedAutocast, 0).isEmpty(),
                    "Rejected Autocast Amulet spells should not be stored through direct Slot#set");
        });
    }
    static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_imbue_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var senseEvil = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();

            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);
            var emptyAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet);
            emptyAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(mageLight));
            assertAutocastSpellData(helper, emptyAmulet, 0, mageLight, 1,
                    "Calibration-imbued Autocast Amulet should contain mage_light");

            var twoSlotAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(twoSlotAmulet);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    twoSlotAmulet,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            AutocastAmulet.setCalibrationScroll(twoSlotAmulet, 0, createSpellScroll(senseEvil));
            var twoSlotAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 1).set(createSpellScroll(mageLight));
            helper.assertTrue(AutocastAmulet.getImbuedSpells(twoSlotAmulet).size() == 2,
                    "Calibration imbue should add a second Autocast Amulet spell");
            assertAutocastSpellData(helper, twoSlotAmulet, 0, senseEvil, 1,
                    "Calibration imbue should keep the existing Autocast Amulet spell");
            assertAutocastSpellData(helper, twoSlotAmulet, 1, mageLight, 1,
                    "Calibration imbue should add mage_light to the empty Autocast Amulet slot");

            var removedScroll = twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(removedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should return a scroll when removing an Autocast Amulet spell");
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).onTake(player, removedScroll);
            helper.assertTrue(AutocastAmulet.getSpellDataAt(twoSlotAmulet, 0) == SpellData.EMPTY
                            && AutocastAmulet.getSpellDataAt(twoSlotAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should not compact spell slots while removing a scroll");
            createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            helper.assertTrue(AutocastAmulet.getSpellDataAt(twoSlotAmulet, 0) == SpellData.EMPTY
                            && AutocastAmulet.getSpellDataAt(twoSlotAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should preserve empty spell slots when opening an existing target");

            var satelliteAmulet = new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get());
            var satelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, satelliteAmulet);
            satelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(mageLight));
            assertSatelliteSpellData(helper, satelliteAmulet, 0, mageLight, 1,
                    "Calibration-imbued Satellite Followcast Amulet should contain mage_light");

            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    satelliteAmulet,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var twoSlotSatelliteMenu = createSpellCalibrationBenchMenuWithTarget(player, satelliteAmulet);
            twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 1)
                    .set(createSpellScroll(magicMissile));
            helper.assertTrue(SatelliteFollowcastAmulet.getImbuedSpells(satelliteAmulet).size() == 2,
                    "Calibration imbue should add a second Satellite Followcast Amulet spell");
            var removedSatelliteScroll = twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(removedSatelliteScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should return a scroll when removing a Satellite Followcast Amulet spell");
            twoSlotSatelliteMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).onTake(player, removedSatelliteScroll);
            helper.assertTrue(SatelliteFollowcastAmulet.getSpellDataAt(satelliteAmulet, 0) == SpellData.EMPTY
                            && SatelliteFollowcastAmulet.getSpellDataAt(satelliteAmulet, 1) != SpellData.EMPTY,
                    "Calibration Bench should not compact Satellite Followcast Amulet slots while removing a scroll");

            var unsupportedMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            var unsupportedArmorStack = createInitializedPresetStack(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                            .mayPlace(unsupportedArmorStack),
                    "Calibration Bench should accept Can be Imbued targets for unsupported-operation hints");

            var externalSpellContainerStack = new ItemStack(Items.DIAMOND_SWORD);
            ISpellContainer.set(externalSpellContainerStack, ISpellContainer.create(1, false, false));
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(externalSpellContainerStack),
                    "Calibration Bench should accept items that show Iron's Can be Imbued tooltip");
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(externalSpellContainerStack),
                    "Generic external ISpellContainer items should remain unsupported by Calibration Bench operations");

            var magicMissileScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(unsupportedArmorStack, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject non-extractable Can be Imbued targets");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(externalSpellContainerStack, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject generic external ISpellContainer items");

            var invokeCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_INVOKE_CARD.get();
            var imbuedInvokeCard = invokeCard.createArcaneAnvilImbueResult(
                    new ItemStack(invokeCard),
                    new SpellData(heal, 1)
            );
            var invokeCardMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedInvokeCard);
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(imbuedInvokeCard),
                    "Spell Invoke Card should not be supported for Calibration Bench extraction");
            helper.assertTrue(SpellCalibrationImbueHelper.createScrollForSlot(imbuedInvokeCard, 0).isEmpty(),
                    "Spell Invoke Card should not create a scroll through Calibration Bench extraction");
            helper.assertTrue(invokeCardMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1).isEmpty(),
                    "Spell Invoke Card should not allow scroll extraction from the Calibration Bench menu");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(imbuedInvokeCard, 0, magicMissileScroll),
                    "Spell Invoke Card should not accept Calibration Bench scroll replacement");
            assertStackHasSpell(helper, imbuedInvokeCard, heal, 1,
                    "Blocked Calibration Bench extraction should keep the Spell Invoke Card spell");

            var autonomyCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_AUTONOMY_CARD.get();
            var imbuedAutonomyCard = autonomyCard.createArcaneAnvilImbueResult(
                    new ItemStack(autonomyCard),
                    new SpellData(heal, 1)
            );
            helper.assertTrue(SpellCalibrationImbueHelper.createScrollForSlot(imbuedAutonomyCard, 0).isEmpty(),
                    "Spell Autonomy Card should not create a scroll through Calibration Bench extraction");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(imbuedAutonomyCard, 0, magicMissileScroll),
                    "Spell Autonomy Card should not accept Calibration Bench scroll replacement");

            var illuminateStellarStaff = createInitializedPresetStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get());
            helper.assertFalse(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(illuminateStellarStaff),
                    "Calibration Bench should reject UniqueItem imbue targets");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(illuminateStellarStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject UniqueItem imbue targets");
            helper.assertFalse(SpellCalibrationImbueHelper.setScrollAt(illuminateStellarStaff, 0, magicMissileScroll.copy()),
                    "Calibration Bench should not directly set spells on UniqueItem targets");

            var crystalBladedStaff = createInitializedPresetStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            helper.assertFalse(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(crystalBladedStaff),
                    "Calibration Bench should keep Crystal Bladed Staff unsupported");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(crystalBladedStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should not expose Crystal Bladed Staff replacement");

            var mithrilFreecastStaff = createInitializedPresetStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(mithrilFreecastStaff),
                    "Calibration Bench should accept Mithril Freecast Staff as an adjustment target");
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(mithrilFreecastStaff, 0, magicMissileScroll),
                    "Calibration Bench server logic should reject direct spell insertion into Mithril Freecast Staff");
            helper.assertFalse(SpellCalibrationImbueHelper.setScrollAt(mithrilFreecastStaff, 0, magicMissileScroll.copy()),
                    "Calibration Bench should not directly set spells on Mithril Freecast Staff");

            var disallowedSpellMenu = createSpellCalibrationBenchMenuWithTarget(player, new ItemStack(autocastAmulet));
            helper.assertFalse(disallowedSpellMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(createSpellScroll(SpellRegistry.MANA_CHARGE.get())),
                    "Calibration Bench should not accept a spell rejected by the target item");

            var spellAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var spellAmplifierMenu = createSpellCalibrationBenchMenuWithTarget(player, spellAmplifier);
            spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, spellAmplifier, heal, 1,
                    "Calibration Bench should imbue generic extractable Spell Amplifiers");
            helper.assertFalse(spellAmplifierMenu.shouldRenderMismatchCastConditionWarning(0),
                    "A filled generic SpellContainer should not be treated as a configured-spell mismatch");
            helper.assertTrue(spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should extract generic Spell Amplifier spells");

            var manaForceBlade = createInitializedPresetStack(ItemRegistry.MANA_FORCE_BLADE.get());
            var manaForceBladeMenu = createSpellCalibrationBenchMenuWithTarget(player, manaForceBlade);
            manaForceBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, manaForceBlade, heal, 1,
                    "Calibration Bench should imbue the tag-allowed Mana Force Blade");
            helper.assertTrue(manaForceBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should extract Mana Force Blade spells");

            var circlet = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var circletMenu = createSpellCalibrationBenchMenuWithTarget(player, circlet);
            circletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, circlet, heal, 1,
                    "Calibration Bench should imbue tag-allowed extractable Curios");
        });
    }

    private static void assertCalibrationImbueState(
            GameTestHelper helper,
            ItemStack targetStack,
            int slot,
            ItemStack scrollStack,
            SpellCalibrationImbueState expected,
            String message
    ) {
        var actual = SpellCalibrationImbueHelper.evaluateScrollAt(targetStack, slot, scrollStack);
        helper.assertTrue(actual == expected, message + ": expected=" + expected + ", actual=" + actual);
    }
    static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var item = (MithrilFreecastStaff) stack.getItem();
            item.initializeSpellContainer(stack);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertFalse(stack.getItem() instanceof RestrictedSpellImbuableItem,
                    "Mithril Freecast Staff should not expose the restricted imbue API");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Mithril Freecast Staff should reject Arcane Anvil spell imbuing"
            );
        });
    }

    static void mithrilFreecastStaffCooldownUsesSelectedSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mithril_freecast_selected_cooldown_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Mithril Freecast Staff selected cooldown test could not resolve player magic data");
            magicData.setMana(1000.0F);

            var staff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var staffItem = (MithrilFreecastStaff) staff.getItem();
            staffItem.initializeSpellContainer(staff);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    staff,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));

            var normalSwordCooldown = MagicManager.getEffectiveSpellCooldown(magicMissile, player, CastSource.SWORD);
            var selectedSourceCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(selectedSourceCooldown == normalSwordCooldown,
                    "Scrollcaster Gauntlet selected SWORD source should use the normal SWORD cooldown: "
                            + selectedSourceCooldown + " / sword " + normalSwordCooldown);
            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == magicMissile,
                    "Mithril Freecast Staff cooldown test should resolve the selected offhand spell");
            var selectedStack = SpellSelectionStackResolver.resolveSelectionStack(player, selection.slot);
            helper.assertTrue(ItemStack.isSameItemSameTags(selectedStack, gauntlet),
                    "Mithril Freecast Staff cooldown test should resolve the selected offhand source stack");
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    magicMissile,
                    selection.getCastSource()
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                        "Mithril Freecast Staff should use the selected source cooldown but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
            }

            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            magicData.setPlayerCastingItem(staff.copy());
            var spellbookBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SPELLBOOK
            );
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    heal,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                        heal,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                var expectedCooldown = spellbookBaseCooldown + heal.getEffectiveCastTime(1, player);
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should use selected SPELLBOOK cooldown plus long cast time but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            var grimoire = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoire, 1);
            new ArchivistsGrimoire.ScrollInventory(grimoire).setStackInSlot(0, createSpellScroll(SpellRegistry.BOUND_BOW.get()));
            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoire);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    0
            ));
            magicData.getPlayerCooldowns().removeCooldown(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Mithril Freecast Staff should immediately trigger silver-ring Bound Bow from the spellbook slot");
            var boundBowRecast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(boundBowRecast != null,
                    "Mithril Freecast Staff silver-ring Bound Bow should create a recast before cooldown");
            magicData.getPlayerRecasts().removeRecast(boundBowRecast, RecastResult.USED_ALL_RECASTS);
            var expectedBoundBowCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                    player,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            var actualBoundBowCooldown = magicData.getPlayerCooldowns()
                    .getSpellCooldowns()
                    .get(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(
                    actualBoundBowCooldown != null
                            && actualBoundBowCooldown.getCooldownRemaining() == expectedBoundBowCooldown,
                    "Mithril Freecast Staff should keep the selected source cooldown for Bound Bow recast cooldown but got "
                            + (actualBoundBowCooldown == null ? "none" : actualBoundBowCooldown.getCooldownRemaining())
                            + " / expected " + expectedBoundBowCooldown
            );
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    player.getUUID(),
                    staff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            magicData.setPlayerCastingItem(grimoire.copy());
            var retainedRecastCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), player, CastSource.SWORD),
                    SpellRegistry.BOUND_BOW.get(),
                    player,
                    CastSource.SWORD
            );
            MinecraftForge.EVENT_BUS.post(retainedRecastCooldownEvent);
            helper.assertTrue(retainedRecastCooldownEvent.getEffectiveCooldown() == expectedBoundBowCooldown,
                    "Mithril Freecast Staff should consume retained recast cooldown source without relying on current casting item but got "
                            + retainedRecastCooldownEvent.getEffectiveCooldown()
                            + " / expected " + expectedBoundBowCooldown);
            helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            ).isEmpty(), "Mithril Freecast Staff should clear resolved retained recast cooldown source");
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    player.getUUID(),
                    staff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            MithrilFreecastStaffCastContext.clearPendingCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            );
            helper.assertTrue(MithrilFreecastStaffCastContext.resolveCooldownSource(
                    player.getUUID(),
                    grimoire,
                    SpellRegistry.BOUND_BOW.get()
            ).isEmpty(), "Mithril Freecast Staff should clear retained pending cooldown source without relying on current casting item");
            var timeoutPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                    "mithril_freecast_recast_timeout_cleanup_test");
            var timeoutMagicData = MagicData.getPlayerMagicData(timeoutPlayer);
            helper.assertTrue(timeoutMagicData != null,
                    "Mithril Freecast Staff timeout cleanup test could not resolve player magic data");
            var timeoutStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            equipNecklaceCurio(timeoutPlayer, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
            MithrilFreecastStaffCastContext.retainUntilCooldown(
                    timeoutPlayer.getUUID(),
                    timeoutStaff,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            var timeoutBoundBowRecast = new RecastInstance(
                    SpellRegistry.BOUND_BOW.get().getSpellId(),
                    1,
                    2,
                    20,
                    CastSource.SWORD,
                    null
            );
            timeoutMagicData.getPlayerRecasts().forceAddRecast(timeoutBoundBowRecast);
            timeoutMagicData.getPlayerRecasts().removeRecast(timeoutBoundBowRecast, RecastResult.TIMEOUT);
            helper.assertFalse(timeoutMagicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.BOUND_BOW.get()),
                    "Mithril Freecast Staff timeout cleanup test should exercise a cooldown-suppressed timeout path");
            var swordBoundBowCooldown = MagicManager.getEffectiveSpellCooldown(SpellRegistry.BOUND_BOW.get(), timeoutPlayer, CastSource.SWORD);
            var timeoutExpectedBoundBowCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                    timeoutPlayer,
                    SpellRegistry.BOUND_BOW.get(),
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(timeoutExpectedBoundBowCooldown != swordBoundBowCooldown,
                    "Mithril Freecast Staff timeout cleanup test needs SPELLBOOK and SWORD cooldowns to differ");
            timeoutMagicData.setPlayerCastingItem(new ItemStack(Items.STICK));
            var staleTimeoutCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    swordBoundBowCooldown,
                    SpellRegistry.BOUND_BOW.get(),
                    timeoutPlayer,
                    CastSource.SWORD
            );
            MinecraftForge.EVENT_BUS.post(staleTimeoutCooldownEvent);
            helper.assertTrue(staleTimeoutCooldownEvent.getEffectiveCooldown() == swordBoundBowCooldown,
                    "Mithril Freecast Staff should clear retained source after recast timeout but got "
                            + staleTimeoutCooldownEvent.getEffectiveCooldown() + " / expected " + swordBoundBowCooldown);

            var spellbookMagicMissileCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    magicMissile,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(spellbookMagicMissileCooldown != normalSwordCooldown,
                    "Mithril Freecast Staff stale pending test needs SPELLBOOK and SWORD cooldowns to differ");
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    magicMissile,
                    CastSource.SPELLBOOK
            )) {
                var immediateCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                magicData.setPlayerCastingItem(staff.copy());
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(immediateCooldownEvent);
                helper.assertTrue(immediateCooldownEvent.getEffectiveCooldown() == spellbookMagicMissileCooldown,
                        "Mithril Freecast Staff should apply selected SPELLBOOK cooldown immediately but got "
                                + immediateCooldownEvent.getEffectiveCooldown() + " / expected " + spellbookMagicMissileCooldown);
                MithrilFreecastStaffCastContext.retainUntilCooldown(
                        player.getUUID(),
                        staff,
                        magicMissile,
                        CastSource.SPELLBOOK
                );
            }
            var stalePendingCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    normalSwordCooldown,
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            magicData.setPlayerCastingItem(staff.copy());
            MithrilFreecastStaffCastEvent.onSpellCooldownAdded(stalePendingCooldownEvent);
            helper.assertTrue(stalePendingCooldownEvent.getEffectiveCooldown() == normalSwordCooldown,
                    "Mithril Freecast Staff should not retain stale selected source after instant cooldown but got "
                            + stalePendingCooldownEvent.getEffectiveCooldown() + " / expected " + normalSwordCooldown);

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var harvestMoon = SpellRegistry.HARVEST_MOON.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    harvestMoon,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(harvestMoon, player, CastSource.SWORD),
                        harvestMoon,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        harvestMoon,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should keep CraftsmansDelight on the selected SPELLBOOK cooldown but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    thermalProcess,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(thermalProcess, player, CastSource.SWORD),
                        thermalProcess,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        thermalProcess,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should keep Thermal Process on the selected SPELLBOOK cooldown with Magi boots and CraftsmansDelight but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            var artisanSmash = SpellRegistry.ARTISAN_SMASH.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    artisanSmash,
                    CastSource.SPELLBOOK
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(artisanSmash, player, CastSource.SWORD),
                        artisanSmash,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(cooldownEvent);
                var expectedCooldown = staffItem.resolveSwingTriggeredCooldownTicks(
                        player,
                        artisanSmash,
                        CastSource.SPELLBOOK
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should not re-reduce Magi boots cooldown after adding long cast time but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
            helper.assertTrue(staffItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Mithril Freecast Staff should initiate the selected instant offhand spell");
            var delayedCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    normalSwordCooldown,
                    magicMissile,
                    player,
                    CastSource.SWORD
            );
            MithrilFreecastStaffCastEvent.onSpellCooldownAdded(delayedCooldownEvent);
            helper.assertTrue(delayedCooldownEvent.getEffectiveCooldown() == selectedSourceCooldown,
                    "Mithril Freecast Staff should keep the selected source cooldown until delayed cooldown but got "
                            + delayedCooldownEvent.getEffectiveCooldown() + " / expected " + selectedSourceCooldown);
        });
    }

    private static void assertAutocastSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = AutocastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
    }

    private static void assertSatelliteSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = SatelliteFollowcastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
    }
}
