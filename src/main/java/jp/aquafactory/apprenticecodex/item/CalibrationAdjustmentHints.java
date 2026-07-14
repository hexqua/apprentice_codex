package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.item.Items;

/** 複数の調整対象で共有する意味的な候補ヘルプ。 */
public final class CalibrationAdjustmentHints {
    private static final String KEY_PREFIX =
            "container.apprenticecodex.spell_calibration_bench.tooltip.";

    private CalibrationAdjustmentHints() {
    }

    public static CalibrationAdjustmentHint schoolRunes() {
        return CalibrationAdjustmentHint.translatable(KEY_PREFIX + "item_hint_runes");
    }

    public static CalibrationAdjustmentHint slotUpgrades() {
        return CalibrationAdjustmentHint.taggedItems(
                KEY_PREFIX + "item_hint_slot_upgrades",
                TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES,
                io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE
        );
    }

    public static CalibrationAdjustmentHint enchantmentBooks() {
        return CalibrationAdjustmentHint.taggedItems(
                KEY_PREFIX + "item_hint_enchantment_books",
                TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS,
                () -> Items.ENCHANTED_BOOK
        );
    }

    public static CalibrationAdjustmentHint mithrilFreecastStaff() {
        return CalibrationAdjustmentHint.specificItem(ItemRegistry.MITHRIL_FREECAST_STAFF);
    }

    public static CalibrationAdjustmentHint recoveryRune() {
        return CalibrationAdjustmentHint.specificItem(
                io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE
        );
    }

    public static CalibrationAdjustmentHint silverRing() {
        return CalibrationAdjustmentHint.specificItem(
                io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING
        );
    }

    public static CalibrationAdjustmentHint wisdomShard() {
        return CalibrationAdjustmentHint.specificItem(ItemRegistry.WISDOM_SHARD);
    }
}
