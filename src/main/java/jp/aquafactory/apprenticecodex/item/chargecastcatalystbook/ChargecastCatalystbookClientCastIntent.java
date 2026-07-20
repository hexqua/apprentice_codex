package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 右クリック入力と直後の Iron's cast-start packet をクライアント内で結び付ける短命な状態。 */
public final class ChargecastCatalystbookClientCastIntent {
    private static final long INTENT_LIFETIME_MILLIS = 1_000L;
    private static ItemStack stack = ItemStack.EMPTY;
    private static String spellId = "";
    private static long expiresAtMillis;
    private static ItemStack activeStack = ItemStack.EMPTY;
    private static String activeSpellId = "";

    private ChargecastCatalystbookClientCastIntent() {
    }

    public static void mark(ItemStack stack, AbstractSpell spell) {
        ChargecastCatalystbookClientCastIntent.stack = stack.copy();
        spellId = spell.getSpellId();
        expiresAtMillis = Util.getMillis() + INTENT_LIFETIME_MILLIS;
    }

    public static boolean matches(ItemStack candidate, @Nullable AbstractSpell spell) {
        if (spell == null || Util.getMillis() > expiresAtMillis) {
            clearPending();
            return false;
        }
        var matches = spellId.equals(spell.getSpellId()) && ItemStack.isSameItemSameTags(stack, candidate);
        if (matches) {
            activeStack = candidate.copy();
            activeSpellId = spell.getSpellId();
        }
        return matches;
    }

    public static boolean matchesActive(ItemStack candidate, @Nullable AbstractSpell spell) {
        return spell != null && activeSpellId.equals(spell.getSpellId())
                && ItemStack.isSameItemSameTags(activeStack, candidate);
    }

    public static boolean matchesActive(@Nullable AbstractSpell spell) {
        return spell != null && activeSpellId.equals(spell.getSpellId()) && !activeStack.isEmpty();
    }

    public static void clearActive() {
        activeStack = ItemStack.EMPTY;
        activeSpellId = "";
    }

    public static void clear() {
        clearPending();
        clearActive();
    }

    private static void clearPending() {
        stack = ItemStack.EMPTY;
        spellId = "";
        expiresAtMillis = 0L;
    }
}
