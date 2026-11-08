package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu;

import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CalibrationTooltipGameTests {
    private CalibrationTooltipGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void adjustmentTooltipsReflectStoredComponentsWithoutMutatingTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1枠の武器と3枠のアクセサリー・防具で、保存データからtooltipへの接続を確認する。
            var representatives = List.of(
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.AUTOCAST_AMULET.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()));
            for (var stack : representatives) {
                var target = (SpellCalibrationAdjustmentTarget) stack.getItem();
                var slot = target.getCalibrationAdjustmentSlotCount(stack) - 1;
                var adjustment = stack.is(ItemRegistry.IRON_SPELLCASTER_GUN.get())
                        ? new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())
                        : stack.is(ItemRegistry.AUTOCAST_AMULET.get())
                        ? new ItemStack(ItemRegistry.WISDOM_SHARD.get())
                        : new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
                adjustment.set(DataComponents.CUSTOM_NAME, Component.literal("Tooltip component"));
                helper.assertTrue(target.trySetCalibrationAdjustment(stack, slot, adjustment,
                                helper.getLevel().registryAccess()), "Tooltip fixture adjustment should be accepted");
                var snapshot = stack.copy();
                var payload = stack.getItem().getTooltipImage(stack).orElseThrow();
                helper.assertTrue(payload instanceof CalibrationAdjustmentTooltip,
                        "Stored adjustment should provide calibration tooltip data");
                var items = ((CalibrationAdjustmentTooltip) payload).items();
                helper.assertTrue(items.size() == slot + 1 && ItemStack.matches(items.get(slot), adjustment),
                        "Tooltip should retain the selected slot and all stored components");
                for (var emptySlot = 0; emptySlot < slot; ++emptySlot) {
                    helper.assertTrue(items.get(emptySlot).isEmpty(), "Tooltip should retain empty slot gaps");
                }
                helper.assertTrue(ItemStack.matches(snapshot, stack), "Reading tooltip data should not mutate storage");
            }
        });
    }
}
