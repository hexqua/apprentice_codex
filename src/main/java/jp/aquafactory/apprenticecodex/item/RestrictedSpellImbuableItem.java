package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface RestrictedSpellImbuableItem {
    boolean canImbueSpell(SpellData spellData);

    boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel);

    void normalizeImbuedSpellContainer(ItemStack stack);
}
