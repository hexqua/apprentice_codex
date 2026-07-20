package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 右クリック入力と Iron's の cast-start / cast-finished packet を結び付けるローカル詠唱状態。 */
public final class ChargecastCatalystbookClientCastIntent {
    private static final long INTENT_LIFETIME_MILLIS = 1_000L;
    private static @Nullable PendingCast pending;
    private static @Nullable ActiveCast active;

    private ChargecastCatalystbookClientCastIntent() {
    }

    public static void mark(UUID casterId, ItemStack stack, AbstractSpell spell) {
        pending = new PendingCast(
                casterId,
                stack.copy(),
                spell.getSpellId(),
                Util.getMillis() + INTENT_LIFETIME_MILLIS
        );
    }

    /** cast-start packet を受け取った時だけ pending を active に遷移させる。 */
    public static boolean activateIfMatches(UUID casterId, ItemStack stack, @Nullable AbstractSpell spell) {
        var pendingCast = pending;
        if (pendingCast == null || spell == null) {
            return false;
        }
        if (Util.getMillis() > pendingCast.expiresAtMillis()) {
            pending = null;
            return false;
        }
        if (!pendingCast.matches(casterId, stack, spell.getSpellId())) {
            return false;
        }
        active = new ActiveCast(casterId, stack.copy(), spell.getSpellId());
        pending = null;
        return true;
    }

    public static boolean isActive(UUID casterId, ItemStack stack, @Nullable AbstractSpell spell) {
        return active != null && spell != null && active.matches(casterId, stack, spell.getSpellId());
    }

    public static boolean isActive(UUID casterId, @Nullable AbstractSpell spell) {
        return active != null && spell != null && active.matches(casterId, spell.getSpellId());
    }

    /** cast-finished packet の識別子が現在のローカル詠唱と一致した場合だけ終了する。 */
    public static void finishIfMatches(UUID casterId, String spellId) {
        if (active != null && active.matches(casterId, spellId)) {
            active = null;
        }
    }

    public static void clear() {
        pending = null;
        active = null;
    }

    private record PendingCast(UUID casterId, ItemStack stack, String spellId, long expiresAtMillis) {
        private boolean matches(UUID candidateCasterId, ItemStack candidateStack, String candidateSpellId) {
            return casterId.equals(candidateCasterId)
                    && spellId.equals(candidateSpellId)
                    && ItemStack.isSameItemSameTags(stack, candidateStack);
        }
    }

    private record ActiveCast(UUID casterId, ItemStack stack, String spellId) {
        private boolean matches(UUID candidateCasterId, ItemStack candidateStack, String candidateSpellId) {
            return matches(candidateCasterId, candidateSpellId)
                    && ItemStack.isSameItemSameTags(stack, candidateStack);
        }

        private boolean matches(UUID candidateCasterId, String candidateSpellId) {
            return casterId.equals(candidateCasterId) && spellId.equals(candidateSpellId);
        }
    }
}
