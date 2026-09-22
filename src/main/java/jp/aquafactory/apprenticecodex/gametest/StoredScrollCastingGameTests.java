package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.StoredScrollCastingEvents;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellExtractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class StoredScrollCastingGameTests {
    private StoredScrollCastingGameTests() {}

    private static Item[] targets() {
        return new Item[]{ItemRegistry.SCROLLCASTER_GAUNTLET.get(), ItemRegistry.CHARGECAST_CATALYSTBOOK.get()};
    }

    private static void put(ItemStack stack, int slot, ItemStack scroll) {
        if (stack.getItem() instanceof ScrollcasterGauntlet) ScrollcasterGauntlet.setCalibrationScroll(stack, slot, scroll);
        else ChargecastCatalystbook.setCalibrationScroll(stack, slot, scroll);
    }

    private static void discard(ItemStack stack) {
        if (stack.getItem() instanceof ScrollcasterGauntlet) ScrollcasterGauntlet.discardLegacySpellContainer(stack);
        else ChargecastCatalystbook.discardLegacySpellContainer(stack);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fixedBonusPreservesAssetsAndStacksWithAffinity(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, spell.getMaxLevel()));
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        for (var item : targets()) for (int level : new int[]{0, 1, 3}) {
            var stack = new ItemStack(item);
            helper.assertTrue(stack.supportsEnchantment(enchantment), "Both stored-scroll items must accept Transcendence");
            if (level > 0) stack.enchant(enchantment, level);
            put(stack, 0, scroll.copy());
            int lockedSlot = item instanceof ScrollcasterGauntlet ? 9 : 3;
            put(stack, lockedSlot, scroll.copy());
            var before = stack.copy();
            int expected = spell.getMaxLevel() + (level > 0 ? 1 : 0);
            var tooltip = item instanceof ScrollcasterGauntlet
                    ? ScrollcasterGauntlet.getScrollTooltipData(stack) : ChargecastCatalystbook.getScrollTooltipData(stack);
            helper.assertTrue(tooltip.selectedSpell().getLevel() == expected
                            && tooltip.entries().getFirst().spell().getLevel() == expected
                            && tooltip.entries().getLast().spell().getLevel() == spell.getMaxLevel(),
                    "Only usable scrolls must expose the fixed bonus");
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, stack), "Tooltip must not mutate stored assets");
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "stored_bonus_" + level + "_" + item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(CuriosSlotConstants.HEAD, 0, ring);
            var magic = MagicData.getPlayerMagicData(player);
            magic.getSyncedData().getSpellSelection().makeSelection(SpellSelectionManager.MAINHAND, 0);
            magic.setMana(10000);
            helper.assertTrue(new SpellSelectionManager(player).getSelectedSpellData().getLevel() == expected,
                    "Wheel must expose the internal bonus without Affinity");
            item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(magic.getCastingSpellLevel() == expected + 2, "Casting must apply internal bonus and Affinity once");
            Utils.serverSideCancelCast(player);
            var stored = item instanceof ScrollcasterGauntlet
                    ? ScrollcasterGauntlet.getCalibrationScroll(stack, 0) : ChargecastCatalystbook.getCalibrationScroll(stack, 0);
            helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, stored), "Casting must preserve the original scroll");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Casting must not create a host container");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyCleanupDiscardsOnlyProjection(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (var item : targets()) {
            var stack = new ItemStack(item);
            put(stack, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
            put(stack, item instanceof ScrollcasterGauntlet ? 9 : 3,
                    SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 2)));
            var before = stack.copy();
            ISpellContainer.createImbuedContainer(spell, 3, stack);
            PresetSpellContainerStateHelper.rememberOverridden(stack, new SpellData(spell, 3));
            discard(stack);
            discard(stack);
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, stack), "Cleanup must preserve all internal assets");
            helper.assertFalse(SpellExtractionHelper.evaluate(stack).isSuccess(), "Destructive extraction must stay blocked");
            var empty = new ItemStack(item);
            ISpellContainer.createImbuedContainer(spell, 3, empty);
            discard(empty);
            helper.assertTrue(StoredScrollCastingEvents.selected(empty) == SpellData.EMPTY, "Projection must not restore scrolls");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wheelDuplicatesKeepTheirOwnLevels(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "stored_duplicates");
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
        put(gauntlet, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
        put(book, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 2)));
        var helmet = new ItemStack(Items.LEATHER_HELMET);
        ISpellContainer.createImbuedContainer(spell, 3, helmet);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        var manager = new SpellSelectionManager(player);
        helper.assertTrue(manager.getSpellCount() == 3, "Identical spells from distinct sources must remain independent");
        helper.assertTrue(manager.getSpellForSlot(SpellSelectionManager.MAINHAND, 0).getLevel() == 1
                        && manager.getSpellForSlot(SpellSelectionManager.OFFHAND, 0).getLevel() == 2,
                "Each hand must keep its own spell level");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void borrowedWheelSpellDoesNotReceiveHostBonus(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        for (var item : targets()) {
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "stored_borrowed_" + item);
            var stack = new ItemStack(item);
            stack.enchant(enchantment, 3);
            put(stack, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)));
            if (item instanceof ChargecastCatalystbook book) {
                helper.assertTrue(book.trySetCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get())),
                        "Wisdom Shard must enable borrowed casting");
            }
            var helmet = new ItemStack(Items.LEATHER_HELMET);
            ISpellContainer.createImbuedContainer(spell, 3, helmet);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magic = MagicData.getPlayerMagicData(player);
            magic.getSyncedData().getSpellSelection().makeSelection(EquipmentSlot.HEAD.getName(), 0);
            magic.setMana(10000);
            item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(magic.getCastingSpellLevel() == 3,
                    "Borrowing the same spell from another source must not add the host's bonus");
            Utils.serverSideCancelCast(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void castLifetimeTracksSourceRatherThanWheelCursor(GameTestHelper helper) {
        for (var item : targets()) {
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                    new BlockPos(0, 2, 0), "stored_lifetime_" + item);
            var stack = new ItemStack(item);
            put(stack, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1)));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magic = MagicData.getPlayerMagicData(player);
            // 外部魔法を借りた長い詠唱でも、使用アイテムの内部選択を監視する契約。
            magic.getSyncedData();
            magic.initiateCast(SpellRegistry.FIREBOLT_SPELL.get(), 1, 20, CastSource.SWORD, SpellSelectionManager.MAINHAND);
            StoredScrollCastingEvents.onCastStarted(player, SpellSelectionManager.MAINHAND);
            magic.getSyncedData().getSpellSelection().makeSelection(SpellSelectionManager.OFFHAND, 0);
            StoredScrollCastingEvents.validateCast(player);
            helper.assertTrue(magic.isCasting(), "Wheel cursor changes must not cancel the current cast");
            put(stack, 0, SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 2)));
            StoredScrollCastingEvents.validateCast(player);
            helper.assertFalse(magic.isCasting(), "Changing the internal spell level must cancel even a borrowed cast");
        }
        helper.succeed();
    }
}
