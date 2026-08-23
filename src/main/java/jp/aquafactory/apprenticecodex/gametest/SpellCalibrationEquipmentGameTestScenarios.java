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
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorCalibration;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorExplosionKnockbackEvent;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.event.BetterCombatOffhandAttributeRescueEvent;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.fml.ModList;

import java.util.HashSet;

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
            helper.assertTrue(suitHoodMenu.isAdjustmentSlotEnabled(1)
                            && suitHoodMenu.isAdjustmentSlotEnabled(2),
                    "Magi Agent Suit should enable all three adjustment slots");
            helper.assertTrue(suitHoodMenu.getEnabledScrollSlotCount() == 0,
                    "Magi Agent Suit non-chest pieces should not expose scroll slots");
            helper.assertTrue(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START).mayPlace(fireRune),
                    "Magi Agent Suit should accept school runes in the first adjustment slot");
            helper.assertTrue(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START + 1).mayPlace(fireRune),
                    "Magi Agent Suit should accept a school rune in any empty adjustment slot");
            helper.assertFalse(suitHoodMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                    "Magi Agent Suit non-chest pieces should reject scroll placement");

            var mithrilFreecastStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var mithrilFreecastStaffMenu = createSpellCalibrationBenchMenuWithTarget(player, mithrilFreecastStaff);
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
            var mithrilItem = (MithrilFreecastStaff) mithrilFreecastStaff.getItem();
            helper.assertTrue(mithrilItem.getDefaultAttributeModifiers(mithrilFreecastStaff).modifiers().stream()
                            .anyMatch(entry -> entry.attribute().equals(
                                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER)
                                    && entry.modifier().operation()
                                    == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    && Math.abs(entry.modifier().amount() - 0.10D) < 0.000001D),
                    "Uncalibrated Mithril Freecast Staff should grant +10% generic spell power");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    mithrilFreecastStaff,
                    0,
                    fireRune
            );
            var tunedMithrilModifiers = mithrilItem.getDefaultAttributeModifiers(mithrilFreecastStaff);
            helper.assertTrue(tunedMithrilModifiers.modifiers().stream().anyMatch(entry ->
                            entry.attribute().equals(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER)
                                    && entry.modifier().operation()
                                    == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    && Math.abs(entry.modifier().amount() - 0.05D) < 0.000001D),
                    "Fire-tuned Mithril Freecast Staff should retain +5% generic spell power");
            helper.assertTrue(tunedMithrilModifiers.modifiers().stream().anyMatch(entry ->
                            entry.attribute().equals(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER)
                                    && entry.modifier().operation()
                                    == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    && Math.abs(entry.modifier().amount() - 0.15D) < 0.000001D),
                    "Fire-tuned Mithril Freecast Staff should grant +15% fire spell power");

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
            var targets = createDeclaredCalibrationAdjustmentTargets();
            var expectedSlotCounts = new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3, 3, 3, 3, 1,
                    3, 3, 3, 3, 3, 3, 3, 3};
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
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())
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
                var displayIds = new HashSet<String>();
                for (var rule : target.getCalibrationAdjustmentProfile(stack).rules()) {
                    helper.assertTrue(rule.displayId().matches("[a-z0-9_]+"),
                            "Calibration display ID should use snake_case: " + rule.displayId());
                    helper.assertTrue(displayIds.add(rule.displayId()),
                            "Calibration display IDs should be unique within a target: " + rule.displayId());
                    var effectLines = rule.effectLines();
                    helper.assertTrue(!effectLines.isEmpty() && effectLines.size() <= 3,
                            "Calibration display rule should expose one to three effect lines: "
                                    + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "/" + rule.displayId());
                    for (var effectLine : effectLines) {
                        helper.assertTrue(
                                effectLine.getContents() instanceof TranslatableContents contents
                                        && contents.getKey().startsWith(
                                        "jei.apprenticecodex.spell_calibration_bench.effect."
                                ),
                                "Calibration effect lines should use the dedicated translation key prefix: "
                                        + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "/" + rule.displayId()
                        );
                    }
                    var displayCandidates = rule.collectDisplayCandidates();
                    helper.assertFalse(displayCandidates.isEmpty(),
                            "Calibration display rule should expose at least one candidate: "
                                    + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "/" + rule.displayId());
                    for (var candidate : displayCandidates) {
                        helper.assertTrue(target.canPlaceCalibrationAdjustment(stack, 0, candidate),
                                "Calibration display candidate should pass the target rule: "
                                        + BuiltInRegistries.ITEM.getKey(candidate.getItem()));
                    }
                    var representative = displayCandidates.getFirst();
                    helper.assertTrue(
                            rule.conflicts(representative, representative)
                                    == (rule.duplicatePolicy()
                                    != jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule.DuplicatePolicy.REPEATABLE),
                            "Calibration duplicate policy should match its conflict rule: " + rule.displayId()
                    );
                    helper.assertTrue(
                            rule.constraintDisplay().translationKey().isEmpty()
                                    == (rule.duplicatePolicy()
                                    == jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule.DuplicatePolicy.REPEATABLE),
                            "Only repeatable calibration rules should omit constraint text: " + rule.displayId()
                    );
                }
                helper.assertTrue(target.canPlaceCalibrationAdjustment(stack, 0, representativeAdjustments[index]),
                        "Calibration target should accept its representative adjustment: " + index);
                helper.assertFalse(target.canPlaceCalibrationAdjustment(stack, 0, new ItemStack(Items.DIRT)),
                        "Calibration target should reject an unrelated item: " + index);
            }

            var gauntlet = targets[0];
            var gauntletProfile = ((SpellCalibrationAdjustmentTarget) gauntlet.getItem())
                    .getCalibrationAdjustmentProfile(gauntlet);
            helper.assertTrue(gauntletProfile.rules().size() == 3,
                    "Scrollcaster Gauntlet should expose its three supported hint groups");
            helper.assertTrue(gauntletProfile.rules().get(0).hint() instanceof CalibrationAdjustmentHint.TaggedItems,
                    "Scrollcaster Gauntlet slot upgrades should use a generated tag hint");
            helper.assertTrue(gauntletProfile.rules().get(2).hint() instanceof CalibrationAdjustmentHint.Translatable,
                    "Scrollcaster Gauntlet school runes should use a translated category hint");
            helper.assertTrue(gauntletProfile.rules().get(0).constraintDisplay().translationKey().isEmpty(),
                    "Scrollcaster Gauntlet slot upgrades should omit constraint text");
            helper.assertTrue(gauntletProfile.rules().get(1).constraintDisplay().translationKey()
                            .filter("jei.apprenticecodex.spell_calibration_bench.constraint.same_effect"::equals)
                            .isPresent(),
                    "Scrollcaster Gauntlet freecast staffs should use the same-effect constraint");
            helper.assertTrue(gauntletProfile.rules().get(2).constraintDisplay().translationKey()
                            .filter("jei.apprenticecodex.spell_calibration_bench.constraint.school_rune"::equals)
                            .isPresent(),
                    "Scrollcaster Gauntlet school runes should use the school-rune constraint");
            assertCalibrationEffectKeys(
                    helper,
                    gauntletProfile.rules().get(0),
                    "add_scroll_slot"
            );
            var slotEffect = (TranslatableContents) gauntletProfile.rules().get(0)
                    .effectLines().getFirst().getContents();
            helper.assertTrue(
                    slotEffect.getArgs().length == 1
                            && slotEffect.getArgs()[0] instanceof Number count
                            && count.intValue() == ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOTS_PER_UPGRADE,
                    "Scrollcaster Gauntlet slot upgrade effect should expose its actual added slot count"
            );
            assertCalibrationEffectKeys(
                    helper,
                    gauntletProfile.rules().get(2),
                    "change_spell_power_1",
                    "change_spell_power_2"
            );
            var schoolPowerEffects = gauntletProfile.rules().get(2).effectLines();
            var schoolPowerEffect = (TranslatableContents) schoolPowerEffects.get(0).getContents();
            var generalReductionEffect = (TranslatableContents) schoolPowerEffects.get(1).getContents();
            helper.assertTrue(
                    schoolPowerEffect.getArgs().length == 1
                            && schoolPowerEffect.getArgs()[0] instanceof Number schoolPower
                            && schoolPower.longValue() == 15L,
                    "School rune effect should expose +15% school spell power"
            );
            helper.assertTrue(
                    generalReductionEffect.getArgs().length == 1
                            && generalReductionEffect.getArgs()[0] instanceof Number reduction
                            && reduction.longValue() == 5L,
                    "School rune effect should expose a 5% general spell power reduction"
            );

            var autocastProfile = ((SpellCalibrationAdjustmentTarget) targets[4].getItem())
                    .getCalibrationAdjustmentProfile(targets[4]);
            assertCalibrationEffectKeys(
                    helper,
                    autocastProfile.rules().get(2),
                    "adapt_autocast_situation_1",
                    "adapt_autocast_situation_2"
            );

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

            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharpness = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
            var sharpnessBook = createEnchantedBook(sharpness, 1);
            sharpnessBook.set(DataComponents.CUSTOM_NAME, Component.literal("Stored calibration component"));
            sharpnessBook.setCount(4);
            var anotherSharpnessBook = sharpnessBook.copy();
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    sharpnessBook,
                    helper.getLevel().registryAccess()
            );
            var expectedStoredBook = sharpnessBook.copyWithCount(1);
            var storedBook = SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(gauntlet, 0);
            helper.assertTrue(storedBook.getCount() == 1
                            && ItemStack.isSameItemSameComponents(expectedStoredBook, storedBook),
                    "Common adjustment storage should preserve all components and normalize count to one");
            var restoredGauntlet = roundTripItemStack(helper, gauntlet);
            var restoredBook = SpellCalibrationAdjustmentGameTestSupport
                    .getCalibrationAdjustment(restoredGauntlet, 0);
            helper.assertTrue(ItemStack.isSameItemSameComponents(expectedStoredBook, restoredBook),
                    "Common adjustment storage should preserve components through ItemStack serialization");
            var commonRoot = getCustomDataTag(gauntlet);
            helper.assertTrue(commonRoot != null
                            && commonRoot.contains("ApprenticeCodexCalibrationAdjustments", Tag.TAG_COMPOUND),
                    "New writes should use the common adjustment storage root");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            gauntlet, 1, anotherSharpnessBook),
                    "Scrollcaster Gauntlet should reject new enchanted-book adjustments");

            var legacyAutocast = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            CustomData.update(DataComponents.CUSTOM_DATA, legacyAutocast, rootTag -> {
                var storedCalibration = new CompoundTag();
                var adjustments = new ListTag();
                var storedItem = new CompoundTag();
                storedItem.putString("ItemId", BuiltInRegistries.ITEM.getKey(silverRing.getItem()).toString());
                for (var slot = 0; slot < 2; ++slot) {
                    var entry = new CompoundTag();
                    entry.putInt("Slot", slot);
                    entry.put("Item", storedItem.copy());
                    adjustments.add(entry);
                }
                storedCalibration.put("Adjustments", adjustments);
                rootTag.put("SpellCalibration", storedCalibration);
            });
            var legacySnapshot = getCustomDataTag(legacyAutocast);
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(legacyAutocast, 1).isEmpty(),
                    "Legacy duplicate adjustment should remain readable");
            helper.assertTrue(legacySnapshot != null && legacySnapshot.equals(getCustomDataTag(legacyAutocast)),
                    "Reading legacy adjustments should not mutate their storage");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            legacyAutocast, 0, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the first slot");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(legacyAutocast, 1).is(silverRing.getItem()),
                    "First legacy mutation should migrate untouched adjustment slots");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            legacyAutocast, 1, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the second slot");
            var migratedRoot = getCustomDataTag(legacyAutocast);
            helper.assertTrue(migratedRoot == null || !migratedRoot.contains("SpellCalibration", Tag.TAG_COMPOUND),
                    "First legacy mutation should remove the legacy adjustment field");
        });
    }

    static void endgameArmorCalibrationAppliesSharedRulesAndAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var endgameArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get())
            };
            for (var stack : endgameArmor) {
                helper.assertTrue(stack.getItem() instanceof SpellCalibrationAdjustmentTarget target
                                && target.getCalibrationAdjustmentSlotCount(stack) == EndgameArmorCalibration.SLOT_COUNT,
                        "Every endgame armor piece should expose three calibration slots: "
                                + BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }

            var manaRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get());
            var manaLeggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            var manaItem = (ArmorItem) manaLeggings.getItem();
            var baseMana = modifierTotal(
                    manaItem.getDefaultAttributeModifiers(manaLeggings),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA,
                    AttributeModifier.Operation.ADD_VALUE
            );
            for (var slot = 0; slot < EndgameArmorCalibration.SLOT_COUNT; ++slot) {
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                manaLeggings, slot, manaRune.copy()),
                        "Arcane Rune should be repeatable in every endgame armor slot");
            }
            var tunedMana = modifierTotal(
                    manaItem.getDefaultAttributeModifiers(manaLeggings),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA,
                    AttributeModifier.Operation.ADD_VALUE
            );
            helper.assertTrue(Math.abs(tunedMana - baseMana - EndgameArmorCalibration.MAX_MANA_PER_RUNE * 3.0D) < 1.0e-9D,
                    "Three Arcane Runes should add 75 max mana to one armor piece");

            var protectionRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get());
            var protectionCoat = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            protectionCoat, 0, protectionRune),
                    "Protective Rune should be accepted by endgame armor");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            protectionCoat, 1, protectionRune),
                    "Protective Rune should be unique within one armor piece");
            helper.assertTrue(Math.abs(modifierTotal(
                            ((ArmorItem) protectionCoat.getItem()).getDefaultAttributeModifiers(protectionCoat),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_RESIST,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ) - EndgameArmorCalibration.SPELL_RESIST_PER_RUNE) < 1.0e-9D,
                    "Protective Rune should add 5% generic spell resistance");

            var blastBoots = new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            blastBoots, 0, new ItemStack(ItemRegistry.BLAST_REACTIVE_PLATE.get())),
                    "Blast-Reactive Plate should be accepted");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            blastBoots, 1, new ItemStack(ItemRegistry.WIND_ACCUMULATION_WEAVE.get())),
                    "Wind Accumulation Weave should coexist with Blast-Reactive Plate");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            blastBoots, 2, new ItemStack(ItemRegistry.SHOCK_ABSORPTION_PLATE.get())),
                    "Shock Absorption Plate should coexist with explosion knockback adjustments");
            helper.assertTrue(Math.abs(modifierTotal(
                            ((ArmorItem) blastBoots.getItem()).getDefaultAttributeModifiers(blastBoots),
                            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                            AttributeModifier.Operation.ADD_VALUE
                    ) - 0.05D) < 1.0e-9D,
                    "Blast and wind adjustments should combine to 5% explosion knockback resistance");
            helper.assertTrue(Math.abs(modifierTotal(
                            ((ArmorItem) blastBoots.getItem()).getDefaultAttributeModifiers(blastBoots),
                            Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier.Operation.ADD_VALUE
                    ) - EndgameArmorCalibration.KNOCKBACK_RESIST_PER_PLATE) < 1.0e-9D,
                    "Shock Absorption Plate should add 10% knockback resistance");

            var scrollLeggings = new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get());
            helper.assertFalse(ISpellContainer.isSpellContainer(scrollLeggings),
                    "Scrollwoven armor should start without a spell container");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            scrollLeggings, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Scrollwoven Parchment should be accepted by non-chest armor");
            helper.assertFalse(ISpellContainer.isSpellContainer(scrollLeggings),
                    "Scrollwoven Parchment should use independent storage instead of a spell container");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()),
                            0,
                            new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Chest armor should reject Scrollwoven Parchment");

            var leatherBootsAdjustment = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            leatherBootsAdjustment, 0, new ItemStack(Items.LEATHER_BOOTS)),
                    "Endgame boots should accept Leather Boots");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "endgame_armor_powder_snow_test");
            helper.assertTrue(((ArmorItem) leatherBootsAdjustment.getItem())
                            .canWalkOnPowderedSnow(leatherBootsAdjustment, player),
                    "Leather Boots adjustment should allow walking on powder snow");
            helper.assertFalse(leatherBootsAdjustment.is(ItemTags.FREEZE_IMMUNE_WEARABLES),
                    "Leather Boots adjustment should not grant freeze immunity");

            var windOnlyBoots = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            windOnlyBoots, 0, new ItemStack(ItemRegistry.WIND_ACCUMULATION_WEAVE.get())),
                    "Wind Accumulation Weave should be accepted by endgame boots");
            player.setItemSlot(EquipmentSlot.FEET, windOnlyBoots);
            var explosionResistance = player.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            helper.assertTrue(explosionResistance != null,
                    "Player should expose explosion knockback resistance");
            var windTestModifierId = ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex", "gametest_wind_accumulation"
            );
            var existingUnclampedResistance =
                    EndgameArmorExplosionKnockbackEvent.calculateUnclampedValue(explosionResistance);
            var temporaryAdjustment = EndgameArmorCalibration.WIND_EXPLOSION_KNOCKBACK_RESIST
                    - existingUnclampedResistance;
            if (Math.abs(temporaryAdjustment) > 1.0e-9D) {
                explosionResistance.addTransientModifier(new AttributeModifier(
                        windTestModifierId,
                        temporaryAdjustment,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
            var knockbackEvent = new ExplosionKnockbackEvent(
                    helper.getLevel(), null, player, new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D)
            );
            EndgameArmorExplosionKnockbackEvent.onExplosionKnockback(knockbackEvent);
            helper.assertTrue(Math.abs(knockbackEvent.getKnockbackVelocity().x - 1.20D) < 1.0e-9D,
                    "Negative explosion resistance should amplify explosion knockback by 20%");
            if (Math.abs(temporaryAdjustment) > 1.0e-9D) {
                explosionResistance.removeModifier(windTestModifierId);
            }

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var suitHood = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(suitHood, 2, fireRune),
                    "Magi Agent Suit should accept its school rune in the third slot");
            helper.assertTrue(MagiAgentSuitItem.getResolvedCalibrationSchool(suitHood) != null,
                    "Magi Agent Suit should resolve a school rune from any adjustment slot");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()), 0, fireRune),
                    "Chromatic Magia Dress should reject school runes");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()), 0, fireRune),
                    "Element Maiden Robe should reject school runes");

            var soulPlate = new ItemStack(ItemRegistry.SOUL_COVERED_PLATE.get());
            var createLoaded = ModList.get().isLoaded("create");
            var malumLoaded = ModList.get().isLoaded("malum");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()), 0, soulPlate) == malumLoaded,
                    "Malum adjustment availability should follow whether Malum is loaded");
            if (malumLoaded) {
                var soulArmor = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                soulArmor, 0, soulPlate),
                        "Soul-Covered Plate should be accepted when Malum is loaded");
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                soulArmor, 1, new ItemStack(ItemRegistry.SOUL_AUGMENTED_WEAVE.get())),
                        "Soul-Augmented Weave should be accepted when Malum is loaded");
                var soulModifiers = ((ArmorItem) soulArmor.getItem()).getDefaultAttributeModifiers(soulArmor);
                assertOptionalAttributeModifier(helper, soulModifiers,
                        EndgameArmorCalibration.SOUL_WARD_CAPACITY_ATTRIBUTE,
                        EndgameArmorCalibration.SOUL_WARD_CAPACITY,
                        AttributeModifier.Operation.ADD_VALUE);
                assertOptionalAttributeModifier(helper, soulModifiers,
                        EndgameArmorCalibration.SOUL_WARD_RECOVERY_RATE_ATTRIBUTE,
                        EndgameArmorCalibration.SOUL_WARD_RECOVERY_RATE,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                assertOptionalAttributeModifier(helper, soulModifiers,
                        EndgameArmorCalibration.MAGIC_PROFICIENCY_ATTRIBUTE,
                        EndgameArmorCalibration.MAGIC_PROFICIENCY,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
            if (createLoaded) {
                var goggles = BuiltInRegistries.ITEM.get(EndgameArmorCalibration.CREATE_GOGGLES).getDefaultInstance();
                var gogglesHood = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get());
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                gogglesHood, 0, goggles),
                        "Create Goggles should be accepted by an endgame helmet when Create is loaded");
                helper.assertTrue(EndgameArmorCalibration.hasCreateGoggles(gogglesHood),
                        "Calibrated helmet should expose the Create Goggles state");
                player.setItemSlot(EquipmentSlot.HEAD, gogglesHood);
                helper.assertTrue((boolean) invokeCreateGameTestHook(
                                "isWearingGoggles",
                                new Class<?>[]{net.minecraft.world.entity.player.Player.class},
                                player
                        ),
                        "Create should recognize a calibrated endgame helmet as worn goggles");
            }
        });
    }

    static void endgameArmorScrollwovenSlotsPersistAndFollowSelectionOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellSelectionPriority(helper, ArchivistsGrimoireSpellSelectionEvents.class, EventPriority.HIGHEST);
            assertSpellSelectionPriority(helper, EnderGrimoireSpellSelectionEvents.class, EventPriority.HIGHEST);
            assertSpellSelectionPriority(helper, EndgameArmorSpellSelectionEvents.class, EventPriority.HIGH);
            assertSpellSelectionPriority(helper, SwingcastStaffSpellSelectionEvents.class, EventPriority.NORMAL);
            assertSpellSelectionPriority(helper, BroomSpellSelectionEvents.class, EventPriority.LOW);
            assertSpellSelectionPriority(helper, CallBroomSpellSelectionEvents.class, EventPriority.LOW);
            assertSpellSelectionPriority(helper, BetterCombatOffhandAttributeRescueEvent.class, EventPriority.LOWEST);

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scroll = createSpellScroll(spell);
            var eligibleArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get())
            };
            for (var armorStack : eligibleArmor) {
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                armorStack, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                        "Eligible endgame armor should accept Scrollwoven Parchment: "
                                + BuiltInRegistries.ITEM.getKey(armorStack.getItem()));
                helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(armorStack) == 1,
                        "Scrollwoven Parchment should enable one stored scroll slot");
                helper.assertTrue(SpellCalibrationImbueHelper.evaluateScrollAt(armorStack, 0, scroll, helper.getLevel().registryAccess())
                                == SpellCalibrationImbueState.ACCEPTED_USABLE,
                        "Enabled endgame armor should accept an unrestricted spell scroll");
                EndgameArmorCalibration.setStoredScroll(armorStack, 0, scroll, helper.getLevel().registryAccess());
                helper.assertTrue(EndgameArmorCalibration.getStoredSpellData(armorStack).getSpell() == spell,
                        "Endgame armor should preserve the stored scroll spell");
                helper.assertFalse(ISpellContainer.isSpellContainer(armorStack),
                        "Non-chest endgame armor should not become a spell container");
            }

            var dormantArmor = eligibleArmor[0];
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            dormantArmor, 0, ItemStack.EMPTY),
                    "Scrollwoven Parchment should be removable while a scroll is stored");
            helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(dormantArmor) == 0,
                    "Removing Scrollwoven Parchment should disable the stored slot");
            helper.assertTrue(EndgameArmorCalibration.getStoredSpellData(dormantArmor).getSpell() == spell,
                    "Removing Scrollwoven Parchment should preserve the dormant scroll");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "scrollwoven_armor_test");
            var dormantMenu = createSpellCalibrationBenchMenuWithTarget(player, dormantArmor);
            helper.assertFalse(dormantMenu.isScrollSlotEnabled(0),
                    "A dormant armor scroll slot should remain disabled");
            helper.assertFalse(dormantMenu.getScrollItem(0).isEmpty(),
                    "A dormant stored scroll should remain visible at the Calibration Bench");
            helper.assertTrue(dormantMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPickup(player),
                    "A dormant stored scroll should remain extractable");
            helper.assertFalse(dormantMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(scroll),
                    "A dormant armor scroll slot should reject insertion");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            dormantArmor, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Scrollwoven Parchment should be reinstallable");
            helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(dormantArmor) == 1
                            && EndgameArmorCalibration.getStoredSpellData(dormantArmor).getSpell() == spell,
                    "Reinstalling Scrollwoven Parchment should reactivate the stored scroll");

            var chestArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get())
            };
            for (var armorStack : chestArmor) {
                helper.assertFalse(EndgameArmorCalibration.usesStoredCalibrationScrolls(armorStack),
                        "Chest armor should preserve its native spell container path");
                helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(armorStack) == 0,
                        "Chest armor should not expose an auxiliary stored scroll slot");
            }

            var head = eligibleArmor[0];
            var legs = eligibleArmor[1];
            var feet = eligibleArmor[2];
            player.setItemSlot(EquipmentSlot.HEAD, head);
            player.setItemSlot(EquipmentSlot.LEGS, legs);
            player.setItemSlot(EquipmentSlot.FEET, feet);
            var staff = new ItemStack(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get());
            var mutable = ISpellContainer.create(1, false, false).mutableCopy();
            mutable.addSpellAtIndex(spell, 1, 0, false);
            ISpellContainer.set(staff, mutable.toImmutable());
            helper.assertTrue(((AbstractSwingcastStaffItem) staff.getItem()).allowImbuedSpellInSpellWheel(staff),
                    "Diamond Swingcast Staff should provide the NORMAL-priority comparison option");
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);

            var options = new SpellSelectionManager(player).getAllSpells();
            helper.assertTrue(options.size() >= 4,
                    "Three armor slots and Swingcast Staff should all remain independent selections");
            helper.assertTrue(EndgameArmorSpellSelectionEvents.HEAD_SLOT.equals(options.get(0).slot)
                            && EndgameArmorSpellSelectionEvents.LEGS_SLOT.equals(options.get(1).slot)
                            && EndgameArmorSpellSelectionEvents.FEET_SLOT.equals(options.get(2).slot),
                    "HIGH-priority armor selections should use stable head, legs, feet ordering");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(options.get(3).slot),
                    "NORMAL-priority Swingcast Staff selection should follow HIGH-priority armor selections");
            helper.assertTrue(ItemStack.isSameItemSameComponents(
                            SpellSelectionStackResolver.resolveSelectionStack(player, options.get(0).slot), head),
                    "Scrollwoven head selection should resolve to the equipped armor stack");
            helper.assertTrue(options.subList(0, 4).stream().allMatch(option -> option.spellData.getSpell() == spell),
                    "Duplicate event-provided spells should remain independent by equipment source");
        });
    }

    private static void assertSpellSelectionPriority(
            GameTestHelper helper,
            Class<?> eventClass,
            EventPriority expected
    ) {
        try {
            var method = eventClass.getDeclaredMethod(
                    "onSpellSelection",
                    SpellSelectionManager.SpellSelectionEvent.class
            );
            var annotation = method.getAnnotation(SubscribeEvent.class);
            helper.assertTrue(annotation != null && annotation.priority() == expected,
                    eventClass.getSimpleName() + " should use " + expected + " priority");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to inspect spell selection event priority", exception);
        }
    }

    private static double modifierTotal(
            net.minecraft.world.item.component.ItemAttributeModifiers modifiers,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        return modifiers.modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute)
                        && entry.modifier().operation() == operation)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }

    private static void assertOptionalAttributeModifier(
            GameTestHelper helper,
            net.minecraft.world.item.component.ItemAttributeModifiers modifiers,
            net.minecraft.resources.ResourceLocation attributeId,
            double expectedAmount,
            AttributeModifier.Operation operation
    ) {
        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeId).orElseThrow();
        helper.assertTrue(Math.abs(modifierTotal(
                        modifiers,
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                        operation
                ) - expectedAmount) < 1.0e-9D,
                "Optional calibration attribute should match its fixed value: " + attributeId);
    }

    static void declaredCalibrationAdjustmentTargetsProvideMatchingTooltips(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var stack : createDeclaredCalibrationAdjustmentTargets()) {
                var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                helper.assertTrue(stack.getItem() instanceof SpellCalibrationAdjustmentTarget,
                        "Declared calibration tooltip target should implement the common target contract: " + itemId);
                var target = (SpellCalibrationAdjustmentTarget) stack.getItem();
                var optionalPayload = stack.getItem().getTooltipImage(stack);
                helper.assertTrue(optionalPayload.isPresent(),
                        "Declared calibration target should provide an image tooltip: " + itemId);
                var payload = optionalPayload.orElseThrow();
                helper.assertTrue(payload instanceof CalibrationAdjustmentTooltip,
                        "Declared calibration target should provide calibration tooltip data: " + itemId);
                var items = ((CalibrationAdjustmentTooltip) payload).items();
                helper.assertTrue(items.size() == target.getCalibrationAdjustmentSlotCount(stack),
                        "Calibration tooltip slot count should match the target contract: " + itemId);
                helper.assertTrue(items.stream().allMatch(ItemStack::isEmpty),
                        "New calibration target tooltip slots should all be empty: " + itemId);
            }
        });
    }

    static void scrollcasterGauntletTooltipExcludesCalibrationScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lookupProvider = helper.getLevel().registryAccess();
            var enchantments = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT);
            var storedBook = createEnchantedBook(
                    enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),
                    1
            );
            storedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Tooltip component book"));
            var expectedBook = storedBook.copyWithCount(1);
            var slotUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
            );
            var target = (SpellCalibrationAdjustmentTarget) gauntlet.getItem();
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    storedBook,
                    lookupProvider
            );
            helper.assertTrue(target.trySetCalibrationAdjustment(gauntlet, 2, slotUpgrade, lookupProvider),
                    "Tooltip test should store a slot upgrade without filling the middle adjustment slot");

            ScrollcasterGauntlet.setCalibrationScroll(
                    gauntlet,
                    0,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    lookupProvider
            );
            var populatedPayload = gauntlet.getItem().getTooltipImage(gauntlet).orElseThrow();
            helper.assertTrue(populatedPayload instanceof CalibrationAdjustmentTooltip,
                    "Populated Scrollcaster Gauntlet should keep calibration adjustment tooltip data");
            var populatedItems = ((CalibrationAdjustmentTooltip) populatedPayload).items();
            helper.assertTrue(ItemStack.isSameItemSameComponents(populatedItems.get(0), expectedBook),
                    "Calibration tooltip should retain adjustment item components");
            helper.assertTrue(populatedItems.get(1).isEmpty(),
                    "Calibration tooltip should preserve an empty middle adjustment slot");
            helper.assertTrue(populatedItems.get(2).is(slotUpgrade.getItem()),
                    "Calibration tooltip should preserve adjustment slot order");
            helper.assertTrue(populatedItems.stream()
                            .noneMatch(stack -> stack.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll),
                    "Calibration tooltip should not include Scrollcaster Gauntlet scroll slots");
        });
    }

    private static ItemStack[] createDeclaredCalibrationAdjustmentTargets() {
        return new ItemStack[]{
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
                new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()),
                new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get()),
                new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()),
                new ItemStack(ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get()),
                new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()),
                new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()),
                new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()),
                new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()),
                new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()),
                new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get())
        };
    }

    private static void assertCalibrationEffectKeys(
            GameTestHelper helper,
            CalibrationAdjustmentRule rule,
            String... expectedSuffixes
    ) {
        var effectLines = rule.effectLines();
        helper.assertTrue(effectLines.size() == expectedSuffixes.length,
                "Calibration effect should expose the expected line count: " + rule.displayId());
        for (var index = 0; index < expectedSuffixes.length; ++index) {
            helper.assertTrue(
                    effectLines.get(index).getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals(
                            "jei.apprenticecodex.spell_calibration_bench.effect." + expectedSuffixes[index]
                    ),
                    "Calibration effect should use the expected translation key: "
                            + rule.displayId() + "/" + expectedSuffixes[index]
            );
        }
    }

    static void legacyCalibrationAdjustmentFormatsMigrateOnFirstMutation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lookupProvider = helper.getLevel().registryAccess();
            var fullStackTargets = new ItemStack[]{
                    new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get())
            };
            var fullStackAdjustments = new ItemStack[]{
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            };
            for (var index = 0; index < fullStackTargets.length; ++index) {
                putLegacyFullStackList(
                        fullStackTargets[index],
                        "SpellCalibration",
                        fullStackAdjustments[index],
                        lookupProvider,
                        index == 0
                );
                assertLegacyMigration(
                        helper,
                        fullStackTargets[index],
                        fullStackAdjustments[index],
                        "SpellCalibration",
                        "Adjustments",
                        index == 0,
                        "full stack list " + index
                );
            }

            var chargecast = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
            var chargecastAdjustment = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            putLegacyFullStackList(
                    chargecast,
                    "ChargecastCalibration",
                    chargecastAdjustment,
                    lookupProvider,
                    false
            );
            assertLegacyMigration(
                    helper,
                    chargecast,
                    chargecastAdjustment,
                    "ChargecastCalibration",
                    "Adjustments",
                    false,
                    "chargecast full stack list"
            );

            var amulets = new ItemStack[]{
                    new ItemStack(ItemRegistry.AUTOCAST_AMULET.get()),
                    new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
            };
            var amuletAdjustments = new ItemStack[]{
                    new ItemStack(ItemRegistry.WISDOM_SHARD.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            };
            for (var index = 0; index < amulets.length; ++index) {
                putLegacyNestedIdList(amulets[index], "SpellCalibration", amuletAdjustments[index]);
                assertLegacyMigration(
                        helper,
                        amulets[index],
                        amuletAdjustments[index],
                        "SpellCalibration",
                        "Adjustments",
                        false,
                        "simplified amulet list " + index
                );
            }

            var shields = new ItemStack[]{
                    new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get()),
                    new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get()),
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get())
            };
            var shieldRoots = new String[]{
                    "BulwarkGreatshieldCalibration",
                    "ParrycastBucklerCalibration",
                    "ReflectcastShieldCalibration"
            };
            var shieldAdjustments = new ItemStack[]{
                    new ItemStack(ItemRegistry.WISDOM_SHARD.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            };
            for (var index = 0; index < shields.length; ++index) {
                putLegacyIdList(shields[index], shieldRoots[index], shieldAdjustments[index]);
                assertLegacyMigration(
                        helper,
                        shields[index],
                        shieldAdjustments[index],
                        shieldRoots[index],
                        "Adjustments",
                        false,
                        "shield ID list " + index
                );
            }

            var suitItems = new ItemStack[]{
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get())
            };
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            for (var index = 0; index < suitItems.length; ++index) {
                putLegacySingleId(suitItems[index], "MagiAgentSuitCalibration", "AdjustmentItem", fireRune);
                assertLegacyMigration(
                        helper,
                        suitItems[index],
                        fireRune,
                        "MagiAgentSuitCalibration",
                        "AdjustmentItem",
                        false,
                        "Magi Agent Suit single ID " + index
                );
            }

            var spellGuns = new ItemStack[]{
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get())
            };
            var amplifier = new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get());
            for (var index = 0; index < spellGuns.length; ++index) {
                putLegacySpellGun(spellGuns[index], amplifier, lookupProvider, false);
                assertLegacyMigration(
                        helper,
                        spellGuns[index],
                        amplifier,
                        "SpellgunCalibration",
                        "Adjustment",
                        false,
                        "spellgun full stack " + index
                );
            }

            var idOnlySpellGun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            putLegacySpellGun(idOnlySpellGun, amplifier, lookupProvider, true);
            assertLegacyMigration(
                    helper,
                    idOnlySpellGun,
                    amplifier,
                    "SpellgunCalibration",
                    "AdjustmentItem",
                    false,
                    "spellgun ID-only fallback"
            );
        });
    }

    private static void assertLegacyMigration(
            GameTestHelper helper,
            ItemStack targetStack,
            ItemStack expectedAdjustment,
            String legacyRootName,
            String legacyFieldName,
            boolean expectPreservedSibling,
            String context
    ) {
        var lookupProvider = helper.getLevel().registryAccess();
        var target = (SpellCalibrationAdjustmentTarget) targetStack.getItem();
        var legacySnapshot = getCustomDataTag(targetStack);
        var legacyRead = target.getCalibrationAdjustment(targetStack, 0, lookupProvider);
        helper.assertTrue(legacyRead.is(expectedAdjustment.getItem()),
                "Legacy adjustment should remain readable: " + context);
        helper.assertTrue(legacySnapshot != null && legacySnapshot.equals(getCustomDataTag(targetStack)),
                "Legacy reads should be side-effect free: " + context);
        helper.assertTrue(target.trySetCalibrationAdjustment(
                        targetStack,
                        0,
                        expectedAdjustment,
                        lookupProvider
                ),
                "First legacy mutation should succeed: " + context);

        var migratedRead = target.getCalibrationAdjustment(targetStack, 0, lookupProvider);
        helper.assertTrue(ItemStack.isSameItemSameComponents(expectedAdjustment.copyWithCount(1), migratedRead),
                "First legacy mutation should write the common full ItemStack format: " + context);
        var migratedRoot = getCustomDataTag(targetStack);
        helper.assertTrue(migratedRoot != null
                        && migratedRoot.contains("ApprenticeCodexCalibrationAdjustments", Tag.TAG_COMPOUND),
                "First legacy mutation should create common storage: " + context);
        if (migratedRoot.contains(legacyRootName, Tag.TAG_COMPOUND)) {
            helper.assertFalse(migratedRoot.getCompound(legacyRootName).contains(legacyFieldName),
                    "Migration should remove only the legacy adjustment field: " + context);
        }
        if (expectPreservedSibling) {
            helper.assertTrue(migratedRoot.getCompound(legacyRootName).getInt("PreservedSibling") == 1,
                    "Migration should preserve unrelated legacy-root data: " + context);
        }
    }

    private static void putLegacyFullStackList(
            ItemStack targetStack,
            String rootName,
            ItemStack adjustment,
            net.minecraft.core.HolderLookup.Provider lookupProvider,
            boolean addPreservedSibling
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            if (addPreservedSibling) {
                legacyRoot.putInt("PreservedSibling", 1);
            }
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.put("Item", adjustment.copyWithCount(1).saveOptional(lookupProvider));
            var adjustments = new ListTag();
            adjustments.add(entry);
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacyNestedIdList(ItemStack targetStack, String rootName, ItemStack adjustment) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var storedItem = new CompoundTag();
            storedItem.putString("ItemId", BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.put("Item", storedItem);
            var adjustments = new ListTag();
            adjustments.add(entry);
            var legacyRoot = new CompoundTag();
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacyIdList(ItemStack targetStack, String rootName, ItemStack adjustment) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            var adjustments = new ListTag();
            adjustments.add(entry);
            var legacyRoot = new CompoundTag();
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacySingleId(
            ItemStack targetStack,
            String rootName,
            String fieldName,
            ItemStack adjustment
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            legacyRoot.putString(fieldName, BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacySpellGun(
            ItemStack targetStack,
            ItemStack adjustment,
            net.minecraft.core.HolderLookup.Provider lookupProvider,
            boolean idOnly
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            if (idOnly) {
                legacyRoot.putString(
                        "AdjustmentItem",
                        BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString()
                );
            } else {
                legacyRoot.put("Adjustment", adjustment.copyWithCount(1).saveOptional(lookupProvider));
            }
            root.put("SpellgunCalibration", legacyRoot);
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
            helper.assertTrue(ItemStack.isSameItemSameComponents(selectedStack, gauntlet),
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
            new ArchivistsGrimoire.ScrollInventory(grimoire, helper.getLevel().registryAccess())
                    .setStackInSlot(0, createSpellScroll(SpellRegistry.BOUND_BOW.get()));
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
            NeoForge.EVENT_BUS.post(retainedRecastCooldownEvent);
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
            NeoForge.EVENT_BUS.post(staleTimeoutCooldownEvent);
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
                NeoForge.EVENT_BUS.post(cooldownEvent);
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
                NeoForge.EVENT_BUS.post(cooldownEvent);
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
                NeoForge.EVENT_BUS.post(cooldownEvent);
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
