package jp.aquafactory.apprenticecodex.loot;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;

public final class RandomSpellImbueHelper {
    private RandomSpellImbueHelper() {
    }

    public static @NotNull ItemStack imbueRandomEnabledSpellOrFallback(
            @NotNull ItemStack targetStack,
            @NotNull List<ResourceLocation> spellIds,
            @NotNull Item fallbackItem,
            @NotNull RandomSource random
    ) {
        var enabledSpells = new LinkedHashSet<AbstractSpell>();
        for (var spellId : spellIds) {
            var spell = SpellRegistry.getSpell(spellId);
            if (spell != SpellRegistry.none() && spell.isEnabled()) {
                enabledSpells.add(spell);
            }
        }

        if (enabledSpells.isEmpty()) {
            return createFallbackStack(targetStack, fallbackItem);
        }

        var candidates = List.copyOf(enabledSpells);
        var selectedSpell = candidates.get(random.nextInt(candidates.size()));
        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();

        // Iron's の Utils.canImbue は UniqueItem を拒否するため、loot専用の初期付与では直接containerを構築する。
        if (!spellContainer.addSpellAtIndex(selectedSpell, 1, 0, true)) {
            return createFallbackStack(targetStack, fallbackItem);
        }

        ISpellContainer.set(targetStack, spellContainer.toImmutable());
        return targetStack;
    }

    private static @NotNull ItemStack createFallbackStack(ItemStack targetStack, Item fallbackItem) {
        return new ItemStack(fallbackItem, targetStack.getCount());
    }
}
