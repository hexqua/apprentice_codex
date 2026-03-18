package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import org.jetbrains.annotations.Nullable;

public interface ManaBypassSpellItem {
    boolean supportsManaBypass(@Nullable AbstractSpell spell);
}
