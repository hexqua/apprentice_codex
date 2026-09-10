package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 発射と表示で共有する系統補正。詠唱・クールダウンの権限は変更しない。 */
public final class ElementalBowSpellPowerContext {
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();

    private ElementalBowSpellPowerContext() {}

    public static Scope open(LivingEntity caster, AbstractSpell spell, ItemStack bow) {
        var previous = ACTIVE.get();
        ACTIVE.set(new Frame(caster, spell, ElementalBowRunes.school(bow)));
        return () -> {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        };
    }

    @Nullable
    public static SchoolType school(AbstractSpell spell, LivingEntity caster) {
        var frame = ACTIVE.get();
        return frame != null && frame.caster == caster && frame.spell == spell ? frame.school : null;
    }

    private record Frame(LivingEntity caster, AbstractSpell spell, @Nullable SchoolType school) {}

    public interface Scope extends AutoCloseable {
        @Override void close();
    }
}
