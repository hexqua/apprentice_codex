package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface RestrictedSpellImbuableItem {
    boolean canImbueSpell(SpellData spellData);

    boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel);

    void normalizeImbuedSpellContainer(ItemStack stack);

    default ItemStack createArcaneAnvilImbueResult(ItemStack baseStack, SpellData spellData) {
        var resultStack = baseStack.copy();
        ISpellContainer.createScrollContainer(spellData.getSpell(), spellData.getLevel(), resultStack);
        normalizeImbuedSpellContainer(resultStack);
        return resultStack;
    }

    default int getWorkbenchSpellExtractionIndex(ItemStack stack, ISpellContainer spellContainer) {
        return 0;
    }

    default boolean canRemoveWorkbenchSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        return spellData.canRemove();
    }
}
