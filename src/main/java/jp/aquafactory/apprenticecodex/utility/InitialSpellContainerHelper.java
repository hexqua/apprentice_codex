package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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

    public static boolean addInitialSpellIfEnabled(
            ISpellContainerMutable spellContainer,
            @Nullable Supplier<? extends AbstractSpell> spellSupplier,
            int spellLevel,
            int index,
            boolean locked
    ) {
        return addInitialSpellIfEnabled(
                spellContainer,
                resolveInitialSpell(spellSupplier),
                spellLevel,
                index,
                locked
        );
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

    public static void setInitialContainer(
            ItemStack stack,
            int maxSpellCount,
            boolean addsToSpellWheel,
            boolean mustBeEquipped,
            @Nullable Supplier<? extends AbstractSpell> spellSupplier,
            int spellLevel
    ) {
        var spellContainer = ISpellContainer.create(maxSpellCount, addsToSpellWheel, mustBeEquipped).mutableCopy();
        addInitialSpellIfEnabled(spellContainer, spellSupplier, spellLevel, 0, true);
        ISpellContainer.set(stack, spellContainer.toImmutable());
    }

    private static @Nullable AbstractSpell resolveInitialSpell(@Nullable Supplier<? extends AbstractSpell> spellSupplier) {
        if (spellSupplier == null) {
            return null;
        }
        if (spellSupplier instanceof DeferredHolder<?, ?> deferredHolder && !deferredHolder.isBound()) {
            return null;
        }
        return spellSupplier.get();
    }
}
