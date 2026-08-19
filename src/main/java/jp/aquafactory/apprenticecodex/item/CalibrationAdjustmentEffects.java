package jp.aquafactory.apprenticecodex.item;

import net.minecraft.network.chat.Component;

import java.util.List;

/** 術式調整台のJEI表示で共有する、調整後の効果説明。 */
public final class CalibrationAdjustmentEffects {
    private static final String KEY_PREFIX =
            "jei.apprenticecodex.spell_calibration_bench.effect.";

    private CalibrationAdjustmentEffects() {
    }

    public static List<Component> addScrollSlot(int count) {
        return line("add_scroll_slot", count);
    }

    public static List<Component> addSpellPower(double amount) {
        return line("add_spell_power", percentage(amount));
    }

    public static List<Component> addSpellResist(double amount) {
        return line("add_spell_resist", percentage(amount));
    }

    public static List<Component> changeSpellPower(double schoolAmount, double generalReduction) {
        return List.of(
                Component.translatable(KEY_PREFIX + "change_spell_power_1", percentage(schoolAmount)),
                Component.translatable(KEY_PREFIX + "change_spell_power_2", percentage(generalReduction))
        );
    }

    public static List<Component> addLongSupport() {
        return line("add_long_support");
    }

    public static List<Component> addAllSupport() {
        return line("add_all_support");
    }

    public static List<Component> changeAttributeOffhand() {
        return line("change_attribute_offhand");
    }

    public static List<Component> changeImbueToSelected() {
        return line("change_imbue_to_select");
    }

    public static List<Component> adaptAutocastSituation() {
        return List.of(
                Component.translatable(KEY_PREFIX + "adapt_autocast_situation_1"),
                Component.translatable(KEY_PREFIX + "adapt_autocast_situation_2")
        );
    }

    public static List<Component> switchSlotOnFailedCast() {
        return line("adapt_failed_switch_slot");
    }

    public static List<Component> addSwingcastFunction() {
        return line("add_swingcast_function");
    }

    public static List<Component> convertQuickToPower() {
        return line("convert_quick_to_power");
    }

    public static List<Component> adaptBackCurios() {
        return line("adapt_back_curios");
    }

    public static List<Component> gainFireward() {
        return List.of(
                Component.translatable(KEY_PREFIX + "gain_fireward_1"),
                Component.translatable(KEY_PREFIX + "gain_fireward_2")
        );
    }

    public static List<Component> adaptUnderwaterMobility() {
        return List.of(
                Component.translatable(KEY_PREFIX + "adapt_underwater_mobility_1"),
                Component.translatable(KEY_PREFIX + "adapt_underwater_mobility_2")
        );
    }

    public static List<Component> overdriveFloatmountBroom() {
        return List.of(
                Component.translatable(KEY_PREFIX + "overdrive_engine.float_1"),
                Component.translatable(KEY_PREFIX + "overdrive_engine.float_2")
        );
    }

    public static List<Component> overdriveHoverrideBroom() {
        return List.of(
                Component.translatable(KEY_PREFIX + "overdrive_engine.hover_1"),
                Component.translatable(KEY_PREFIX + "overdrive_engine.hover_2")
        );
    }

    private static List<Component> line(String key, Object... args) {
        return List.of(Component.translatable(KEY_PREFIX + key, args));
    }

    private static long percentage(double amount) {
        return Math.round(amount * 100.0D);
    }
}
