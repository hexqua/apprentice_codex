package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public final class ClientSwingMagicAttackTrigger {
    private static long lastSentTick = Long.MIN_VALUE;

    private ClientSwingMagicAttackTrigger() {
    }

    public static void trySend(Minecraft minecraft) {
        trySend(minecraft, false);
    }

    public static void trySendForBetterCombat(Minecraft minecraft) {
        trySend(minecraft, true);
    }

    private static void trySend(Minecraft minecraft, boolean bypassChargeCheck) {
        var player = minecraft.player;
        if (!canSend(minecraft, player, bypassChargeCheck)) {
            return;
        }

        lastSentTick = player.level().getGameTime();
        ClientSwingcastStaffCastContext.beginPending(player.getUUID(), player.getMainHandItem());
        Networks.sendToServer(new ClientSwingMagicAttackPacket(bypassChargeCheck));
    }

    private static boolean canSend(Minecraft minecraft, @Nullable LocalPlayer player, boolean bypassChargeCheck) {
        if (minecraft.screen != null) {
            return false;
        }

        if (player == null || player.isSpectator() || !(player.getMainHandItem().getItem() instanceof AbstractSwingMagicItem)) {
            return false;
        }

        // Swing 系は地形ヒットもフルスイング入力として扱い、空振り時と同じ発動条件へ寄せる。
        if (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return false;
        }

        return player.level().getGameTime() != lastSentTick;
    }
}
