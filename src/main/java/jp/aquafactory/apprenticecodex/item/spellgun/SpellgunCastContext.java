package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Spellgun が Iron's の詠唱処理へ入る短い区間だけ、追加の詠唱契約を伝える。
 * 通常詠唱の LONG / CONTINUOUS は複数 tick の状態管理が必要になるため対象にしない。
 */
public final class SpellgunCastContext {
    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private SpellgunCastContext() {
    }

    public static Scope openInitiation(
            Player player,
            AbstractSpell spell,
            ItemStack castingItem,
            boolean bypassManaCheck
    ) {
        return push(createFrame(player, spell, castingItem, bypassManaCheck, false));
    }

    public static Scope openActivation(
            LivingEntity caster,
            AbstractSpell spell,
            @Nullable MagicData magicData
    ) {
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        var frame = createFrame(caster, spell, castingItem, false, true);
        return frame.powerOverride() == null ? Scope.NOOP : push(frame);
    }

    public static boolean shouldBypassManaCheck(AbstractSpell spell, Player player) {
        var frame = findFrame(spell, player);
        return frame != null && frame.bypassManaCheck();
    }

    public static double resolveSpellPower(AbstractSpell spell, LivingEntity caster, double originalValue) {
        var frame = findFrame(spell, caster);
        return frame == null || frame.powerOverride() == null
                ? originalValue
                : frame.powerOverride().forcedSpellPower();
    }

    public static double resolveSchoolSpellPower(AbstractSpell spell, LivingEntity caster, double originalValue) {
        var frame = findFrame(spell, caster);
        return frame == null || frame.powerOverride() == null
                ? originalValue
                : frame.powerOverride().forcedSchoolSpellPower();
    }

    public static @Nullable Double resolveSummonDamage(LivingEntity caster) {
        for (var frame : FRAMES.get()) {
            if (frame.overrideSummonDamage()
                    && frame.casterId().equals(caster.getUUID())
                    && frame.powerOverride() != null) {
                return frame.powerOverride().forcedSummonDamage();
            }
        }
        return null;
    }

    private static Frame createFrame(
            LivingEntity caster,
            AbstractSpell spell,
            ItemStack castingItem,
            boolean bypassManaCheck,
            boolean overrideSummonDamage
    ) {
        var powerOverride = castingItem.getItem() instanceof ForcedSpellPowerSpellgun forcedSpellgun
                ? forcedSpellgun
                : null;
        return new Frame(caster.getUUID(), spell, bypassManaCheck, overrideSummonDamage, powerOverride);
    }

    private static @Nullable Frame findFrame(AbstractSpell spell, LivingEntity caster) {
        for (var frame : FRAMES.get()) {
            if (frame.spell() == spell && frame.casterId().equals(caster.getUUID())) {
                return frame;
            }
        }
        return null;
    }

    private static Scope push(Frame frame) {
        var frames = FRAMES.get();
        frames.push(frame);
        return new Scope(frames, frame);
    }

    private record Frame(
            UUID casterId,
            AbstractSpell spell,
            boolean bypassManaCheck,
            boolean overrideSummonDamage,
            @Nullable ForcedSpellPowerSpellgun powerOverride
    ) {
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, null);
        private final @Nullable Deque<Frame> frames;
        private final @Nullable Frame frame;
        private boolean closed;

        private Scope(@Nullable Deque<Frame> frames, @Nullable Frame frame) {
            this.frames = frames;
            this.frame = frame;
        }

        @Override
        public void close() {
            if (closed || frames == null || frame == null) {
                return;
            }
            closed = true;
            if (frames.peek() == frame) {
                frames.pop();
            } else {
                frames.removeFirstOccurrence(frame);
            }
            if (frames.isEmpty()) {
                FRAMES.remove();
            }
        }
    }
}
