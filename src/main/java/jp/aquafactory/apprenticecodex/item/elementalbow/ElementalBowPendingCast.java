package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowCastPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/** 対象データは Iron's に保持させ、自動発動だけを弓の release へ置き換える。 */
public final class ElementalBowPendingCast {
    private static final Map<ServerPlayer, State> PENDING = new WeakHashMap<>();

    private ElementalBowPendingCast() {}

    public static boolean begin(ServerPlayer player, ItemStack stack, InteractionHand hand,
                                AbstractSpell spell, int spellLevel, int requiredTicks) {
        var magic = MagicData.getPlayerMagicData(player);
        if (PENDING.containsKey(player) || magic.isCasting()) return false;
        var state = new State(stack, stack.copy(), hand, player.getInventory().selected, spell, spellLevel,
                player.level().getGameTime(), requiredTicks, player.level().dimension().location().toString());
        PENDING.put(player, state);
        // 開始通知より先に同期し、client の通常詠唱アニメーションと自動カウントダウンを抑止する。
        Networks.sendToTrackingEntityAndSelf(player, new SyncElementalBowCastPacket(player.getUUID(), spell.getSpellId(), true));
        boolean started = false;
        try (var ignored = ElementalBowCasting.open(player, stack, spell)) {
            started = spell.attemptInitiateCast(stack, spellLevel, player.level(), player, CastSource.SWORD, true,
                    hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND);
            if (started) {
                updateDuration(player, state);
                player.startUsingItem(hand);
            }
            return started;
        } finally {
            if (!started) {
                if (ownsMagic(player, state)) cancel(player);
                else {
                    PENDING.remove(player);
                    magic.resetAdditionalCastData();
                    syncEnd(player, state);
                }
            }
        }
    }

    public static boolean shouldBypassMagicManager(MagicData magic) {
        var player = ((MagicDataAccessor) magic).apprenticecodex$getServerPlayer();
        return player != null && PENDING.containsKey(player);
    }

    public static boolean isPending(ServerPlayer player) {
        return PENDING.containsKey(player);
    }

    public static void syncToObserver(ServerPlayer player, ServerPlayer observer) {
        var state = PENDING.get(player);
        if (state != null && valid(player, state)) {
            Networks.sendToPlayer(observer, new SyncElementalBowCastPacket(player.getUUID(), state.spell.getSpellId(), true));
        }
    }

    private static boolean ownsMagic(ServerPlayer player, State state) {
        var magic = MagicData.getPlayerMagicData(player);
        return magic.isCasting() && magic.getPlayerCastingItem() == state.stack
                && state.spell.getSpellId().equals(magic.getCastingSpellId());
    }

    private static boolean valid(ServerPlayer player, State state) {
        return player.isAlive() && !player.isSpectator() && player.isUsingItem()
                && player.getUsedItemHand() == state.hand && player.getUseItem() == state.stack
                && player.getItemInHand(state.hand) == state.stack
                && player.getInventory().selected == state.hotbarSlot
                && ItemStack.isSameItemSameComponents(state.stack, state.snapshot)
                && state.dimension.equals(player.level().dimension().location().toString())
                && state.spell.isEnabled() && ownsMagic(player, state);
    }

    public static void tick(ServerPlayer player) {
        var state = PENDING.get(player);
        if (state == null) return;
        if (!valid(player, state)) {
            cancel(player);
            return;
        }
        updateDuration(player, state);
        try (var ignored = ElementalBowCasting.open(player, state.stack, state.spell)) {
            state.spell.onServerCastTick(player.level(), state.spellLevel, player, MagicData.getPlayerMagicData(player));
        }
    }

    private static void updateDuration(ServerPlayer player, State state) {
        var accessor = (MagicDataAccessor) MagicData.getPlayerMagicData(player);
        accessor.apprenticecodex$setCastDuration(state.requiredTicks);
        accessor.apprenticecodex$setCastDurationRemaining((int) Math.max(0,
                state.requiredTicks - (player.level().getGameTime() - state.startedAt)));
    }

    public static boolean ready(ServerPlayer player, ItemStack stack, int drawTicks) {
        var state = PENDING.get(player);
        return state != null && state.stack == stack && valid(player, state)
                && drawTicks >= state.requiredTicks;
    }

    public static boolean release(ServerPlayer player, ItemStack stack, AbstractSpell spell, int spellLevel, int drawTicks) {
        var state = PENDING.get(player);
        if (!ready(player, stack, drawTicks) || state.spell != spell || state.spellLevel != spellLevel) return false;
        PENDING.remove(player);
        var magic = MagicData.getPlayerMagicData(player);
        boolean cast = false;
        try (var ignored = ElementalBowCasting.open(player, stack, spell)) {
            try {
                spell.castSpell(player.level(), spellLevel, player, CastSource.SWORD, true);
                cast = true;
            } finally {
                spell.onServerCastComplete(player.level(), spellLevel, player, magic, !cast);
            }
        } finally {
            magic.resetAdditionalCastData();
            syncEnd(player, state);
        }
        return true;
    }

    public static void cancel(ServerPlayer player) {
        var state = PENDING.remove(player);
        if (state == null) return;
        // 別詠唱に所有権が移った場合、その状態と対象データには触れない。
        try {
            if (ownsMagic(player, state)) {
                var magic = MagicData.getPlayerMagicData(player);
                try (var ignored = ElementalBowCasting.open(player, state.stack, state.spell)) {
                    state.spell.onServerCastComplete(player.level(), state.spellLevel, player, magic, true);
                } finally {
                    magic.resetAdditionalCastData();
                }
            }
        } finally {
            if (player.getUseItem() == state.stack) player.stopUsingItem();
            syncEnd(player, state);
        }
    }

    /** 外部の中断経路が既に終了通知を実行する場合、二重通知せず弓の所有状態だけ解除する。 */
    public static void onSpellComplete(ServerPlayer player, AbstractSpell spell) {
        var state = PENDING.get(player);
        if (state == null || state.spell != spell || !ownsMagic(player, state)) return;
        PENDING.remove(player);
        MagicData.getPlayerMagicData(player).resetAdditionalCastData();
        if (player.getUseItem() == state.stack) player.stopUsingItem();
        syncEnd(player, state);
    }

    private static void syncEnd(ServerPlayer player, State state) {
        Networks.sendToTrackingEntityAndSelf(player, new SyncElementalBowCastPacket(player.getUUID(), state.spell.getSpellId(), false));
    }

    private record State(ItemStack stack, ItemStack snapshot, InteractionHand hand, int hotbarSlot,
                         AbstractSpell spell, int spellLevel, long startedAt, int requiredTicks, String dimension) {}
}
