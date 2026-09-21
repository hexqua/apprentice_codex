package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record RifleSpellTooltipData(SpellData selectedSpell, CastSource castSource, int selectedSlot,
                                    List<Slot> slots) {
    public record Slot(int index, ItemStack scroll, SpellData spell, boolean usable) {}

    public RifleSpellTooltipData {
        slots = List.copyOf(slots);
    }

    public static RifleSpellTooltipData read(ItemStack stack, Player player, HolderLookup.Provider lookup) {
        boolean multipurpose = stack.getItem() instanceof MultipurposeStaffrifle;
        var target = (SpellCalibrationImbueTarget) stack.getItem();
        boolean wisdom = multipurpose && MultipurposeStaffrifle.hasWisdomShard(stack, lookup);
        int storedSelection = multipurpose ? MultipurposeStaffrifleScrollStorage.selected(stack)
                : FullautoRapidcastSpellrifleScrollStorage.selected(stack);
        int count = multipurpose ? MultipurposeStaffrifleScrollStorage.MAX_SCROLL_SLOTS
                : FullautoRapidcastSpellrifleScrollStorage.MAX_SCROLL_SLOTS;
        var slots = new ArrayList<Slot>();
        for (int i = 0; i < count; i++) {
            var scroll = multipurpose ? MultipurposeStaffrifleScrollStorage.get(stack, i, lookup)
                    : FullautoRapidcastSpellrifleScrollStorage.get(stack, i, lookup);
            if (scroll.isEmpty()) continue;
            var spell = multipurpose ? MultipurposeStaffrifleScrollStorage.spell(stack, i, lookup)
                    : FullautoRapidcastSpellrifleScrollStorage.spell(stack, i, lookup);
            boolean usable = target.evaluateCalibrationImbue(stack, i, spell, lookup).isUsable();
            if (SpellCalibrationImbueTarget.isValidCalibrationSpell(spell)) {
                int level = multipurpose ? MultipurposeStaffrifle.resolveImbuedSpellLevel(stack, spell)
                        : FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(stack, spell);
                spell = new SpellData(spell.getSpell(), level);
            }
            slots.add(new Slot(i, scroll, spell, usable));
        }
        if (wisdom) {
            var selection = player == null ? null : new SpellSelectionManager(player).getSelection();
            return new RifleSpellTooltipData(selection == null ? SpellData.EMPTY : selection.spellData,
                    selection == null ? CastSource.SPELLBOOK : selection.getCastSource(), -1, slots);
        }
        // 通常の選択補正と同じ優先順を使うが、hoverだけで保存データを書き換えない。
        var selected = slots.stream().filter(slot -> slot.usable() && slot.index() == storedSelection)
                .findFirst().orElseGet(() -> slots.stream().filter(Slot::usable).findFirst().orElse(null));
        return new RifleSpellTooltipData(selected == null ? SpellData.EMPTY : selected.spell(),
                CastSource.SWORD, selected == null ? -1 : selected.index(), slots);
    }
}
