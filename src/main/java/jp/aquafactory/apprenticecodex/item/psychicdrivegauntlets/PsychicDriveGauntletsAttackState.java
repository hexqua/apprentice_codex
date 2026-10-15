package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

public final class PsychicDriveGauntletsAttackState {
    public static final int REPEAT_TICKS = 15;
    private final PsychicDriveGauntletsArm right = new PsychicDriveGauntletsArm(true);
    private final PsychicDriveGauntletsArm left = new PsychicDriveGauntletsArm(false);
    private final PsychicDriveGauntletsTransition transition = new PsychicDriveGauntletsTransition();
    private boolean pressed;
    private boolean usePressed;
    private PsychicDriveGauntletsArm charging;
    private InteractionHand useHand;
    private long useStarted;

    public PsychicDriveGauntletsArm right() { return right; }
    public PsychicDriveGauntletsArm left() { return left; }
    public boolean active() { return !right.available() || !left.available(); }
    public boolean usePressed() { return usePressed; }
    public boolean charging() { return charging != null; }

    public boolean input(Player player, Input input, long time) {
        if (input == Input.CANCEL_USE) { cancelUse(); return false; }
        if (input == Input.RESET_USE) {
            if (charging == null) usePressed = false;
            return false;
        }
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
            // serverでも計時するとtickのずれで要求を拒否し、次の15tickまで待たせてしまう。
            if (!pressed) return false;
        }
        var arm = right.available() ? right : left.available() ? left : null;
        if (arm == null) return false;
        updateTransition(player, time);
        arm.launch(player, PsychicDriveGauntletsRaycast.aim(player).point(), transition.amount(time), time);
        return true;
    }

    public void tick(Player player, long time) {
        if (PsychicDriveGauntletsSelection.duplicate(player)) {
            pressed = false;
            cancelUse();
            if (PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())) player.stopUsingItem();
            right.reset();
            left.reset();
            return;
        }
        if (charging != null && (!PsychicDriveGauntletsSelection.canUse(player, useHand)
                || charging.sourceChanged(player))) {
            cancelUse();
            player.stopUsingItem();
        }
        updateTransition(player, time);
        if (!PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())) pressed = false;
        right.tick(player, transition.amount(time), time);
        left.tick(player, transition.amount(time), time);
    }

    public boolean beginUse(Player player, InteractionHand hand, long time) {
        if (usePressed) return false;
        usePressed = true;
        if (!PsychicDriveGauntletsSelection.canUse(player, hand)) return false;
        charging = left.available() ? left : right.available() ? right : null;
        if (charging == null) return false;
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
        var arm = charging;
        charging = null;
        if (arm == null) return false;
        if (time - useStarted < PsychicDriveGauntletsCharge.MIN_TICKS
                || !PsychicDriveGauntletsSelection.canUse(player, useHand) || arm.sourceChanged(player)) {
            arm.reset();
            return false;
        }
        float charge = PsychicDriveGauntletsCharge.amount(time - useStarted);
        arm.launch(player, PsychicDriveGauntletsRaycast.aim(player, PsychicDriveGauntletsCharge.range(charge)).point(),
                1, time, useHand, charge);
        return true;
    }

    public void cancelUse() {
        if (charging != null) charging.reset();
        charging = null;
    }

    private void updateTransition(Player player, long time) {
        var mode = PsychicDriveGauntletsSelection.find(player, true).mode();
        // 最後のアイテムを除去しても、攻撃中の腕は納刀位置への帰還を完了させる。
        transition.update(mode == Mode.HIDDEN && active() ? Mode.SHEATHED : mode, time);
    }

    public enum Input { PRESS, REPEAT, RELEASE, CANCEL_USE, RESET_USE }
}
