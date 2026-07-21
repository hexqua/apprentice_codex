package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record SneakSelectionView(
        int selectionIndex,
        Component displayName,
        @Nullable ResourceLocation icon,
        boolean selectable
) {
    public static SneakSelectionView forSpell(int selectionIndex, SpellData spellData, boolean selectable) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return new SneakSelectionView(selectionIndex, Component.empty(), null, false);
        }

        var spell = spellData.getSpell();
        var displayName = spell.getDisplayName(null)
                .copy()
                .append(" ")
                .append(Integer.toString(spellData.getLevel()))
                .withStyle(spell.getSchoolType().getDisplayName().getStyle());
        return new SneakSelectionView(
                selectionIndex,
                displayName,
                spell.getSpellIconResource(),
                selectable
        );
    }
}
