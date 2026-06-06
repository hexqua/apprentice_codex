package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

public final class ClientSwingMagicAttackTrigger {
    private static long lastSentTick = Long.MIN_VALUE;

    private ClientSwingMagicAttackTrigger() {
    }

    public static void trySend(Minecraft minecraft) {
        trySend(minecraft, InteractionHand.MAIN_HAND, false, false);
    }

    public static void trySendForBetterCombat(Minecraft minecraft, InteractionHand hand) {
        trySend(minecraft, hand, true, true);
    }

    private static void trySend(
            Minecraft minecraft,
            InteractionHand hand,
            boolean bypassChargeCheck,
            boolean logEmptyHandFailure
    ) {
        var player = minecraft.player;
        if (!canSend(minecraft, player, hand, bypassChargeCheck, logEmptyHandFailure) || player == null) {
            return;
        }

        lastSentTick = player.level().getGameTime();
        ClientSwingcastStaffCastContext.beginPending(player.getUUID(), player.getItemInHand(hand));
        Networks.sendToServer(new ClientSwingMagicAttackPacket(bypassChargeCheck, hand));
    }

    private static boolean canSend(
            Minecraft minecraft,
            @Nullable LocalPlayer player,
            InteractionHand hand,
            boolean bypassChargeCheck,
            boolean logEmptyHandFailure
    ) {
        if (minecraft.screen != null) {
            return false;
        }

        if (player == null || player.isSpectator()) {
            return false;
        }

        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SwingTriggeredMagicItem)) {
            if (logEmptyHandFailure && stack.isEmpty()) {
                ApprenticeCodex.LOGGER.error(
                        "Better Combat swing magic trigger skipped because {} resolved to an empty stack.",
                        hand
                );
            }
            return false;
        }

        // Swing 系は地形ヒットもフルスイング入力として扱い、空振り時と同じ発動条件へ寄せる。
        if (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return false;
        }

        return player.level().getGameTime() != lastSentTick;
    }
}
