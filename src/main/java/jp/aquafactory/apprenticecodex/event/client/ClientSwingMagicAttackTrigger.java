package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public final class ClientSwingMagicAttackTrigger {
    private static final EnumMap<InteractionHand, Long> LAST_SENT_TICKS = new EnumMap<>(InteractionHand.class);
    private static final int DEFAULT_MISS_EVALUATION_DELAY_TICKS = 1;
    private static final int VANILLA_PRE_ATTACK_MISS_EVALUATION_DELAY_TICKS = 2;

    private ClientSwingMagicAttackTrigger() {
    }

    public static void trySend(Minecraft minecraft) {
        trySend(minecraft, InteractionHand.MAIN_HAND, false, false, DEFAULT_MISS_EVALUATION_DELAY_TICKS);
    }

    public static void trySendForVanillaPreAttack(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null
                || !CrystalBladedStaff.isCrystalBladedStaff(player.getMainHandItem())
                || !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return;
        }

        // バニラ空振りはクライアント側で攻撃クールダウンを即リセットするため、
        // press 直後に確認したフルチャージ判定を採用してサーバー側の遅延判定へ渡す。
        trySend(minecraft, InteractionHand.MAIN_HAND, true, false, VANILLA_PRE_ATTACK_MISS_EVALUATION_DELAY_TICKS);
    }

    public static void trySendForBetterCombat(Minecraft minecraft, InteractionHand hand) {
        trySend(minecraft, hand, true, true, DEFAULT_MISS_EVALUATION_DELAY_TICKS);
    }

    private static void trySend(
            Minecraft minecraft,
            InteractionHand hand,
            boolean bypassChargeCheck,
            boolean logEmptyHandFailure,
            int missEvaluationDelayTicks
    ) {
        var player = minecraft.player;
        if (!canSend(minecraft, player, hand, bypassChargeCheck, logEmptyHandFailure) || player == null) {
            return;
        }

        LAST_SENT_TICKS.put(hand, player.level().getGameTime());
        ClientSwingcastStaffCastContext.beginPending(player.getUUID(), player.getItemInHand(hand));
        Networks.sendToServer(new ClientSwingMagicAttackPacket(bypassChargeCheck, hand, missEvaluationDelayTicks));
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
        if (!(stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem)) {
            if (logEmptyHandFailure && stack.isEmpty()) {
                ApprenticeCodex.LOGGER.error(
                        "Better Combat swing magic trigger skipped because {} resolved to an empty stack.",
                        hand
                );
            }
            return false;
        }
        if (!swingTriggeredMagicItem.canTriggerSpellOnSwing(player, hand)) {
            return false;
        }

        // Swing 系は地形ヒットもフルスイング入力として扱い、空振り時と同じ発動条件へ寄せる。
        if (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return false;
        }

        return player.level().getGameTime() != LAST_SENT_TICKS.getOrDefault(hand, Long.MIN_VALUE);
    }
}
