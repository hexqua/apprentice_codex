package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellCalibrationAcceptanceGameTests extends ApprenticeCodexGameTestScenarios {
    @GameTest(template = "gametest/basic_floor")
    public static void rifleAcceptanceIsIndependentOfSilverRing(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "rifle_acceptance");
        var lookup = helper.getLevel().registryAccess();
        var instant = new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
        var longSpell = new SpellData(SpellRegistry.FIREBALL_SPELL.get(), 1);
        var continuous = new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1);
        for (var item : new Item[]{ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get()}) {
            var stack = new ItemStack(item);
            var target = (SpellCalibrationImbueTarget) item;
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(0).set(stack);
            var scrollSlot = menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START);
            var sourceSlot = menu.slots.size() - 1;
            try {
                for (var ringPresent : new boolean[]{false, true, false}) {
                    menu.getSlot(1).set(ringPresent
                            ? new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                            : ItemStack.EMPTY);
                    for (var data : new SpellData[]{instant, longSpell, continuous, SpellData.EMPTY}) {
                        var expected = data == instant || data == longSpell;
                        var state = target.evaluateCalibrationImbue(stack, 0, data, lookup);
                        helper.assertTrue(state.canInsert() == expected, "Only INSTANT and LONG must be accepted");
                        helper.assertTrue(state.isUsable() == (data == instant || data == longSpell && ringPresent),
                                "Silver Ring must affect LONG usability only");
                        helper.assertTrue(target.evaluateCalibrationImbue(stack, 0, data).equals(state),
                                "Both lookup overloads must agree");
                        var scroll = SpellCalibrationImbueHelper.createScroll(data);
                        helper.assertTrue(scrollSlot.mayPlace(scroll) == expected, "Menu acceptance must match evaluation");
                    }
                    helper.assertFalse(scrollSlot.mayPlace(new ItemStack(Items.STICK)), "Non-scroll items must be rejected");
                    helper.assertFalse(target.evaluateCalibrationImbue(stack, -1, instant, lookup).canInsert(),
                            "Negative slots must be rejected");
                    helper.assertFalse(target.evaluateCalibrationImbue(stack, 12, instant, lookup).canInsert(),
                            "Out-of-range slots must be rejected");

                    // shift-clickで元だけ減る回帰を防ぎ、格納・警告・リング取り外し後の保持まで確認する。
                    scrollSlot.set(ItemStack.EMPTY);
                    menu.getSlot(sourceSlot).set(SpellCalibrationImbueHelper.createScroll(longSpell));
                    helper.assertFalse(menu.quickMoveStack(player, sourceSlot).isEmpty(), "LONG shift-click must succeed");
                    helper.assertTrue(menu.getSlot(sourceSlot).getItem().isEmpty() && !scrollSlot.getItem().isEmpty(),
                            "Accepted LONG must move into storage without loss");
                    helper.assertTrue(menu.shouldRenderMismatchCastConditionWarning(0) == !ringPresent,
                            "Stored LONG warning must follow Silver Ring");
                    menu.getSlot(sourceSlot).set(SpellCalibrationImbueHelper.createScroll(continuous));
                    helper.assertTrue(menu.quickMoveStack(player, sourceSlot).isEmpty(), "CONTINUOUS shift-click must fail");
                    helper.assertTrue(menu.getSlot(sourceSlot).getItem().getCount() == 1, "Rejected scroll must remain in inventory");
                    var rejected = SpellCalibrationImbueHelper.createScroll(continuous);
                    if (item == ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get()) {
                        FullautoRapidcastSpellrifleScrollStorage.set(stack, 0, rejected, lookup);
                    } else {
                        MultipurposeStaffrifleScrollStorage.set(stack, 0, rejected, lookup);
                    }
                    helper.assertTrue(ItemStack.isSameItemSameTags(scrollSlot.getItem(),
                            SpellCalibrationImbueHelper.createScroll(longSpell)), "Storage must reject CONTINUOUS without replacing LONG");
                }
            } finally {
                menu.getSlot(sourceSlot).set(ItemStack.EMPTY);
                menu.removed(player);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyFullautoContinuousScrollRemainsExtractable(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "legacy_rifle_scroll");
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1));
        // 修正前に保存された非対応スクロールは削除せず、取り出しを保証する。
        {
            var root = stack.getOrCreateTag();
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.put("Item", scroll.save(new CompoundTag()));
            var list = new ListTag();
            list.add(entry);
            var calibration = new CompoundTag();
            calibration.put("Scrolls", list);
            root.put("FullautoRapidcastSpellrifleCalibration", calibration);
        }
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        try {
            menu.getSlot(0).set(stack);
            var slot = menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START);
            helper.assertTrue(menu.shouldRenderMismatchCastConditionWarning(0), "Legacy CONTINUOUS must remain unusable");
            helper.assertTrue(slot.mayPickup(player), "Legacy rejected scroll must remain extractable");
        helper.assertTrue(ItemStack.isSameItemSameTags(slot.remove(1), scroll), "Legacy extraction must preserve scroll");
            helper.assertTrue(slot.getItem().isEmpty(), "Extracted legacy scroll must leave empty storage");
            helper.assertFalse(slot.mayPlace(scroll), "Extracted CONTINUOUS must not be reinsertable");
        } finally {
            menu.removed(player);
        }
        helper.succeed();
    }
}
