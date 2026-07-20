package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 右クリック入力と直後の Iron's cast-start packet をクライアント内で結び付ける短命な状態。 */
public final class ChargecastCatalystbookClientCastIntent {
    private static final long INTENT_LIFETIME_MILLIS = 1_000L;
    private static ItemStack stack = ItemStack.EMPTY;
    private static String spellId = "";
    private static @Nullable UUID casterId;
    private static long expiresAtMillis;
    private static ItemStack activeStack = ItemStack.EMPTY;
    private static String activeSpellId = "";
    private static @Nullable UUID activeCasterId;

    private ChargecastCatalystbookClientCastIntent() {
    }

    public static void mark(UUID casterId, ItemStack stack, AbstractSpell spell) {
        ChargecastCatalystbookClientCastIntent.casterId = casterId;
        ChargecastCatalystbookClientCastIntent.stack = stack.copy();
        spellId = spell.getSpellId();
        expiresAtMillis = Util.getMillis() + INTENT_LIFETIME_MILLIS;
    }

    public static boolean matches(ItemStack candidate, @Nullable AbstractSpell spell) {
        return matches(casterId, candidate, spell);
    }

    public static boolean matches(@Nullable UUID candidateCasterId, ItemStack candidate, @Nullable AbstractSpell spell) {
        if (spell == null || Util.getMillis() > expiresAtMillis) {
            clearPending();
            return false;
        }
        var matches = casterId != null && casterId.equals(candidateCasterId)
                && spellId.equals(spell.getSpellId()) && ItemStack.isSameItemSameTags(stack, candidate);
        if (matches) {
            activeStack = candidate.copy();
            activeSpellId = spell.getSpellId();
            activeCasterId = casterId;
        }
        return matches;
    }

    public static boolean matchesActive(ItemStack candidate, @Nullable AbstractSpell spell) {
        return matchesActive(activeCasterId, candidate, spell);
    }

    public static boolean matchesActive(@Nullable UUID candidateCasterId, ItemStack candidate, @Nullable AbstractSpell spell) {
        return spell != null && activeSpellId.equals(spell.getSpellId())
                && activeCasterId != null && activeCasterId.equals(candidateCasterId)
                && ItemStack.isSameItemSameTags(activeStack, candidate);
    }

    public static boolean matchesActive(@Nullable AbstractSpell spell) {
        return matchesActive(activeCasterId, spell);
    }

    public static boolean matchesActive(@Nullable UUID candidateCasterId, @Nullable AbstractSpell spell) {
        return spell != null && activeCasterId != null && activeCasterId.equals(candidateCasterId)
                && activeSpellId.equals(spell.getSpellId()) && !activeStack.isEmpty();
    }

    public static void clearActive() {
        activeStack = ItemStack.EMPTY;
        activeSpellId = "";
        activeCasterId = null;
    }

    public static void clearActiveIfMatches(@Nullable UUID candidateCasterId, String candidateSpellId) {
        if (activeCasterId != null && activeCasterId.equals(candidateCasterId)
                && activeSpellId.equals(candidateSpellId)) {
            clearActive();
        }
    }

    public static void clear() {
        clearPending();
        clearActive();
    }

    private static void clearPending() {
        stack = ItemStack.EMPTY;
        spellId = "";
        casterId = null;
        expiresAtMillis = 0L;
    }
}
