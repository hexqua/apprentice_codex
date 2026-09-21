package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellExtractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING;
import static jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BLAST;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RevolvercastStaffContainerGameTests {
    private RevolvercastStaffContainerGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void transcendenceBoostsInternalScrollsWithoutChangingAssets(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, spell.getMaxLevel()));
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        for (int level : new int[]{0, 1, 3, 10}) {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            if (level > 0) staff.enchant(enchantment, level);
            RevolvercastStaff.setCalibrationScroll(staff, 0, scroll.copy());
            RevolvercastStaff.setCalibrationScroll(staff, 8, scroll.copy());
            var before = staff.copy();
            int expected = spell.getMaxLevel() + (level > 0 ? 1 : 0);
            for (int repeat = 0; repeat < 2; ++repeat) {
                helper.assertTrue(RevolvercastStaff.getSelectedSpellData(staff).getLevel() == expected,
                        "Internal bonus must exceed the maximum once, regardless of enchantment level");
                var tooltip = RevolvercastStaff.getScrollTooltipData(staff);
                helper.assertTrue(tooltip.selectedSpell().getLevel() == expected
                                && tooltip.entries().getFirst().spell().getLevel() == expected
                                && tooltip.entries().getLast().spell().getLevel() == spell.getMaxLevel(),
                        "Tooltip must boost usable scrolls only");
            }
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, staff),
                    "Resolving levels must preserve scrolls and legacy enchantment levels");
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "revolver_bonus_" + level);
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(CuriosSlotConstants.HEAD, 0, ring);
            helper.assertTrue(new SpellSelectionManager(player).getSpellForSlot(SpellSelectionManager.MAINHAND, 0)
                            .getLevel() == expected,
                    "Wheel must expose the internally boosted level without applying Affinity twice");
            var magic = MagicData.getPlayerMagicData(player);
            magic.getSyncedData();
            magic.setMana(10000);
            helper.assertTrue(((RevolvercastStaff) staff.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.MAIN_HAND, true),
                    "Swing must start casting the internal scroll");
            helper.assertTrue(magic.getCastingSpellLevel() == expected + 2,
                    "Actual swing casting must stack Affinity once with the internal bonus");
            helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, RevolvercastStaff.getCalibrationScroll(staff, 0)),
                    "Casting must not write the bonus into the stored scroll");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void transcendenceDoesNotBoostExternalWheelSpell(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        staff.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE), 3);
        RevolvercastStaff.setCalibrationScroll(staff, 0,
                SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "revolver_external");
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var helmet = new ItemStack(Items.LEATHER_HELMET);
        ISpellContainer.createImbuedContainer(spell, 1, helmet);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        var magic = MagicData.getPlayerMagicData(player);
        magic.getSyncedData().getSpellSelection().makeSelection(EquipmentSlot.HEAD.getName(), 0);
        magic.setMana(10000);
        staff.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(magic.isCasting() && magic.getCastingSpellLevel() == 1,
                "Right click must not boost the same spell sourced from another item");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void tooltipIncludesInactiveScrollsWithoutChangingSelection(GameTestHelper helper) {
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        var instant = SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1));
        var longSpell = SpellCalibrationImbueHelper.createScroll(new SpellData(ARCANE_BLAST.get(), 1));
        RevolvercastStaff.setCalibrationScroll(staff, 0, instant);
        RevolvercastStaff.setCalibrationScroll(staff, 1, longSpell);
        RevolvercastStaff.setCalibrationScroll(staff, 8, instant);
        CustomData.update(DataComponents.CUSTOM_DATA, staff,
                root -> root.getCompound("SpellCalibration").putInt("SelectedScrollIndex", 8));
        var original = staff.copy();
        var tooltip = RevolvercastStaff.getScrollTooltipData(staff);
        helper.assertTrue(tooltip.entries().size() == 3 && tooltip.selectedSlot() == 0,
                "Tooltip must include disabled slots and resolve the displayed selection");
        helper.assertTrue(tooltip.entries().getFirst().usable()
                        && !tooltip.entries().get(1).usable() && !tooltip.entries().get(2).usable(),
                "Long spells without Silver Ring and disabled slots must be displayed as inactive");
        helper.assertTrue(ItemStack.isSameItemSameComponents(original, staff),
                "Tooltip queries must not normalize or otherwise change saved data");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyCleanupPreservesInternalAssets(GameTestHelper helper) {
        var spell = new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 2);
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolver_cleanup");
        for (int slot : new int[]{0, 8}) {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            RevolvercastStaff.setCalibrationScroll(staff, slot, SpellCalibrationImbueHelper.createScroll(spell));
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(staff, 0,
                    new ItemStack(SILVER_RING.get()));
            var original = staff.copy();
            setLegacy(staff, new SpellData(spell.getSpell(), 3));
            PresetSpellContainerStateHelper.rememberOverridden(staff, spell);
            helper.assertFalse(SpellExtractionHelper.evaluate(staff).isSuccess(), "Legacy revolver must reject destructive extraction");
            staff.getItem().inventoryTick(staff, helper.getLevel(), player, 0, false);
            helper.assertTrue(ItemStack.isSameItemSameComponents(original, staff),
                    "Cleanup must preserve internal scrolls, disabled slots, adjustments, and selection exactly");
            assertRepeatable(helper, staff);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyOnlySpellsAreDiscardedWithoutRestoration(GameTestHelper helper) {
        var spell = new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 3);
        for (int variant = 0; variant < 3; ++variant) {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            if (variant != 1) setLegacy(staff, spell);
            if (variant != 0) PresetSpellContainerStateHelper.rememberOverridden(staff, spell);
            discard(helper, staff);
            helper.assertTrue(countScrolls(staff) == 0, "Host and remembered spells must not become scrolls");
            helper.assertTrue(RevolvercastStaff.getSelectedSpellData(staff) == SpellData.EMPTY,
                    "Discarded host data must not become an active spell");
            var data = staff.get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data == null || data.copyTag().isEmpty(), "Cleanup must not leave remembered data or a backup");
            assertRepeatable(helper, staff);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void benchDiscardsProjectionWithoutChangingStoredScrolls(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolver_bench_cleanup");
        var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1));
        RevolvercastStaff.setCalibrationScroll(staff, 0, scroll);
        setLegacy(staff, new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 3));
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(staff);
        helper.assertFalse(ISpellContainer.isSpellContainer(staff), "Bench preparation must discard the host projection");
        var removed = menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).remove(1);
        helper.assertTrue(ItemStack.isSameItemSameComponents(removed, scroll), "Bench removal must return the original scroll");
        helper.assertTrue(countScrolls(staff) == 0, "Discarded projection must not recreate a removed scroll");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void duplicateWheelOptionsKeepIndependentSlotsAndLevels(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolver_wheel_duplicates");
        var first = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        var second = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        RevolvercastStaff.setCalibrationScroll(first, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
        RevolvercastStaff.setCalibrationScroll(first, 1, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 3)));
        RevolvercastStaff.setCalibrationScroll(second, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 2)));
        var helmet = new ItemStack(Items.LEATHER_HELMET);
        ISpellContainer.createImbuedContainer(spell, 4, helmet);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setItemInHand(InteractionHand.MAIN_HAND, first);
        player.setItemInHand(InteractionHand.OFF_HAND, second);
        MagicData.getPlayerMagicData(player).getSyncedData().getSpellSelection().makeSelection(SpellSelectionManager.MAINHAND, 0);
        var manager = new SpellSelectionManager(player);
        helper.assertTrue(manager.getSpellCount() == 3, "Each revolver must append its own duplicate spell");
        helper.assertTrue(manager.getSelection().spellData.getLevel() == 1, "Selection must retain the mainhand level");
        RevolvercastStaff.advanceToNextValidScrollIndex(first);
        manager = new SpellSelectionManager(player);
        helper.assertTrue(manager.getSpellCount() == 3 && manager.getSelection().slot.equals(SpellSelectionManager.MAINHAND)
                && manager.getSelection().spellData.getLevel() == 3, "Advancing must preserve candidate count and selected source");
        helper.assertTrue(manager.getSpellForSlot(SpellSelectionManager.OFFHAND, 0).getLevel() == 2, "Offhand level must not merge");
        var book = new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
        setLegacy(book, new SpellData(spell, 3));
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        ApprenticeCodexGameTestScenarios.equipCurio(player, Curios.SPELLBOOK_SLOT, book);
        MagicData.getPlayerMagicData(player).getSyncedData().getSpellSelection().makeSelection(Curios.SPELLBOOK_SLOT, 0);
        RevolvercastStaff.advanceToNextValidScrollIndex(first);
        manager = new SpellSelectionManager(player);
        helper.assertTrue(manager.getSpellCount() == 3 && manager.getSelection().slot.equals(Curios.SPELLBOOK_SLOT),
                "Book duplicates must remain independent and rotation must not steal selection");
        helper.succeed();
    }

    private static void setLegacy(ItemStack staff, SpellData spell) {
        var container = ISpellContainer.create(1, true, false).mutableCopy();
        container.addSpellAtIndex(spell.getSpell(), spell.getLevel(), 0, false);
        ISpellContainer.set(staff, container.toImmutable());
    }

    @GameTest(template = "gametest/basic_floor")
    public static void activeCastCancelsOnReplacementRemovalAndScrollChange(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "revolver_cast_invalidation");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var magic = MagicData.getPlayerMagicData(player);
        magic.getSyncedData();
        for (int variant = 0; variant < 3; ++variant) {
            var staff = new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get());
            RevolvercastStaff.setCalibrationScroll(staff, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
            RevolvercastStaff.setCalibrationScroll(staff, 1, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 2)));
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            magic.initiateCast(spell, 1, 40, CastSource.SWORD, SpellSelectionManager.MAINHAND);
            magic.setPlayerCastingItem(staff);
            RevolvercastStaffSpellSelectionEvents.onCastStarted(player, SpellSelectionManager.MAINHAND);
            RevolvercastStaffSpellSelectionEvents.tickPlayer(player);
            helper.assertTrue(magic.isCasting(), "An unchanged revolver must not cancel the first casting tick");
            if (variant == 0) player.setItemInHand(InteractionHand.MAIN_HAND, staff.copy());
            else if (variant == 1) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            else RevolvercastStaff.advanceToNextValidScrollIndex(staff);
            RevolvercastStaffSpellSelectionEvents.tickPlayer(player);
            helper.assertFalse(magic.isCasting(), "Replacement, removal, and selection changes must cancel active casts");
        }
        helper.succeed();
    }

    private static void discard(GameTestHelper helper, ItemStack staff) {
        RevolvercastStaff.discardLegacySpellContainer(staff);
        helper.assertFalse(ISpellContainer.isSpellContainer(staff), "Cleanup must remove the host container");
        helper.assertFalse(SpellExtractionHelper.evaluate(staff).isSuccess(), "Cleaned revolver must reject destructive extraction");
    }

    private static int countScrolls(ItemStack staff) {
        int count = 0;
        for (int slot = 0; slot < RevolvercastStaff.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!RevolvercastStaff.getCalibrationScroll(staff, slot).isEmpty()) ++count;
        }
        return count;
    }

    private static void assertRepeatable(GameTestHelper helper, ItemStack staff) {
        var saved = staff.saveOptional(helper.getLevel().registryAccess());
        var reloaded = ItemStack.parse(helper.getLevel().registryAccess(), saved).orElseThrow();
        discard(helper, reloaded);
        helper.assertTrue(ItemStack.isSameItemSameComponents(staff, reloaded), "Reload and repeated cleanup must preserve exact data");
    }
}
