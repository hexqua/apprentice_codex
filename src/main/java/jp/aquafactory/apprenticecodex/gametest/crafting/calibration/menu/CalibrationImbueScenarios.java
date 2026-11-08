package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu.CalibrationImbueGameTests.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.assertStackHasSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenuWithTarget;

final class CalibrationImbueScenarios {
    private CalibrationImbueScenarios() {}

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

            var spellReaperScythe = createInitializedPresetStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
            var spellReaperScytheMenu = createSpellCalibrationBenchMenuWithTarget(player, spellReaperScythe);
            spellReaperScytheMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                    .set(createSpellScroll(heal));
            assertStackHasSpell(helper, spellReaperScythe, heal, 1,
                    "Calibration Bench should imbue the tag-allowed Spell Reaper Scythe");
            var extractedScytheScroll = spellReaperScytheMenu
                    .getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(extractedScytheScroll.is(
                            io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should extract Spell Reaper Scythe spells");
            var spellReaperScytheContainer = ISpellContainer.get(spellReaperScythe);
            helper.assertTrue(spellReaperScytheContainer != null
                            && spellReaperScytheContainer.getMaxSpellCount() == 1
                            && spellReaperScytheContainer.isEmpty(),
                    "Calibration Bench should clear the Spell Reaper Scythe spell while preserving its slot");

            var circlet = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var circletMenu = createSpellCalibrationBenchMenuWithTarget(player, circlet);
            circletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, circlet, heal, 1,
                    "Calibration Bench should imbue tag-allowed extractable Curios");
        });
    }
}
