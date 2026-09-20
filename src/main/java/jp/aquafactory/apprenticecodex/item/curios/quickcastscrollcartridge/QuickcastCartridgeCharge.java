package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.QuickcastCartridgeChargeState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.packet.SyncQuickcastCartridgePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import io.redspace.ironsspellbooks.setup.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** サーバーが確認期限とリロードを管理する。座標差は外力と区別できないため判定に使わない。 */
public final class QuickcastCartridgeCharge {
    private static final Map<ServerPlayer, RuntimeState> STATES = new WeakHashMap<>();
    private QuickcastCartridgeCharge() {}

    public static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    public static QuickcastCartridgeChargeState state(ServerPlayer player) {
        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.QUICKCAST_CARTRIDGE_CHARGE_STATE);
        state.repairAfterLoad(now(player));
        return state;
    }

    private static RuntimeState runtime(ServerPlayer player) {
        return STATES.computeIfAbsent(player, ignored -> new RuntimeState());
    }

    public static boolean isReloading(ServerPlayer player) { return runtime(player).reloadUntil > 0; }

    public static void clearConfirmation(ServerPlayer player) { runtime(player).confirmationAt = -1; }

    public static void selectionChanged(ServerPlayer player) {
        clearConfirmation(player);
        interruptReload(player);
    }

    public static void requestReload(ServerPlayer player) {
        tick(player);
        var runtime = runtime(player);
        long now = now(player);
        if (runtime.confirmationAt == now) return;
        if (runtime.confirmationAt >= 0 && now - runtime.confirmationAt <= 20) {
            runtime.confirmationAt = -1;
            if (QuickcastCartridgeCasting.hasReservation(player)) return;
            var magic = MagicData.getPlayerMagicData(player);
            if (magic.isCasting()) Utils.serverSideCancelCast(player);
            if (magic.isCasting()) return;
            player.stopUsingItem();
            runtime.reloadDuration = ApprenticeCodexServerConfig.quickcastCartridge().reloadTicks();
            runtime.reloadUntil = QuickcastCartridgeChargeState.addTime(now, runtime.reloadDuration);
            QuickcastCartridgeReloadEffects.start(player);
            runtime.lastParticleAt = now;
            sync(player, false, false);
        } else {
            runtime.confirmationAt = now;
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.quickcast_scroll_cartridge.warning_manual_reload").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    public static void interruptReload(ServerPlayer player) {
        var runtime = runtime(player);
        if (runtime.reloadUntil == 0) return;
        // 境界tickに届いた入力でも、既に完了した回復は取り消さない。
        tick(player);
        runtime.reloadUntil = 0;
        runtime.reloadDuration = 0;
        runtime.confirmationAt = -1;
        sync(player, false, false);
    }

    public static void equipmentChanged(ServerPlayer player) {
        var runtime = runtime(player);
        runtime.confirmationAt = -1;
        runtime.reloadUntil = 0;
        runtime.reloadDuration = 0;
        // CuriosはアニメーションID追加でも変更通知する。開始時に保持した実個体・内容で判定する。
        QuickcastCartridgeCasting.validate(player);
        observeEquipment(player, runtime);
        sync(player, false, true);
    }

    private static void observeEquipment(ServerPlayer player, RuntimeState runtime) {
        var stack = QuickcastCartridgeCasting.findEquipped(player);
        runtime.equipped = stack;
        runtime.equipmentSnapshot = stack.copy();
    }

    public static void tick(ServerPlayer player) {
        var runtime = runtime(player);
        var stack = QuickcastCartridgeCasting.findEquipped(player);
        if (runtime.equipped != stack || !ItemStack.isSameItemSameTags(runtime.equipmentSnapshot, stack)) {
            runtime.confirmationAt = -1;
            runtime.reloadUntil = 0;
            runtime.reloadDuration = 0;
            observeEquipment(player, runtime);
        }
        long now = now(player);
        if (runtime.confirmationAt >= 0 && now - runtime.confirmationAt > 20) runtime.confirmationAt = -1;
        var state = state(player);
        boolean completed = (state.recoveryUntil() > 0 && state.available(now))
                || (runtime.reloadUntil > 0 && now >= runtime.reloadUntil);
        if (completed) {
            // 手動中の自動回復先着も完了扱いにする。状態を消す前に一度だけ演出する。
            boolean manual = runtime.reloadUntil > 0;
            state.recover();
            runtime.confirmationAt = -1;
            runtime.reloadUntil = 0;
            runtime.reloadDuration = 0;
            if (manual) QuickcastCartridgeReloadEffects.complete(player);
        } else if (runtime.reloadUntil > 0 && now - runtime.lastParticleAt >= 4) {
            runtime.lastParticleAt = now;
            QuickcastCartridgeReloadEffects.charging(player);
        }
        sync(player, completed && runtime.last != null && runtime.last.equipped() && !runtime.last.available(), false);
    }

    public static void sync(ServerPlayer player, boolean completed, boolean force) {
        var runtime = runtime(player);
        var state = state(player);
        var snapshot = new Snapshot(!QuickcastCartridgeCasting.findEquipped(player).isEmpty(),
                state.available(now(player)), QuickcastCartridgeCasting.hasReservation(player),
                state.recoveryUntil(), state.recoveryDuration(), runtime.reloadUntil, runtime.reloadDuration);
        if (!force && !completed && snapshot.equals(runtime.last)) return;
        runtime.last = snapshot;
        jp.aquafactory.apprenticecodex.network.Networks.sendToPlayer(player, new SyncQuickcastCartridgePacket(snapshot.equipped(),
                snapshot.available(), snapshot.reserved(), now(player), snapshot.recoveryUntil(),
                snapshot.recoveryDuration(), snapshot.reloadUntil(), snapshot.reloadDuration(), completed));
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player);
        QuickcastCartridgeCasting.clear(player);
    }

    private record Snapshot(boolean equipped, boolean available, boolean reserved, long recoveryUntil,
                            long recoveryDuration, long reloadUntil, long reloadDuration) {}

    private static final class RuntimeState {
        ItemStack equipped = ItemStack.EMPTY;
        ItemStack equipmentSnapshot = ItemStack.EMPTY;
        long confirmationAt = -1;
        long reloadUntil;
        long reloadDuration;
        long lastParticleAt;
        Snapshot last;
    }
}
