package jp.aquafactory.apprenticecodex.compat.magitech;

import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.ItemInit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Magitech不在時に上流の型を読み込まないよう、調整定義も連携側へ隔離する。 */
public final class MagiEngineersStaffCalibration {
    private record Adjustment(String id, Supplier<? extends Item> item,
                              double charge, double cooldown, double launch, double efficiency) {
        CalibrationAdjustmentRule rule() {
            return CalibrationAdjustmentRule.repeatable(id, stack -> stack.is(item.get()),
                    CalibrationAdjustmentHint.specificItem(item)).withEffectLines(this::effects);
        }

        private List<Component> effects() {
            var lines = new ArrayList<Component>();
            addEffect(lines, "charge_speed", charge);
            addEffect(lines, "cooldown_speed", cooldown);
            addEffect(lines, "launch", launch);
            addEffect(lines, "mana_efficiency", efficiency);
            return lines;
        }
    }

    private static final List<Adjustment> ADJUSTMENTS = List.of(
            new Adjustment("high_purity_fluorite", ItemInit.HIGH_PURITY_FLUORITE, 0, 0, -0.15, 0.05),
            new Adjustment("magic_crystal", ItemInit.MAGIC_CRYSTAL, -0.05, -0.05, 0, 0.05),
            new Adjustment("surge_crystal", ItemInit.SURGE_CRYSTAL, 0.05, 0, 0, -0.10),
            new Adjustment("glace_crystal", ItemInit.GLACE_CRYSTAL, 0, 0.05, 0, -0.10),
            new Adjustment("hollow_crystal", ItemInit.HOLLOW_CRYSTAL, 0, 0, 0.10, -0.10));

    public static final CalibrationAdjustmentProfile PROFILE = CalibrationAdjustmentProfile.of(
            ADJUSTMENTS.stream().map(Adjustment::rule).toArray(CalibrationAdjustmentRule[]::new));

    private MagiEngineersStaffCalibration() {
    }

    public static double base(ItemStack stack, Holder<Attribute> attribute, HolderLookup.Provider lookup) {
        double value = attribute.equals(AttributeInit.LAUNCH) ? 1.5 : 1;
        int slots = MagiEngineersStaff.CALIBRATION_ADJUSTMENT_SLOT_COUNT;
        for (int slot = 0; slot < slots; slot++) {
            var inserted = CalibrationAdjustmentStorage.get(stack, slot, slots, lookup);
            for (var adjustment : ADJUSTMENTS) {
                if (!inserted.is(adjustment.item().get())) continue;
                if (attribute.equals(AttributeInit.CHARGE_SPEED)) value += adjustment.charge();
                else if (attribute.equals(AttributeInit.COOLDOWN_SPEED)) value += adjustment.cooldown();
                else if (attribute.equals(AttributeInit.LAUNCH)) value += adjustment.launch();
                else if (attribute.equals(AttributeInit.MANA_EFFICIENCY)) value += adjustment.efficiency();
            }
        }
        return value;
    }

    private static void addEffect(List<Component> lines, String property, double amount) {
        if (amount == 0) return;
        long percent = Math.round(amount * 100);
        lines.add(Component.translatable("jei.apprenticecodex.spell_calibration_bench.effect.magitech.change_property",
                Component.translatable("tool.magitech.property." + property),
                (percent > 0 ? "+" : "") + percent));
    }
}
