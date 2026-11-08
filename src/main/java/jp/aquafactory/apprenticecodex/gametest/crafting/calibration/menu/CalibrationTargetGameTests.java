package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.crafting.support.WorkbenchMenuScope;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.assertStackHasSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenuWithTarget;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CalibrationTargetGameTests {
    private CalibrationTargetGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void amuletMenuReflectsStoredScrollsAndSilverRingWarnings(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
                var emptyAmulet = new ItemStack(autocastAmulet);
                autocastAmulet.initializeSpellContainer(emptyAmulet);

                var emptyAmuletMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet), player);
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
                var imbuedAmuletMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, imbuedAmulet), player);
                helper.assertTrue(imbuedAmuletMenu.getScrollItem(0)
                                .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                        "Imbued Autocast Amulet should expose a removable scroll");
                var longAmulet = new ItemStack(autocastAmulet);
                autocastAmulet.initializeSpellContainer(longAmulet);
                var longAmuletMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, longAmulet), player);
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
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void unsupportedArmorShowsHintsWithoutAllowingExtraction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var unsupportedArmor = createInitializedPresetStack(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
                var unsupportedArmorMenu = menus.track(new SpellCalibrationBenchMenu(0, player.getInventory()), player);
                helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                                .mayPlace(unsupportedArmor),
                        "Apprentice Mage Torso should be accepted by Spell Calibration Bench because it shows Can be Imbued");

                var imbuedUnsupportedArmor = createInitializedPresetStack(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
                setSingleUnlockedSpell(helper, imbuedUnsupportedArmor,
                        io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
                helper.assertTrue(unsupportedArmorMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                                .mayPlace(imbuedUnsupportedArmor),
                        "Imbued Apprentice Mage Torso should be accepted by Spell Calibration Bench because it shows Can be Imbued");
                var imbuedUnsupportedArmorMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, imbuedUnsupportedArmor), player);
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
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void adjustmentOnlyEquipmentRejectsScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
                var healScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
                var suitHoodMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get())), player);
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
                var mithrilFreecastStaffMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, mithrilFreecastStaff), player);
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
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void shiftClickAndChestImbueReachTheirStoredTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
                var healScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
                var gauntletWithFreecastAdjustmentMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(
                        player,
                        new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get())
                ), player);
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
                var suitCoatMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, suitCoat), player);
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

                var chromaticCoat = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
                var chromaticCoatMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, chromaticCoat), player);
                helper.assertTrue(chromaticCoatMenu.getEnabledScrollSlotCount() == 1,
                        "Chromatic Magia Dress coat should expose one scroll slot");
                helper.assertTrue(chromaticCoatMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(healScroll),
                        "Chromatic Magia Dress coat should accept a scroll in the Spell Calibration Bench slot");
                chromaticCoatMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(healScroll.copy());
                assertStackHasSpell(helper, chromaticCoat, io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(), 1,
                        "Chromatic Magia Dress coat should accept scroll imbue at the Spell Calibration Bench");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void presetSpellTargetsRemainReplaceableWithoutExposingLockedScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var presetStaffMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(
                        player,
                        createInitializedPresetStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get())
                ), player);
                helper.assertTrue(presetStaffMenu.getScrollItem(0).isEmpty(),
                        "Copper Swingcast Staff preset spell should not expose a removable scroll");
                helper.assertTrue(presetStaffMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                                .mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())),
                        "Copper Swingcast Staff preset slot should accept a replacement scroll");
                var uninitializedPresetStaff = new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
                ISpellContainer.remove(uninitializedPresetStaff);
                helper.assertFalse(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                        "Prepared Copper Swingcast Staff test stack should not have spell_container");
                helper.assertTrue(presetStaffMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                                .mayPlace(uninitializedPresetStaff),
                        "Uninitialized preset spell containers should be accepted by Spell Calibration Bench");
                menus.track(createSpellCalibrationBenchMenuWithTarget(player, uninitializedPresetStaff), player);
                helper.assertTrue(ISpellContainer.isSpellContainer(uninitializedPresetStaff),
                        "Spell Calibration Bench should initialize accepted preset spell containers");

                var alchemistsFlask = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
                var defaultAlchemistsFlask = new ItemStack(alchemistsFlask);
                alchemistsFlask.initializeSpellContainer(defaultAlchemistsFlask);
                var alchemistsFlaskMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, defaultAlchemistsFlask), player);
                helper.assertTrue(alchemistsFlaskMenu.getScrollItem(0).isEmpty(),
                        "Alchemist's Flask preset Extract should not expose a removable scroll");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void satelliteMenuUpdatesWarningsAndSlotCapacity(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "calibration_menu");
                var satelliteFollowcastMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(
                        player,
                        new ItemStack(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
                ), player);
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
                var fourSlotSatelliteMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, fourSlotSatellite), player);
                helper.assertTrue(fourSlotSatelliteMenu.getEnabledScrollSlotCount() == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                        "Satellite Followcast Amulet should expose four scroll slots after three slot upgrades");

                var smashcastMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(
                        player,
                        new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get())
                ), player);
                helper.assertFalse(smashcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                        "Smashcast Scepter should expose Calibration Bench spell restriction tooltip lines");
                helper.assertFalse(smashcastMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                                .mayPlace(createSpellScroll(SpellRegistry.MANA_CHARGE.get())),
                        "Smashcast Scepter should reject CONTINUOUS scrolls in the Spell Calibration Bench");
            }
        });
    }
}
