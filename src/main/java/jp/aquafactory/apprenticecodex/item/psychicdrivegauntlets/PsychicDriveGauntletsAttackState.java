package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class PsychicDriveGauntletsAttackState {
    public static final int REPEAT_TICKS = 7;
    private final PsychicDriveGauntletsArm right = new PsychicDriveGauntletsArm(true);
    private final PsychicDriveGauntletsArm left = new PsychicDriveGauntletsArm(false);
    private final PsychicDriveGauntletsTransition transition = new PsychicDriveGauntletsTransition();
    private boolean pressed;
    private boolean usePressed;
    private PsychicDriveGauntletsArm charging;
    private InteractionHand useHand;
    private long useStarted;
    private int chargePaidTicks;
    private int chargeManaCostPerTick;
    private long useSession;
    private boolean useOpen;

    public PsychicDriveGauntletsArm right() { return right; }
    public PsychicDriveGauntletsArm left() { return left; }
    public boolean active() { return !right.available() || !left.available(); }
    public boolean usePressed() { return usePressed; }
    public boolean charging() { return charging != null; }

    public boolean input(Player player, Input input, long time) {
        if (input == Input.RELEASE) {
            pressed = false;
            return false;
        }
        if (!player.isAlive() || player.isSpectator()
                || PsychicDriveGauntletsSelection.duplicate(player)
                || !PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())) {
            pressed = false;
            return false;
        }
        if (input == Input.PRESS) {
            if (pressed) return false;
            pressed = true;
        } else {
            // 単発クリックに間隔制限がないため、連射間隔もclientだけで管理する。
            // serverでも計時するとtickのずれで要求を拒否し、次の7tickまで待たせてしまう。
            if (!pressed) return false;
        }
        var arm = right.available() ? right : left.available() ? left : null;
        if (arm == null) return false;
        int manaCost = ApprenticeCodexServerConfig.psychicDriveGauntletsAttackManaCost();
        if (PsychicDriveGauntletsMana.insufficient(player, manaCost)) return false;
        PsychicDriveGauntletsMana.spend(player, manaCost);
        updateTransition(player, time);
        arm.prepare(player, PsychicDriveGauntletsRaycast.aim(player), transition.amount(time), time);
        return true;
    }

    public void tick(Player player, long time) {
        if (PsychicDriveGauntletsSelection.duplicate(player)) {
            pressed = false;
            cancelUse();
            right.reset();
            left.reset();
            return;
        }
        if (charging != null && (!PsychicDriveGauntletsSelection.canUse(player, useHand)
                || charging.sourceChanged(player) || player.isUsingItem()
                || player.containerMenu != player.inventoryMenu)) {
            cancelUse();
        }
        updateTransition(player, time);
        if (!PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())) pressed = false;
        if (charging != null) spendChargeMana(player, time);
        right.tick(player, transition.amount(time), time);
        left.tick(player, transition.amount(time), time);
    }

    public boolean beginUse(Player player, InteractionHand hand, long time) {
        if (usePressed) return false;
        usePressed = true;
        if (!PsychicDriveGauntletsSelection.canUse(player, hand) || player.isUsingItem()
                || player.containerMenu != player.inventoryMenu) return false;
        var arm = left.available() ? left : right.available() ? right : null;
        if (arm == null) return false;
        chargeManaCostPerTick = ApprenticeCodexServerConfig.psychicDriveGauntletsChargeManaCostPerTick();
        if (PsychicDriveGauntletsMana.insufficient(player, chargeManaCostPerTick * PsychicDriveGauntletsCharge.MAX_TICKS)) return false;
        charging = arm;
        chargePaidTicks = 0;
        useHand = hand;
        useStarted = time;
        charging.reserve(player, hand, time);
        updateTransition(player, time);
        // 予約時にも完成位置を作り、初回同期が原点を描画しないようにする。
        charging.tick(player, 1, time);
        return true;
    }

    public boolean releaseUse(Player player, long time) {
        usePressed = false;
        if (charging != null) spendChargeMana(player, time);
        var arm = charging;
        charging = null;
        if (arm == null) return false;
        if (time - useStarted < PsychicDriveGauntletsCharge.MIN_TICKS
                || !PsychicDriveGauntletsSelection.canUse(player, useHand) || arm.sourceChanged(player)
                || player.isUsingItem() || player.containerMenu != player.inventoryMenu) {
            arm.reset();
            return false;
        }
        float charge = PsychicDriveGauntletsCharge.amount(time - useStarted);
        arm.launch(player, PsychicDriveGauntletsRaycast.aim(player, PsychicDriveGauntletsCharge.range(charge)),
                1, time, useHand, charge);
        return true;
    }

    public void cancelUse() {
        if (charging != null) charging.reset();
        charging = null;
    }

    private void spendChargeMana(Player player, long time) {
        int ticks = Math.clamp(time - useStarted, 0, PsychicDriveGauntletsCharge.MAX_TICKS);
        if (ticks <= chargePaidTicks) return;
        PsychicDriveGauntletsMana.spend(player, (ticks - chargePaidTicks) * chargeManaCostPerTick);
        chargePaidTicks = ticks;
    }

    public boolean useInput(Player player, UseAction action, InteractionHand hand, long session, long time) {
        if (session <= 0) return false;
        if (action == UseAction.START) {
            // 同一押下の再送は計時を戻さず、拒否した押下も終端要求までは記憶する。
            if (useOpen || session <= useSession) {
                return session == useSession && hand == useHand && useOpen && charging();
            }
            useSession = session;
            useHand = hand;
            useOpen = true;
            return beginUse(player, hand, time);
        }
        if (!useOpen || session != useSession || hand != useHand) return false;
        useOpen = false;
        if (action == UseAction.RELEASE) releaseUse(player, time);
        else {
            cancelUse();
            usePressed = false;
        }
        return false;
    }

    public long useSession() { return useSession; }

    private void updateTransition(Player player, long time) {
        var mode = PsychicDriveGauntletsSelection.find(player, true).mode();
        // 最後のアイテムを除去しても、攻撃中の腕は納刀位置への帰還を完了させる。
        transition.update(mode == Mode.HIDDEN && active() ? Mode.SHEATHED : mode, time);
    }

    public enum Input { PRESS, REPEAT, RELEASE }
    public enum UseAction { START, RELEASE, CANCEL }
}
