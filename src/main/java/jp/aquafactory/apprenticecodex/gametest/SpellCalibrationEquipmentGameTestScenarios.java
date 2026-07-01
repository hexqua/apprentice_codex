package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

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
            helper.assertFalse(emptyAmuletMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Autocast Amulet should expose Calibration Bench spell restriction tooltip lines");
            helper.assertTrue(emptyAmuletMenu.getScrollItem(0).isEmpty(),
                    "Empty Autocast Amulet should not expose a scroll");

            var imbuedAmulet = autocastAmulet.createArcaneAnvilImbueResult(
                    new ItemStack(autocastAmulet),
                    new SpellData(SpellRegistry.SENSE_EVIL.get(), 1)
            );
            var imbuedAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedAmulet);
            helper.assertTrue(imbuedAmuletMenu.getScrollItem(0)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Imbued Autocast Amulet should expose a removable scroll");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var emptyBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(emptyBlade);
            var bladeMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(emptyBlade),
                    "Mana Force Blade should be accepted by Spell Calibration Bench because it shows Can be Imbued");

            var imbuedBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(imbuedBlade);
            setSingleUnlockedSpell(helper, imbuedBlade,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(imbuedBlade),
                    "Imbued Mana Force Blade should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            var imbuedBladeMenu = createSpellCalibrationBenchMenuWithTarget(player, imbuedBlade);
            helper.assertFalse(imbuedBladeMenu.hasOperationalImbueTarget(),
                    "Mana Force Blade should still be unsupported by Calibration Bench operations");
            helper.assertTrue(imbuedBladeMenu.hasTargetSpellAt(0),
                    "Imbued Mana Force Blade spell should be visible for unsupported slot hints");
            helper.assertTrue(imbuedBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).getItem().isEmpty(),
                    "Unsupported Calibration Bench targets should not expose a real removable scroll");
            helper.assertTrue(imbuedBladeMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1).isEmpty(),
                    "Unsupported Calibration Bench targets should not allow scroll extraction");

            var emptyEnchantressRobe = new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get());
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should be accepted by Spell Calibration Bench because it shows Can be Imbued");
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(emptyEnchantressRobe),
                    "Enchantress Robe chestplate should remain unsupported by Calibration Bench operations");

            helper.assertFalse(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get())),
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
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem
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
            helper.assertTrue(bladeMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(uninitializedPresetStaff),
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
            helper.assertFalse(satelliteFollowcastMenu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Satellite Followcast Amulet should expose Calibration Bench spell restriction tooltip lines");

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
    static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_imbue_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
            var senseEvil = SpellRegistry.SENSE_EVIL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();

            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);
            var emptyAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, emptyAmulet);
            emptyAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, emptyAmulet, heal, 1,
                    "Calibration-imbued Autocast Amulet should contain heal");

            var twoSlotAmulet = autocastAmulet.createSpellSlotUpgradeResult(new ItemStack(autocastAmulet), upgradeItem);
            twoSlotAmulet = autocastAmulet.createArcaneAnvilImbueResult(twoSlotAmulet, new SpellData(senseEvil, 1));
            var twoSlotAmuletMenu = createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 1).set(createSpellScroll(heal));
            var twoSlotContainer = ISpellContainer.get(twoSlotAmulet);
            helper.assertTrue(twoSlotContainer != null && twoSlotContainer.getActiveSpellCount() == 2,
                    "Calibration imbue should add a second Autocast Amulet spell");
            assertStackHasSpell(helper, twoSlotAmulet, senseEvil, 1,
                    "Calibration imbue should keep the existing Autocast Amulet spell");
            assertStackHasSpell(helper, twoSlotAmulet, heal, 1,
                    "Calibration imbue should add heal to the empty Autocast Amulet slot");

            var removedScroll = twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
            helper.assertTrue(removedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should return a scroll when removing an Autocast Amulet spell");
            twoSlotAmuletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).onTake(player, removedScroll);
            var afterRemovalContainer = ISpellContainer.get(twoSlotAmulet);
            helper.assertTrue(afterRemovalContainer != null
                            && afterRemovalContainer.getSpellAtIndex(0) == SpellData.EMPTY
                            && afterRemovalContainer.getSpellAtIndex(1) != SpellData.EMPTY,
                    "Calibration Bench should not compact spell slots while removing a scroll");
            createSpellCalibrationBenchMenuWithTarget(player, twoSlotAmulet);
            var afterReinsertContainer = ISpellContainer.get(twoSlotAmulet);
            helper.assertTrue(afterReinsertContainer != null
                            && afterReinsertContainer.getSpellAtIndex(0) == SpellData.EMPTY
                            && afterReinsertContainer.getSpellAtIndex(1) != SpellData.EMPTY,
                    "Calibration Bench should preserve empty spell slots when opening an existing target");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var unsupportedMenu = new SpellCalibrationBenchMenu(0, player.getInventory());
            var manaForceBladeStack = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(manaForceBladeStack);
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(manaForceBladeStack),
                    "Calibration Bench should accept Can be Imbued targets for unsupported-operation hints");

            var externalSpellContainerStack = new ItemStack(Items.DIAMOND_SWORD);
            ISpellContainer.set(externalSpellContainerStack, ISpellContainer.create(1, false, false));
            helper.assertTrue(unsupportedMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).mayPlace(externalSpellContainerStack),
                    "Calibration Bench should accept items that show Iron's Can be Imbued tooltip");
            helper.assertFalse(SpellCalibrationImbueHelper.isSupportedTarget(externalSpellContainerStack),
                    "Generic external ISpellContainer items should remain unsupported by Calibration Bench operations");

            var magicMissileScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            helper.assertFalse(SpellCalibrationImbueHelper.canPlaceScrollAt(manaForceBladeStack, 0, magicMissileScroll),
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
                            .mayPlace(magicMissileScroll.copy()),
                    "Calibration Bench should not accept a spell rejected by the target item");

            var spellAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var spellAmplifierMenu = createSpellCalibrationBenchMenuWithTarget(player, spellAmplifier);
            spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, spellAmplifier, heal, 1,
                    "Calibration Bench should imbue generic extractable Spell Amplifiers");
            helper.assertTrue(spellAmplifierMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Calibration Bench should extract generic Spell Amplifier spells");

            var circlet = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var circletMenu = createSpellCalibrationBenchMenuWithTarget(player, circlet);
            circletMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).set(createSpellScroll(heal));
            assertStackHasSpell(helper, circlet, heal, 1,
                    "Calibration Bench should imbue tag-allowed extractable Curios");
        });
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

    static void mithrilFreecastStaffCooldownUsesSelectedSourceAndPolicy(GameTestHelper helper) {
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
            MithrilFreecastStaff.setCalibrationAdjustment(
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
            var selectedPolicyCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    magicMissile,
                    player,
                    CastSource.SWORD,
                    gauntlet
            );
            helper.assertTrue(selectedPolicyCooldown > normalSwordCooldown,
                    "Scrollcaster Gauntlet policy should visibly remove the SWORD cooldown multiplier: "
                            + selectedPolicyCooldown + " / sword " + normalSwordCooldown);
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
                    selection.getCastSource(),
                    selectedStack
            )) {
                var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                        normalSwordCooldown,
                        magicMissile,
                        player,
                        CastSource.SWORD
                );
                MithrilFreecastStaffCastEvent.onSpellCooldownAdded(cooldownEvent);
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == selectedPolicyCooldown,
                        "Mithril Freecast Staff should use the selected source policy cooldown but got "
                                + cooldownEvent.getEffectiveCooldown() + " / expected " + selectedPolicyCooldown);
            }

            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            magicData.setPlayerCastingItem(staff.copy());
            var spellbookBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
            );
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    heal,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
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
                    CastSource.SPELLBOOK,
                    grimoire
            );
            var actualBoundBowCooldown = magicData.getPlayerCooldowns()
                    .getSpellCooldowns()
                    .get(SpellRegistry.BOUND_BOW.get().getSpellId());
            helper.assertTrue(
                    actualBoundBowCooldown != null
                            && actualBoundBowCooldown.getCooldownRemaining() == expectedBoundBowCooldown,
                    "Mithril Freecast Staff should keep the selected SPELLBOOK source until Bound Bow recast cooldown but got "
                            + (actualBoundBowCooldown == null ? "none" : actualBoundBowCooldown.getCooldownRemaining())
                            + " / expected " + expectedBoundBowCooldown
            );

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var harvestMoon = SpellRegistry.HARVEST_MOON.get();
            magicData.setPlayerCastingItem(staff.copy());
            try (var ignored = MithrilFreecastStaffCastContext.open(
                    player.getUUID(),
                    staff,
                    harvestMoon,
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
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
                        CastSource.SPELLBOOK,
                        ItemStack.EMPTY
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
                    CastSource.SPELLBOOK,
                    ItemStack.EMPTY
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
                        CastSource.SPELLBOOK,
                        ItemStack.EMPTY
                );
                helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Mithril Freecast Staff should keep Thermal Process on the selected SPELLBOOK cooldown with Magi boots and CraftsmansDelight but got "
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
            helper.assertTrue(delayedCooldownEvent.getEffectiveCooldown() == selectedPolicyCooldown,
                    "Mithril Freecast Staff should keep the selected source policy until delayed cooldown but got "
                            + delayedCooldownEvent.getEffectiveCooldown() + " / expected " + selectedPolicyCooldown);
        });
    }
}
