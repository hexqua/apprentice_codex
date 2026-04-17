package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface WeaponImbueCooldownPolicyItem {
    default boolean ignoresWeaponImbueCooldownMultiplier(
            ItemStack stack,
            @Nullable AbstractSpell spell,
            CastSource castSource
    ) {
        return false;
    }
}
