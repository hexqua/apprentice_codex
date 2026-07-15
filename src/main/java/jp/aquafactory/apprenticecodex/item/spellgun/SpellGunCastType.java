package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.spells.CastType;
import org.jetbrains.annotations.Nullable;

public enum SpellGunCastType {
    INSTANT,
    LONG;

    @Nullable
    public static SpellGunCastType from(CastType castType) {
        if (castType == CastType.INSTANT) {
            return INSTANT;
        }
        if (castType == CastType.LONG) {
            return LONG;
        }
        return null;
    }
}
