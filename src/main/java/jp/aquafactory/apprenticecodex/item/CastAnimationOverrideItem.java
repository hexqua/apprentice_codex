package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface CastAnimationOverrideItem {
    default boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell);

    AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel);

    boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell);
}
