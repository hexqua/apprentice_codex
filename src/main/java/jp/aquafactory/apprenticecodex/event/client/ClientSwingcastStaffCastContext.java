package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffClientRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class ClientSwingcastStaffCastContext {
    private static final long PENDING_EXPIRE_TICKS = 5L;

    @Nullable
    private static PendingContext pendingContext;
    @Nullable
    private static ActiveContext activeContext;

    private ClientSwingcastStaffCastContext() {
    }

    public static void beginPending(UUID playerId, ItemStack stack) {
        if (stack.isEmpty()) {
            clear();
            return;
        }

        pendingContext = new PendingContext(playerId, stack.getItem(), resolveGameTime(), resolvePendingSpellId(stack));
    }

    public static void tryActivate(UUID playerId, ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null || stack.isEmpty()) {
            return;
        }

        var pending = pendingContext;
        if (pending == null || !pending.playerId().equals(playerId) || pending.item() != stack.getItem()) {
            return;
        }

        var gameTime = resolveGameTime();
        if (gameTime < 0L || gameTime - pending.gameTime() > PENDING_EXPIRE_TICKS) {
            pendingContext = null;
            return;
        }

        if (!matchesPendingSpell(stack, pending, spell)) {
            return;
        }

        activeContext = new ActiveContext(playerId, stack.getItem(), spell.getSpellId());
        pendingContext = null;
    }

    public static boolean matches(UUID playerId, ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null || stack.isEmpty()) {
            return false;
        }

        var active = activeContext;
        return active != null
                && active.playerId().equals(playerId)
                && active.item() == stack.getItem()
                && active.spellId().equals(spell.getSpellId());
    }

    public static void clearFinished(UUID playerId, String spellId) {
        var active = activeContext;
        if (active != null && active.playerId().equals(playerId) && active.spellId().equals(spellId)) {
            activeContext = null;
        }
    }

    public static void clear() {
        pendingContext = null;
        activeContext = null;
    }

    private static boolean matchesPrimarySpell(ItemStack stack, AbstractSpell spell) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData != SpellData.EMPTY && Objects.equals(spellData.getSpell(), spell);
    }

    private static boolean matchesPendingSpell(ItemStack stack, PendingContext pending, AbstractSpell spell) {
        if (stack.getItem() instanceof MithrilFreecastStaff) {
            return Objects.equals(pending.spellId(), spell.getSpellId());
        }

        return matchesPrimarySpell(stack, spell);
    }

    @Nullable
    private static String resolvePendingSpellId(ItemStack stack) {
        if (stack.getItem() instanceof MithrilFreecastStaff) {
            return MithrilFreecastStaffClientRenderState.resolveSelectedSpellId();
        }

        return null;
    }

    private static long resolveGameTime() {
        var minecraft = Minecraft.getInstance();
        return minecraft.level != null ? minecraft.level.getGameTime() : -1L;
    }

    private record PendingContext(UUID playerId, Item item, long gameTime, @Nullable String spellId) {
    }

    private record ActiveContext(UUID playerId, Item item, String spellId) {
    }
}
