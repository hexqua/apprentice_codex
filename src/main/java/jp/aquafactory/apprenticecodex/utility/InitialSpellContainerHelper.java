package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InitialSpellContainerHelper {
    private InitialSpellContainerHelper() {
    }

    public static boolean isInitialSpellEnabled(@Nullable AbstractSpell spell) {
        return spell != null && spell != SpellRegistry.none() && spell.isEnabled();
    }

    public static boolean addInitialSpellIfEnabled(
            ISpellContainerMutable spellContainer,
            @Nullable AbstractSpell spell,
            int spellLevel,
            int index,
            boolean locked
    ) {
        return isInitialSpellEnabled(spell)
                && spellContainer.addSpellAtIndex(spell, spellLevel, index, locked);
    }

    public static void setInitialContainer(
            ItemStack stack,
            int maxSpellCount,
            boolean addsToSpellWheel,
            boolean mustBeEquipped,
            @Nullable AbstractSpell spell,
            int spellLevel
    ) {
        var spellContainer = ISpellContainer.create(maxSpellCount, addsToSpellWheel, mustBeEquipped).mutableCopy();
        addInitialSpellIfEnabled(spellContainer, spell, spellLevel, 0, true);
        ISpellContainer.set(stack, spellContainer.toImmutable());
    }
}
