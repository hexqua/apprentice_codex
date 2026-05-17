package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ItemTransformPreservingCastAnimationItem extends CastAnimationOverrideItem {
    @Override
    default boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return getItemTransformPreservingCastStartAnimation(stack, spell).isPresent();
    }

    @Override
    default AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return getItemTransformPreservingCastStartAnimation(stack, spell)
                .orElseGet(spell::getCastStartAnimation);
    }

    @Override
    default boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    default Optional<AnimationHolder> getItemTransformPreservingCastStartAnimation(
            ItemStack stack,
            @Nullable AbstractSpell spell
    ) {
        if (spell == null) {
            return Optional.empty();
        }

        return ItemTransformPreservingCastAnimations.resolve(spell.getCastStartAnimation());
    }
}
