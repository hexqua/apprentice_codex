package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.item.ItemStack;

import java.util.function.ToIntFunction;

/**
 * 装備の収集方法やエンチャント registry の世代差に依存しない Transcendence の共通判定。
 */
public final class TranscendenceResolver {
    private TranscendenceResolver() {
    }

    public static int resolveMaxEventLevel(
            AbstractSpell spell,
            Iterable<Candidate> candidates,
            ToIntFunction<ItemStack> enchantmentLevelResolver
    ) {
        if (spell == null) {
            return 0;
        }

        var maxLevel = 0;
        for (var candidate : candidates) {
            if (candidate == null || candidate.stack() == null || candidate.stack().isEmpty()) {
                continue;
            }

            var stack = candidate.stack();
            if (!(stack.getItem() instanceof TranscendencePolicy policy)
                    || policy.transcendenceHandling() != TranscendencePolicy.Handling.EVENT
                    || candidate.held() && !policy.isTranscendenceActiveWhileHeld()
                    || !containsActiveSpell(stack, spell)) {
                continue;
            }

            maxLevel = Math.max(maxLevel, Math.max(0, enchantmentLevelResolver.applyAsInt(stack)));
        }
        return maxLevel;
    }

    public static boolean containsActiveSpell(ItemStack stack, AbstractSpell spell) {
        if (stack == null || stack.isEmpty() || spell == null || !ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var container = ISpellContainer.get(stack);
        return container != null && container.getActiveSpells().stream()
                .anyMatch(slot -> spell.equals(slot.getSpell()));
    }

    public record Candidate(ItemStack stack, boolean held) {
    }
}
