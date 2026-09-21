package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 保存枠を含めた一覧と選択魔法を、表示による保存データ更新なしで渡す。 */
public record ScrollSlotTooltipData(SpellData selectedSpell, int selectedSlot, List<Entry> entries) {
    public ScrollSlotTooltipData {
        entries = List.copyOf(entries);
    }

    public record Entry(int slot, ItemStack scroll, SpellData spell, boolean usable) {}
}
