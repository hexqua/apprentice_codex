package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletOffhandBridge;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.*;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletFreecastContext;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SPIRIT_PLUNDER;
import static jp.aquafactory.apprenticecodex.gametest.storage.stabilizer.StorageStabilizerNameTestSupport.assertStorageStabilizerDisplayName;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.describeModifiers;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.sumModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertSpellData;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenu;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellcasterWorkbenchMenuWithSingleInput;

final class ScrollcasterGauntletScenarios {
    private ScrollcasterGauntletScenarios() {}

    static void spellCalibrationBenchStoresScrollsOnGauntlet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_storage_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var firstScroll = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(menu.getSlot(0).mayPlace(gauntlet), "Scrollcaster Gauntlet should be accepted in the gauntlet slot");
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)), "Non-gauntlet items should be rejected from the gauntlet slot");
            helper.assertTrue(!menu.getSlot(1).mayPlace(lesserUpgrade), "Adjustment slots should be disabled without a gauntlet");
            helper.assertTrue(lesserUpgrade.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES),
                    "Lesser spell slot upgrade should be tagged as a Scrollcaster Gauntlet slot upgrade");

            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(menu.isScrollSlotEnabled(0), "Scroll slot 0 should be enabled by default");
            helper.assertTrue(menu.isScrollSlotEnabled(3), "Scroll slot 3 should be enabled by default");
            helper.assertTrue(!menu.isScrollSlotEnabled(4), "Scroll slot 4 should be locked before adding an upgrade");
            helper.assertTrue(!menu.getSlot(8).mayPlace(firstScroll.copy()), "Locked scroll slots should reject insertion");

            menu.getSlot(1).set(lesserUpgrade);
            helper.assertTrue(menu.isScrollSlotEnabled(5), "One lesser slot upgrade should unlock six scroll slots");
            helper.assertTrue(!menu.isScrollSlotEnabled(6), "One lesser slot upgrade should not unlock the seventh scroll slot");
            helper.assertTrue(menu.getSlot(9).mayPlace(firstScroll.copy()), "Newly unlocked scroll slot should accept scrolls");
            menu.getSlot(9).set(firstScroll);

            menu.getSlot(1).set(ItemStack.EMPTY);
            helper.assertTrue(!menu.isScrollSlotEnabled(5), "Removing an upgrade should disable its extra scroll slots");
            helper.assertTrue(menu.getSlot(9).hasItem(), "Disabled scroll slot should keep its existing scroll");
            helper.assertTrue(menu.getSlot(9).mayPickup(player), "Disabled scroll slot should still allow pickup");
            helper.assertTrue(!menu.getSlot(9).mayPlace(createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())),
                    "Disabled scroll slot should reject new scroll insertion");
            helper.assertTrue(!ScrollcasterGauntlet.getCalibrationScroll(gauntlet, 5).isEmpty(),
                    "Scroll should be stored on the gauntlet NBT");

            menu.removed(player);
            helper.assertTrue(player.getInventory().contains(gauntlet),
                    "Closing the Spell Calibration Bench should return the gauntlet to the player");
        });
    }

    static void scrollcasterGauntletCastModeWorkbenchToggle(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            gauntlet.setHoverName(Component.literal("Cast Mode Test"));
            var freecastAdjustment = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            gauntlet,
                            0,
                            freecastAdjustment.copy()
                    ),
                    "Scrollcaster Gauntlet cast mode test should install its state-preservation adjustment");

            helper.assertTrue(ScrollcasterGauntlet.getCastMode(gauntlet) == ScrollcasterGauntlet.CastMode.SPELL_WHEEL,
                    "Scrollcaster Gauntlet without mode data should default to spell-wheel selection");
            assertTooltipKeyAt(helper, gauntlet, 2, "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    "Spell-wheel mode should show the cast mode label");
            assertTooltipKeyArgumentUsesTranslationKey(
                    helper,
                    gauntlet,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    0,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.wheel",
                    "Spell-wheel mode label should explicitly identify spell-wheel selection"
            );
            assertTooltipKeyArgumentUsesColor(
                    helper,
                    gauntlet,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    0,
                    TextColor.fromLegacyFormat(ChatFormatting.AQUA),
                    "Spell-wheel mode label should use aqua"
            );
            assertTooltipKeyAt(helper, gauntlet, 3, "item.apprenticecodex.scrollcaster_gauntlet.desc.wheel",
                    "Spell-wheel mode should show its dedicated description");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_cast_mode_workbench_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var offhandStaff = new ItemStack(ItemRegistry.ZENITH_STAFF.get());
            var staffSpells = ISpellContainer.create(1, true, false).mutableCopy();
            staffSpells.addSpellAtIndex(heal, 1, 0, false);
            ISpellContainer.set(offhandStaff, staffSpells.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, offhandStaff);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet cast mode test could not resolve player magic data");
            magicData.setMana(1000.0F);
            magicData.getSyncedData().setSpellSelection(new SpellSelection(
                    SpellSelectionManager.OFFHAND,
                    0
            ));

            var wheelSpell = ScrollcasterGauntlet.resolveUseSpell(player, gauntlet);
            helper.assertTrue(wheelSpell != null && wheelSpell.spellData().getSpell() == heal,
                    "Default Scrollcaster Gauntlet use should resolve the spell-wheel selected spell");
            var resolvedWheelRightClick = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedWheelRightClick.isPresent()
                            && resolvedWheelRightClick.get().spellData().getSpell() == heal
                            && "scrollcaster_gauntlet_wheel_selection".equals(resolvedWheelRightClick.get().resolutionPath()),
                    "Right-click resolver should expose the Scrollcaster Gauntlet spell-wheel mode path");
            var wheelUseResult = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(wheelUseResult.getResult().consumesAction()
                            && magicData.getCastingSpellId().equals(heal.getSpellId()),
                    "Default Scrollcaster Gauntlet use should cast the spell-wheel selected spell");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(heal.getSpellId());

            var toggleMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, gauntlet.copy());
            var gauntletModePreview = toggleMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
            helper.assertTrue(ScrollcasterGauntlet.getCastMode(gauntletModePreview) == ScrollcasterGauntlet.CastMode.GAUNTLET,
                    "Spellcaster Workbench should preview gauntlet-selection mode from a default gauntlet");
            helper.assertTrue("Cast Mode Test".equals(gauntletModePreview.getHoverName().getString()),
                    "Spellcaster Workbench cast mode toggle should preserve the custom name");
            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntletModePreview) == 0
                            && ScrollcasterGauntlet.getSelectedSpellData(gauntletModePreview).getSpell() == magicMissile,
                    "Spellcaster Workbench cast mode toggle should preserve the selected scroll");
            helper.assertTrue(ItemStack.isSameItemSameTags(
                            SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(gauntletModePreview, 0),
                            freecastAdjustment
                    ),
                    "Spellcaster Workbench cast mode toggle should preserve calibration adjustments");

            var gauntletModeStack = toggleMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
            helper.assertFalse(gauntletModeStack.isEmpty(),
                    "Spellcaster Workbench should craft the toggled Scrollcaster Gauntlet");
            helper.assertTrue(toggleMenu.getSlot(0).getItem().isEmpty(),
                    "Spellcaster Workbench cast mode toggle should consume the single input gauntlet");
            assertTooltipKeyArgumentUsesTranslationKey(
                    helper,
                    gauntletModeStack,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    0,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.gauntlet",
                    "Gauntlet mode label should explicitly identify gauntlet selection"
            );
            assertTooltipKeyArgumentUsesColor(
                    helper,
                    gauntletModeStack,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    0,
                    TextColor.fromLegacyFormat(ChatFormatting.YELLOW),
                    "Gauntlet mode label should use yellow"
            );
            assertTooltipKeyAt(helper, gauntletModeStack, 3, "item.apprenticecodex.scrollcaster_gauntlet.desc.gauntlet",
                    "Gauntlet-selection mode should show its dedicated description");

            player.setItemInHand(InteractionHand.MAIN_HAND, gauntletModeStack);
            var gauntletSpell = ScrollcasterGauntlet.resolveUseSpell(player, gauntletModeStack);
            helper.assertTrue(gauntletSpell != null
                            && gauntletSpell.spellData().getSpell() == magicMissile
                            && gauntletSpell.castSource() == CastSource.SWORD,
                    "Gauntlet-selection mode should resolve the gauntlet-selected spell as a sword cast");
            var resolvedGauntletRightClick = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedGauntletRightClick.isPresent()
                            && resolvedGauntletRightClick.get().spellData().getSpell() == magicMissile
                            && "scrollcaster_gauntlet_selected".equals(resolvedGauntletRightClick.get().resolutionPath()),
                    "Right-click resolver should preserve the Scrollcaster Gauntlet selected-spell mode path");
            var gauntletUseResult = gauntletModeStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(gauntletUseResult.getResult().consumesAction()
                            && magicData.getCastingSpellId().equals(magicMissile.getSpellId()),
                    "Gauntlet-selection mode use should cast the gauntlet-selected spell");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());

            var reverseToggleMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, gauntletModeStack.copy());
            var wheelModePreview = reverseToggleMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
            helper.assertTrue(ScrollcasterGauntlet.getCastMode(wheelModePreview) == ScrollcasterGauntlet.CastMode.SPELL_WHEEL,
                    "Spellcaster Workbench should toggle gauntlet-selection mode back to spell-wheel mode");
        });
    }

    static void scrollcasterGauntletSelectedScrollDrivesImbuedSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            helper.assertTrue(gauntlet.getItem() instanceof UniqueItem,
                    "Scrollcaster Gauntlet should be a UniqueItem to block Arcane Anvil imbue tooltips and normal imbue");
            assertTooltipKeyAt(helper, gauntlet, 0, "item.apprenticecodex.right_click_magic_weapon.desc",
                    "Scrollcaster Gauntlet should show offhand priority tooltip first");
            assertTooltipKeyAt(helper, gauntlet, 1, "item.apprenticecodex.right_click_magic_weapon.item_type",
                    "Scrollcaster Gauntlet should show offhand priority item type tooltip second");
            assertTooltipKeyAt(helper, gauntlet, 2, "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    "Scrollcaster Gauntlet should show its cast mode tooltip third");
            assertTooltipKeyArgumentUsesColor(
                    helper,
                    gauntlet,
                    "item.apprenticecodex.scrollcaster_gauntlet.mode.label",
                    0,
                    TextColor.fromLegacyFormat(ChatFormatting.AQUA),
                    "Default Scrollcaster Gauntlet mode should be aqua"
            );
            assertTooltipKeyAt(helper, gauntlet, 3, "item.apprenticecodex.scrollcaster_gauntlet.desc.wheel",
                    "Scrollcaster Gauntlet should describe spell-wheel casting by default");
            ScrollcasterGauntlet.normalizeSelectedScrollIndex(gauntlet);
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet),
                    "Empty Scrollcaster Gauntlet should not expose a spell container");

            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 3, createSpellScroll(magicMissile));

            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == 3,
                    "First inserted scroll should become the selected gauntlet scroll");
            var selectionViews = ScrollcasterGauntlet.getSelectionViews(gauntlet);
            helper.assertTrue(selectionViews.size() == ScrollcasterGauntlet.BASE_CALIBRATION_SCROLL_SLOT_COUNT,
                    "Scrollcaster Gauntlet selection UI should expose every enabled slot");
            helper.assertTrue(!selectionViews.get(0).selectable() && selectionViews.get(3).selectable(),
                    "Scrollcaster Gauntlet selection UI should keep empty slots visible");
            helper.assertTrue(selectionViews.get(3).displayName().getString().endsWith(" 1"),
                    "Scrollcaster Gauntlet selection label should append the spell level number");
            helper.assertTrue(Objects.equals(
                            selectionViews.get(3).displayName().getStyle().getColor(),
                            magicMissile.getSchoolType().getDisplayName().getStyle().getColor()
                    ),
                    "Scrollcaster Gauntlet selection label should use the spell school color");
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet), "Gauntlet must not project a container");
            helper.assertTrue(ScrollcasterGauntlet.getSelectedSpellData(gauntlet).getSpell() == magicMissile,
                    "Selected Scrollcaster Gauntlet spell mismatch");
            helper.assertFalse(Utils.canImbue(gauntlet),
                    "Scrollcaster Gauntlet should not be treated as Arcane Anvil imbue equipment");
            helper.assertTrue(Utils.handleShriving(gauntlet).isEmpty(),
                    "Scrollcaster Gauntlet exposed spell should not be removable through Shriving Stone");
            ISpellContainer.createImbuedContainer(magicMissile, 1, gauntlet);
            helper.assertTrue(ISpellContainer.get(gauntlet).getSpellAtIndex(0).isLocked(),
                    "Legacy Scrollcaster Gauntlet projection setup should create a locked spell for this test");
            ScrollcasterGauntlet.discardLegacySpellContainer(gauntlet);
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet), "Legacy projection must be discarded");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 1, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 1);
            helper.assertTrue(ScrollcasterGauntlet.getSelectedSpellData(gauntlet).getSpell() == heal,
                    "Changing Scrollcaster Gauntlet index should change the exposed spell");
            gauntlet = ScrollcasterGauntlet.copyWithToggledCastMode(gauntlet);
            helper.assertTrue(ScrollcasterGauntlet.getCastMode(gauntlet) == ScrollcasterGauntlet.CastMode.GAUNTLET,
                    "Selected-scroll behavior test should explicitly use gauntlet-selection mode");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_right_click_resolver_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var resolvedRightClickSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedRightClickSpell.isPresent(),
                    "Scrollcaster Gauntlet right-click resolver should find the selected gauntlet spell");
            helper.assertTrue(resolvedRightClickSpell.get().spellData().getSpell() == heal,
                    "Scrollcaster Gauntlet right-click resolver should use the gauntlet-selected spell");
            helper.assertTrue("scrollcaster_gauntlet_selected".equals(resolvedRightClickSpell.get().resolutionPath()),
                    "Scrollcaster Gauntlet right-click resolver should expose its dedicated resolution path");
            var storageStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, storageStabilizer);
            var resolvedStorageStabilizerSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedStorageStabilizerSpell.isPresent(),
                    "Storage Stabilizer right-click resolver should find the selected stabilizer spell");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Storage Stabilizer right-click resolver should use the selected Personal Shelf spell");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().hand() == InteractionHand.MAIN_HAND,
                    "Storage Stabilizer right-click resolver should mark the main hand");
            helper.assertTrue("storage_stabilizer_selected".equals(resolvedStorageStabilizerSpell.get().resolutionPath()),
                    "Storage Stabilizer right-click resolver should expose its dedicated resolution path");
            helper.assertTrue(resolvedStorageStabilizerSpell.get().spellData().getSpell() instanceof IClientBlockTargetingSpell,
                    "Storage Stabilizer Personal Shelf should stay eligible for client block target sync");
            var storageStabilizerViews = StorageStabilizer.getSelectionViews(storageStabilizer);
            helper.assertTrue(storageStabilizerViews.size() == 3,
                    "Storage Stabilizer selection UI should expose all three storage spells");
            helper.assertTrue(storageStabilizerViews.get(2).selectionIndex() == 2
                            && storageStabilizerViews.get(2).selectable(),
                    "Storage Stabilizer should expose its third spell as a selectable common UI entry");
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 2);
            var companionTrunkContainer = ISpellContainer.get(storageStabilizer);
            helper.assertTrue(companionTrunkContainer != null,
                    "Storage Stabilizer Companion Trunk spell container is null");
            if (companionTrunkContainer != null) {
                assertSpellData(helper, companionTrunkContainer, 0, SpellRegistry.COMPANION_TRUNK.get(), 1, true,
                        "Storage Stabilizer should project Companion Trunk level 1 as a locked spell");
            }
            assertStorageStabilizerDisplayName(helper, storageStabilizer, SpellRegistry.COMPANION_TRUNK.get(),
                    "Storage Stabilizer default name should include Companion Trunk");
            storageStabilizer.setHoverName(Component.literal("Storage Test"));
            helper.assertTrue("Storage Test".equals(storageStabilizer.getHoverName().getString()),
                    "Storage Stabilizer custom name should hide its spell name");
            StorageStabilizer.setSelectedSpellIndex(storageStabilizer, 1);
            helper.assertTrue("Storage Test".equals(storageStabilizer.getHoverName().getString()),
                    "Storage Stabilizer custom name should remain after changing its spell selection");
            var invalidStorageStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(invalidStorageStabilizer, 99);
            helper.assertTrue(StorageStabilizer.getSelectedSpellIndex(invalidStorageStabilizer) == 0,
                    "Storage Stabilizer invalid selection should fall back to Summon Ender Chest");
            var companionTrunkStabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
            StorageStabilizer.setSelectedSpellIndex(companionTrunkStabilizer, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, companionTrunkStabilizer);
            var resolvedCompanionTrunkSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedCompanionTrunkSpell.isPresent()
                            && resolvedCompanionTrunkSpell.get().spellData().getSpell() == SpellRegistry.COMPANION_TRUNK.get(),
                    "Storage Stabilizer right-click resolver should use the selected Companion Trunk spell");
            var inertMainHand = new ItemStack(Items.STICK);
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, storageStabilizer);
            var resolvedOffhandStorageStabilizerSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.isPresent(),
                    "Storage Stabilizer right-click resolver should find the selected offhand stabilizer spell");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Storage Stabilizer right-click resolver should use the offhand selected Personal Shelf spell");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Storage Stabilizer right-click resolver should mark the offhand");
            helper.assertTrue(resolvedOffhandStorageStabilizerSpell.get().spellData().getSpell() instanceof IClientBlockTargetingSpell,
                    "Offhand Storage Stabilizer Personal Shelf should stay eligible for client block target sync");
            var emptyMainHandGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, emptyMainHandGauntlet);
            var resolvedOffhandStorageAfterEmptyGauntlet = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.isPresent(),
                    "Right-click resolver should find the offhand stabilizer spell after an empty mainhand gauntlet");
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the offhand stabilizer spell after an empty mainhand gauntlet");
            helper.assertTrue(resolvedOffhandStorageAfterEmptyGauntlet.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the offhand after an empty mainhand gauntlet");
            var emptyMainHandSpellGun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, emptyMainHandSpellGun);
            var resolvedMainHandSpellGunRightClick = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedMainHandSpellGunRightClick.isEmpty(),
                    "Mainhand Spellgun should not resolve a right-click spell before vanilla advances to the offhand");
            var resolvedOffhandStorageAfterEmptySpellGun = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.isPresent(),
                    "Right-click resolver should find the offhand stabilizer spell after an empty mainhand spell gun");
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the offhand stabilizer spell after an empty mainhand spell gun");
            helper.assertTrue(resolvedOffhandStorageAfterEmptySpellGun.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the offhand after an empty mainhand spell gun");
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
            var resolvedOffhandGauntletSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandGauntletSpell.isPresent(),
                    "Scrollcaster Gauntlet right-click resolver should find the selected offhand gauntlet spell");
            helper.assertTrue(resolvedOffhandGauntletSpell.get().spellData().getSpell() == heal,
                    "Scrollcaster Gauntlet right-click resolver should use the offhand gauntlet-selected spell");
            helper.assertTrue(resolvedOffhandGauntletSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Scrollcaster Gauntlet right-click resolver should mark the offhand");
            var offhandSpellGun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            ISpellContainer.createImbuedContainer(SpellRegistry.PERSONAL_SHELF.get(), 1, offhandSpellGun);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandSpellGun);
            var resolvedOffhandSpellGunSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandSpellGunSpell.isPresent(),
                    "Spellcaster Gun right-click resolver should find the offhand gun spell");
            helper.assertTrue(resolvedOffhandSpellGunSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Spellcaster Gun right-click resolver should use the offhand gun spell container");
            helper.assertTrue(resolvedOffhandSpellGunSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Spellcaster Gun right-click resolver should mark the offhand");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            var resolvedPriorityOffhandGunOverWeapon = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.isPresent(),
                    "Right-click resolver should find the priority offhand gun spell over a deferring magic weapon");
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the priority offhand gun spell over a deferring magic weapon");
            helper.assertTrue(resolvedPriorityOffhandGunOverWeapon.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the priority offhand gun over a deferring magic weapon");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var resolvedPriorityOffhandGunOverGauntlet = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.isPresent(),
                    "Right-click resolver should find the priority offhand gun spell over a deferring gauntlet");
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Right-click resolver should use the priority offhand gun spell over a deferring gauntlet");
            helper.assertTrue(resolvedPriorityOffhandGunOverGauntlet.get().hand() == InteractionHand.OFF_HAND,
                    "Right-click resolver should mark the priority offhand gun over a deferring gauntlet");
            player.setItemInHand(InteractionHand.MAIN_HAND, inertMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, createSpellScroll(SpellRegistry.PERSONAL_SHELF.get()));
            var resolvedOffhandScrollSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandScrollSpell.isPresent(),
                    "Scroll right-click resolver should find the offhand scroll spell");
            helper.assertTrue(resolvedOffhandScrollSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "Scroll right-click resolver should use the offhand scroll spell container");
            helper.assertTrue(resolvedOffhandScrollSpell.get().hand() == InteractionHand.OFF_HAND,
                    "Scroll right-click resolver should mark the offhand");
            var offhandStaff = new ItemStack(ItemRegistry.ZENITH_STAFF.get());
            var mutableStaffSpells = ISpellContainer.create(1, true, false).mutableCopy();
            mutableStaffSpells.addSpellAtIndex(SpellRegistry.PERSONAL_SHELF.get(), 1, 0, false);
            ISpellContainer.set(offhandStaff, mutableStaffSpells.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, offhandStaff);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet cooldown test could not resolve player mana data");
            magicData.getSyncedData().setSpellSelection(new SpellSelection(
                    SpellSelectionManager.OFFHAND,
                    0
            ));
            var resolvedOffhandCastingItemSpell = RightClickSpellResolver.resolve(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedOffhandCastingItemSpell.isPresent(),
                    "CastingItem right-click resolver should find the selected offhand spell");
            helper.assertTrue(resolvedOffhandCastingItemSpell.get().spellData().getSpell() == SpellRegistry.PERSONAL_SHELF.get(),
                    "CastingItem right-click resolver should use the offhand spell selection");
            helper.assertTrue(resolvedOffhandCastingItemSpell.get().hand() == InteractionHand.OFF_HAND,
                    "CastingItem right-click resolver should mark the offhand");
            player.setItemInHand(InteractionHand.MAIN_HAND, createSpellScroll(magicMissile));
            player.setItemInHand(InteractionHand.OFF_HAND, storageStabilizer);
            var resolvedMainHandPrioritySpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedMainHandPrioritySpell.isPresent(),
                    "Right-click resolver should still resolve the mainhand spell item first");
            helper.assertTrue(resolvedMainHandPrioritySpell.get().spellData().getSpell() == magicMissile,
                    "Right-click resolver should not prefer offhand spell sources over a mainhand spell item");
            helper.assertTrue(resolvedMainHandPrioritySpell.get().hand() == InteractionHand.MAIN_HAND,
                    "Right-click resolver should mark the prioritized mainhand spell");
            magicData.setPlayerCastingItem(gauntlet.copy());
            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Scrollcaster Gauntlet cooldown event should use the helper cooldown amount but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            magicData.setPlayerCastingItem(ItemStack.EMPTY);
            var controlEvent = new SpellCooldownAddedEvent.Pre(
                    160,
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == 160,
                    "Scrollcaster Gauntlet cooldown event should not affect non-gauntlet casts");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 1, ItemStack.EMPTY);
            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == 3,
                    "Removing the selected scroll should normalize to the first remaining scroll");
            helper.assertTrue(ScrollcasterGauntlet.getSelectedSpellData(gauntlet).getSpell() == magicMissile,
                    "Normalized Scrollcaster Gauntlet spell mismatch");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 3, ItemStack.EMPTY);
            helper.assertTrue(ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet) == -1,
                    "Removing every scroll should clear the selected gauntlet index");
            helper.assertFalse(ISpellContainer.isSpellContainer(gauntlet),
                    "Removing every scroll should clear the exposed gauntlet spell container");
        });
    }

    static void scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var baseViews = ScrollcasterGauntlet.getSelectionViews(gauntlet);
            helper.assertTrue(baseViews.size() == ScrollcasterGauntlet.BASE_CALIBRATION_SCROLL_SLOT_COUNT,
                    "Empty Scrollcaster Gauntlet selection UI should expose every base slot");
            helper.assertTrue(baseViews.stream().noneMatch(SneakSelectionView::selectable),
                    "Empty Scrollcaster Gauntlet selection UI should expose only empty slots");
            var emptyState = SneakSelectionState.open(
                    InteractionHand.MAIN_HAND,
                    baseViews,
                    ScrollcasterGauntlet.getSelectedScrollIndex(gauntlet)
            );
            helper.assertTrue(emptyState.selectableViewCount() == 0 && emptyState.move(1) == emptyState,
                    "Empty Scrollcaster Gauntlet selection UI should stay open without a movable selection");

            var expandedGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lesserUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
            );
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            expandedGauntlet,
                            0,
                            lesserUpgrade
                    ),
                    "Failed to add a slot upgrade to the empty Scrollcaster Gauntlet");

            var expandedViews = ScrollcasterGauntlet.getSelectionViews(expandedGauntlet);
            helper.assertTrue(expandedViews.size()
                            == ScrollcasterGauntlet.getEnabledCalibrationScrollSlotCount(expandedGauntlet),
                    "Empty Scrollcaster Gauntlet selection UI should track its enabled slot count");
            helper.assertTrue(expandedViews.size() > baseViews.size(),
                    "Slot upgrade should add empty slots to the Scrollcaster Gauntlet selection UI");
            helper.assertTrue(expandedViews.stream().noneMatch(SneakSelectionView::selectable),
                    "Expanded empty Scrollcaster Gauntlet selection UI should keep every slot empty");
        });
    }

    static void scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var gauntletItem = (ScrollcasterGauntlet) gauntlet.getItem();
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var fireBreath = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_freecast_swing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet freecast swing test could not resolve player magic data");
            magicData.setMana(1000.0F);

            helper.assertFalse(gauntletItem.canTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND),
                    "Scrollcaster Gauntlet should not be treated as swing-triggerable before freecast adjustment");
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet should not swing-cast without a Mithril Freecast Staff adjustment");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(gauntlet, 0, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()));
            helper.assertTrue(ScrollcasterGauntlet.hasFreecastStaffAdjustment(gauntlet),
                    "Scrollcaster Gauntlet should detect its Mithril Freecast Staff adjustment");
            helper.assertTrue(gauntletItem.canTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND),
                    "Scrollcaster Gauntlet should be treated as swing-triggerable after freecast adjustment");
            assertTooltipKeyAt(helper, gauntlet, 4, "item.apprenticecodex.freecast.common.desc",
                    "Freecast-adjusted Scrollcaster Gauntlet should show the generic freecast tooltip");

            helper.assertTrue(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet should swing-cast the selected instant spell with a Mithril Freecast Staff adjustment");
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                    "Scrollcaster Gauntlet freecast should cast with the gauntlet stack");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Scrollcaster Gauntlet freecast should mark the mainhand casting slot");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(fireBreath));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should reject continuous spells like Mithril Freecast Staff");

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 1.0F));
            helper.assertTrue(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should allow long spells with only a Mithril Freecast Staff adjustment");
            magicData.resetCastingState();
            magicData.getPlayerCooldowns().removeCooldown(heal.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            MagicHelper.MAGIC_MANAGER.addCooldown(player, magicMissile, CastSource.SWORD);
            helper.assertFalse(gauntletItem.tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Scrollcaster Gauntlet freecast should reject spells that are already on cooldown");
            magicData.getPlayerCooldowns().removeCooldown(magicMissile.getSpellId());

            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(heal));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            magicData.setPlayerCastingItem(gauntlet.copy());
            var baseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    heal,
                    player,
                    CastSource.SWORD
            );
            var normalEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                    heal,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(normalEvent);
            helper.assertTrue(normalEvent.getEffectiveCooldown() == baseCooldown,
                    "Normal Scrollcaster Gauntlet casts should keep the base sword cooldown");

            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, heal)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(heal, player, CastSource.SWORD),
                        heal,
                        player,
                        CastSource.SWORD
                );
                ScrollcasterGauntletCastEvent.onSpellCooldownAdded(freecastEvent);
                var expectedCooldown = baseCooldown + heal.getEffectiveCastTime(1, player);
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should extend long spell cooldown by cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(thermalProcess));
            magicData.setPlayerCastingItem(gauntlet.copy());
            var craftsmansBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    thermalProcess,
                    player,
                    CastSource.SWORD
            );
            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, thermalProcess)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(thermalProcess, player, CastSource.SWORD),
                        thermalProcess,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(freecastEvent);
                var expectedCooldown = gauntletItem.resolveFreecastSwingCooldownTicks(
                        player,
                        gauntlet,
                        thermalProcess,
                        craftsmansBaseCooldown
                );
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should keep CraftsmansDelight cooldown and long cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var artisanSmash = SpellRegistry.ARTISAN_SMASH.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(artisanSmash));
            magicData.setPlayerCastingItem(gauntlet.copy());
            var artisanBaseCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    artisanSmash,
                    player,
                    CastSource.SWORD
            );
            try (var ignored = ScrollcasterGauntletFreecastContext.open(player.getUUID(), gauntlet, artisanSmash)) {
                var freecastEvent = new SpellCooldownAddedEvent.Pre(
                        MagicManager.getEffectiveSpellCooldown(artisanSmash, player, CastSource.SWORD),
                        artisanSmash,
                        player,
                        CastSource.SWORD
                );
                MinecraftForge.EVENT_BUS.post(freecastEvent);
                var expectedCooldown = gauntletItem.resolveFreecastSwingCooldownTicks(
                        player,
                        gauntlet,
                        artisanSmash,
                        artisanBaseCooldown
                );
                helper.assertTrue(freecastEvent.getEffectiveCooldown() == expectedCooldown,
                        "Scrollcaster Gauntlet freecast should not re-reduce Magi boots cooldown after adding long cast time but got "
                                + freecastEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            }
        });
    }

    static void scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    gauntlet,
                    0,
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get())
            );

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_epicfight_mirror_swing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Scrollcaster Gauntlet Epic Fight mirror swing test could not resolve player magic data");
            magicData.setMana(1000.0F);

            helper.assertTrue(
                    EpicFightScrollcasterGauntletOffhandBridge
                            .shouldMirrorMainhand(player, InteractionHand.OFF_HAND),
                    "Epic Fight mirrored offhand state should resolve from a mainhand Gauntlet and empty offhand"
            );
            var resolvedHand = EpicFightSwingMagicCompat
                    .resolveSwingMagicTriggerHand(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedHand == InteractionHand.MAIN_HAND,
                    "Epic Fight mirrored offhand swing should use the mainhand Gauntlet but resolved " + resolvedHand);

            helper.assertTrue(
                    EpicFightSwingMagicCompat
                            .triggerSwingMagicFromAttackPhase(player, InteractionHand.OFF_HAND, -1, 0),
                    "Epic Fight mirrored offhand attack should trigger the mainhand Gauntlet Swingcast"
            );
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                    "Epic Fight mirrored Gauntlet Swingcast should cast with the mainhand Gauntlet stack");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Epic Fight mirrored Gauntlet Swingcast should mark the mainhand casting slot");
        });
    }

    static void scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var fallbackStaff = new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_epicfight_fallback_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
            player.setItemInHand(InteractionHand.OFF_HAND, fallbackStaff);

            var resolvedHand = EpicFightSwingMagicCompat
                    .resolveAvailableSwingMagicTriggerHand(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedHand == InteractionHand.OFF_HAND,
                    "Epic Fight timed swing trigger should fall back from an unadjusted mainhand Gauntlet to the offhand Swingcast item but resolved "
                            + resolvedHand);
        });
    }

    static void spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spell_calibration_freecast_adjustment_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var freecastStaff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());

            helper.assertFalse(menu.getSlot(1).mayPlace(freecastStaff),
                    "Mithril Freecast Staff adjustment should be rejected without a calibration target");
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(menu.getSlot(1).mayPlace(freecastStaff),
                    "Scrollcaster Gauntlet should accept a Mithril Freecast Staff adjustment");
            menu.getSlot(1).set(freecastStaff.copy());
            helper.assertTrue(ScrollcasterGauntlet.hasFreecastStaffAdjustment(gauntlet),
                    "Mithril Freecast Staff adjustment should be stored on the gauntlet");
            helper.assertFalse(menu.getSlot(2).mayPlace(freecastStaff),
                    "Scrollcaster Gauntlet should reject duplicate Mithril Freecast Staff adjustments");
            helper.assertFalse(menu.getSlot(2).mayPlace(silverRing),
                    "Scrollcaster Gauntlet should reject Silver Ring adjustments");

            menu.getSlot(1).set(ItemStack.EMPTY);
            menu.getSlot(0).set(staff);
            helper.assertFalse(menu.getSlot(1).mayPlace(freecastStaff),
                    "Revolvercast Staff should not accept a Mithril Freecast Staff adjustment");
            helper.assertTrue(menu.getSlot(1).mayPlace(silverRing),
                    "Revolvercast Staff should accept Silver Ring adjustments");
        });
    }

    static void scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var state = Blocks.STONE.defaultBlockState();
            var pos = new BlockPos(0, 1, 0);
            var survivalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_survival_block_attack_test");
            var creativePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_creative_block_attack_test");

            creativePlayer.gameMode.changeGameModeForPlayer(GameType.CREATIVE);

            helper.assertTrue(gauntlet.getItem().canAttackBlock(state, helper.getLevel(), pos, survivalPlayer),
                    "Scrollcaster Gauntlet should keep normal block attacks outside creative mode");
            helper.assertFalse(gauntlet.getItem().canAttackBlock(state, helper.getLevel(), pos, creativePlayer),
                    "Scrollcaster Gauntlet should block creative mode block attacks like vanilla swords");
        });
    }

    static void spellCalibrationBenchAdjustmentSlotsValidateInputs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_adjustment_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var iceRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get());
            var arcaneRuneItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_rune"));
            helper.assertTrue(arcaneRuneItem != null, "irons_spellbooks:arcane_rune is not registered");
            var arcaneRune = new ItemStack(arcaneRuneItem);
            var fireRuneId = ForgeRegistries.ITEMS.getKey(fireRune.getItem());
            var arcaneRuneId = ForgeRegistries.ITEMS.getKey(arcaneRune.getItem());
            helper.assertTrue(fireRuneId != null, "irons_spellbooks:fire_rune should have a registry id");
            helper.assertTrue(arcaneRuneId != null, "irons_spellbooks:arcane_rune should have a registry id");
            var enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            var lesserUpgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var recoveryRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());

            helper.assertTrue(lesserUpgrade.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES),
                    "Lesser spell slot upgrade should be tagged as a Scrollcaster Gauntlet slot upgrade");
            player.getInventory().setItem(9, recoveryRune.copy());
            var quickMovedRecoveryRune = menu.quickMoveStack(
                    player,
                    SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT
            );
            helper.assertTrue(quickMovedRecoveryRune.is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    "Recovery Rune shift-click without a target should fall back to normal inventory movement");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty()
                            && player.getInventory().getItem(0).is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()),
                    "Recovery Rune should move from main inventory to hotbar when no Calibration Bench target is present");
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(fireRune)
                            .filter(school -> SchoolRegistry.FIRE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Fire rune should resolve to the fire school without a whitelist entry"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(arcaneRune).isEmpty(),
                    "Non-school runes should not resolve to a scrollcaster school"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(arcaneRuneId, Map.of(arcaneRuneId, SchoolRegistry.FIRE_RESOURCE))
                            .filter(school -> SchoolRegistry.FIRE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Manual rune override should resolve a rune that automatic lookup cannot resolve"
            );
            helper.assertTrue(
                    ScrollcasterSchoolRuneResolver.resolveSchool(fireRuneId, Map.of(fireRuneId, SchoolRegistry.ICE_RESOURCE))
                            .filter(school -> SchoolRegistry.ICE_RESOURCE.equals(school.getId()))
                            .isPresent(),
                    "Manual rune override should take precedence over automatic rune lookup"
            );
            helper.assertTrue(menu.getSlot(1).mayPlace(fireRune), "School rune should be accepted as a calibration adjustment");
            helper.assertFalse(menu.getSlot(2).mayPlace(enchantedBook),
                    "Enchanted books should no longer be accepted as calibration adjustments");
            helper.assertTrue(menu.getSlot(3).mayPlace(lesserUpgrade), "Tagged slot upgrades should be accepted as a calibration adjustment");
            helper.assertTrue(!menu.getSlot(1).mayPlace(arcaneRune), "Arcane rune should not be treated as a scrollcaster school rune");

            menu.getSlot(1).set(fireRune);
            helper.assertTrue(!menu.getSlot(2).mayPlace(iceRune), "Only one school rune should be accepted at a time");
            helper.assertFalse(menu.getSlot(2).mayPlace(enchantedBook),
                    "Enchanted books should remain rejected after a rune is present");
            helper.assertTrue(!SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(gauntlet, 0).isEmpty(),
                    "Adjustment item should be stored on the gauntlet NBT");
        });
    }

    static void spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_calibration_school_power_test");
            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());

            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.10D, 0.0D, 0.0D,
                    "Uncalibrated Scrollcaster Gauntlet should keep its base spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet).isEmpty(),
                    "Uncalibrated Scrollcaster Gauntlet should not expose an inventory rune overlay");

            menu.getSlot(0).set(gauntlet);
            menu.getSlot(1).set(fireRune);
            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.05D, 0.15D, 0.0D,
                    "Fire rune should retain reduced base spell power and add fire spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet)
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                    "Fire-calibrated Scrollcaster Gauntlet should expose the Fire rune inventory overlay");
            var fireSchool = SchoolRegistry.getSchool(SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireSchool != null, "Fire school should be registered");
            assertTooltipKeyArgumentUsesColor(
                    helper,
                    gauntlet,
                    "item.apprenticecodex.scrollcaster_gauntlet.school_rune",
                    0,
                    fireSchool.getDisplayName().getStyle().getColor(),
                    "School rune tooltip should keep the resolved school color"
            );

            menu.getSlot(1).set(ItemStack.EMPTY);
            assertScrollcasterGauntletSpellPower(helper, gauntlet, 0.10D, 0.0D, 0.0D,
                    "Removing the school rune should restore base spell power");
            helper.assertTrue(ScrollcasterGauntlet.getInventoryOverlayIconStack(gauntlet).isEmpty(),
                    "Removing the school rune should remove the inventory rune overlay");
            assertTooltipKeyAbsent(helper, gauntlet, "item.apprenticecodex.scrollcaster_gauntlet.school_rune",
                    "Removing the school rune should remove the school rune tooltip");

            var staleGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(staleGauntlet, 0, fireRune);
            staleGauntlet.getOrCreateTagElement("SpellCalibration")
                    .putString("SchoolPowerSchool", SchoolRegistry.ICE_RESOURCE.toString());
            assertScrollcasterGauntletSpellPower(helper, staleGauntlet, 0.05D, 0.0D, 0.15D,
                    "Stale school power should reflect the stored school before bench refresh");

            var refreshMenu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(1, 1, 0));
            refreshMenu.getSlot(0).set(staleGauntlet);
            assertScrollcasterGauntletSpellPower(helper, staleGauntlet, 0.05D, 0.15D, 0.0D,
                    "Placing the gauntlet on the bench should refresh the stored rune school");
        });
    }

    static void scrollcasterGauntletUsesNormalEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var item = gauntlet.getItem();
            helper.assertTrue(item.isEnchantable(gauntlet),
                    "Scrollcaster Gauntlet should be enchantable");
            helper.assertTrue(item.getEnchantmentValue(gauntlet) == 22,
                    "Scrollcaster Gauntlet enchantability should be 22");

            for (var supported : List.of(
                    Enchantments.SHARPNESS,
                    Enchantments.BLOCK_FORTUNE,
                    Enchantments.SILK_TOUCH,
                    EnchantmentRegistry.WISDOM.get(),
                    EnchantmentRegistry.ALACRITY.get()
            )) {
                helper.assertTrue(item.canApplyAtEnchantingTable(gauntlet, supported),
                        "Scrollcaster Gauntlet enchanting table should support "
                                + ForgeRegistries.ENCHANTMENTS.getKey(supported));
                helper.assertTrue(item.isBookEnchantable(
                                gauntlet,
                                createEnchantedBook(new EnchantmentInstance(supported, 1))
                        ),
                        "Scrollcaster Gauntlet anvil should support "
                                + ForgeRegistries.ENCHANTMENTS.getKey(supported));
            }
            for (var rejected : List.of(
                    Enchantments.BLOCK_EFFICIENCY,
                    Enchantments.UNBREAKING,
                    Enchantments.MENDING,
                    EnchantmentRegistry.PLUNDER.get()
            )) {
                helper.assertFalse(item.canApplyAtEnchantingTable(gauntlet, rejected),
                        "Scrollcaster Gauntlet should reject " + ForgeRegistries.ENCHANTMENTS.getKey(rejected));
                helper.assertFalse(item.isBookEnchantable(gauntlet,
                                createEnchantedBook(new EnchantmentInstance(rejected, 1))),
                        "Scrollcaster Gauntlet enchanted book should reject " + ForgeRegistries.ENCHANTMENTS.getKey(rejected));
                helper.assertFalse(((NonDamageableAnvilMergeItem) item).isAnvilMergeEnchantmentAllowed(gauntlet, rejected),
                        "Scrollcaster Gauntlet anvil merge should reject " + ForgeRegistries.ENCHANTMENTS.getKey(rejected));
            }
            helper.assertTrue(item instanceof NonDamageableAnvilMergeItem,
                    "Scrollcaster Gauntlet should use the non-damageable anvil merge path");
            helper.assertTrue(((NonDamageableAnvilMergeItem) item)
                            .isAnvilMergeEnchantmentAllowed(gauntlet, Enchantments.SHARPNESS),
                    "Scrollcaster Gauntlet anvil merge should accept Sharpness");

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                for (var enchantmentId : List.of(MALUM_SPIRIT_PLUNDER, MALUM_HAUNTED)) {
                    var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                    helper.assertTrue(enchantment != null,
                            "Malum enchantment should be registered: " + enchantmentId);
                    helper.assertTrue(item.canApplyAtEnchantingTable(gauntlet, enchantment),
                            "Scrollcaster Gauntlet should support compatible Malum enchantment " + enchantmentId);
                }
                var animated = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_ANIMATED);
                helper.assertTrue(animated != null, "Malum Animated enchantment should be registered");
                helper.assertFalse(item.canApplyAtEnchantingTable(gauntlet, animated),
                        "Scrollcaster Gauntlet should reject Malum Animated on 1.20.1");
                helper.assertFalse(item.isBookEnchantable(
                                gauntlet,
                                createEnchantedBook(new EnchantmentInstance(animated, 1))
                        ),
                        "Scrollcaster Gauntlet anvil should reject Malum Animated on 1.20.1");
                helper.assertFalse(((NonDamageableAnvilMergeItem) item)
                                .isAnvilMergeEnchantmentAllowed(gauntlet, animated),
                        "Scrollcaster Gauntlet anvil merge should reject Malum Animated on 1.20.1");
            }
        });
    }

    static void scrollcasterGauntletKeepsLegacyEnchantmentsAndAllowsBookExtraction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_legacy_enchantment_test");
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var legacyBook = createEnchantedBook(new EnchantmentInstance(Enchantments.SHARPNESS, 2));
            gauntlet.enchant(Enchantments.SHARPNESS, 2);
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    legacyBook
            );

            var menu = createSpellCalibrationBenchMenu(helper, player, new BlockPos(0, 1, 0));
            menu.getSlot(0).set(gauntlet);
            helper.assertTrue(gauntlet.getEnchantmentLevel(Enchantments.SHARPNESS) == 2,
                    "Opening the Calibration Bench should preserve existing gauntlet enchantments");
            helper.assertTrue(menu.getSlot(1).mayPickup(player),
                    "Legacy enchanted books should remain extractable from disabled adjustment rules");
            var extractedBook = menu.getSlot(1).remove(1);
            helper.assertTrue(EnchantmentHelper.getEnchantments(extractedBook)
                            .getOrDefault(Enchantments.SHARPNESS, 0) == 2,
                    "Extracted legacy books should preserve their enchantment");
            helper.assertFalse(menu.getSlot(1).mayPlace(extractedBook),
                    "Extracted enchanted books should not be insertable again");
            helper.assertTrue(gauntlet.getEnchantmentLevel(Enchantments.SHARPNESS) == 2,
                    "Extracting a legacy book should not remove the gauntlet enchantment");
        });
    }

    static void scrollcasterGauntletGrindstoneRemovesEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_grindstone_test");
            var grindstonePos = new BlockPos(0, 1, 0);
            helper.setBlock(grindstonePos, Blocks.GRINDSTONE);
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            gauntlet.enchant(Enchantments.SHARPNESS, 1);
            var menu = new GrindstoneMenu(
                    0,
                    player.getInventory(),
                    ContainerLevelAccess.create(
                            helper.getLevel(),
                            helper.absolutePos(grindstonePos)
                    )
            );

            menu.getSlot(0).set(gauntlet);
            var output = menu.getSlot(2).getItem();
            helper.assertTrue(output.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    "Scrollcaster Gauntlet should expose a grindstone output");
            helper.assertTrue(output.getEnchantmentLevel(Enchantments.SHARPNESS) == 0,
                    "Scrollcaster Gauntlet grindstone output should remove Sharpness");
        });
    }

    static void assertScrollcasterGauntletSpellPower(
            GameTestHelper helper,
            ItemStack stack,
            double expectedGlobalSpellPower,
            double expectedFireSpellPower,
            double expectedIceSpellPower,
            String message
    ) {
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var globalSpellPower = sumModifierAmount(
                modifiers.get(AttributeRegistry.SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        var fireSpellPower = sumModifierAmount(
                modifiers.get(AttributeRegistry.FIRE_SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        var iceSpellPower = sumModifierAmount(
                modifiers.get(AttributeRegistry.ICE_SPELL_POWER.get()),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        helper.assertTrue(Math.abs(globalSpellPower - expectedGlobalSpellPower) < 1.0e-9D
                        && Math.abs(fireSpellPower - expectedFireSpellPower) < 1.0e-9D
                        && Math.abs(iceSpellPower - expectedIceSpellPower) < 1.0e-9D,
                message
                        + ": expected global/fire/ice="
                        + expectedGlobalSpellPower + "/" + expectedFireSpellPower + "/" + expectedIceSpellPower
                        + " but got "
                        + globalSpellPower + "/" + fireSpellPower + "/" + iceSpellPower
                        + " modifiers=" + describeModifiers(modifiers));
    }

    static void assertScrollcasterGauntletOffhandUseCasts(
            GameTestHelper helper,
            ItemStack mainHandStack,
            AbstractSpell spell,
            String profileName
    ) {
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Scrollcaster Gauntlet offhand use test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "Scrollcaster Gauntlet offhand use should cast through the offhand when main hand does not consume use but got "
                        + result.getResult());
        helper.assertTrue(magicData.isCasting(), "Scrollcaster Gauntlet offhand use should start casting");
        helper.assertTrue(SpellSelectionManager.OFFHAND.equals(magicData.getCastingEquipmentSlot()),
                "Scrollcaster Gauntlet offhand use should mark the offhand casting slot but got "
                        + magicData.getCastingEquipmentSlot());
        helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), gauntlet),
                "Scrollcaster Gauntlet offhand use should cast with the offhand gauntlet stack");
    }

    static void assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
            GameTestHelper helper,
            ItemStack mainHandStack,
            AbstractSpell gauntletSpell,
            String profileName
    ) {
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(gauntletSpell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Scrollcaster Gauntlet offhand defer test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(result.getResult() == InteractionResult.PASS,
                "Scrollcaster Gauntlet offhand use should defer when main hand is a spell item but got "
                        + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet offhand use should not start a second cast while main hand owns spell use");
        helper.assertTrue(magicData.getPlayerCastingItem().isEmpty(),
                "Scrollcaster Gauntlet offhand defer should not set a casting item");
    }
}
