package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;

public final class SpellScrollTestSupport {
    private SpellScrollTestSupport() {}

    public static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }
}
