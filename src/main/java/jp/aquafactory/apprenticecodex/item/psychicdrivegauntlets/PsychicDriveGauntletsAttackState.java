package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.world.entity.player.Player;

public final class PsychicDriveGauntletsAttackState {
    public static final int REPEAT_TICKS = 15;
    private final PsychicDriveGauntletsArm right = new PsychicDriveGauntletsArm(true);
    private final PsychicDriveGauntletsArm left = new PsychicDriveGauntletsArm(false);
    private final PsychicDriveGauntletsTransition transition = new PsychicDriveGauntletsTransition();
    private boolean pressed;

    public PsychicDriveGauntletsArm right() { return right; }
    public PsychicDriveGauntletsArm left() { return left; }
    public boolean active() { return !right.available() || !left.available(); }

    public boolean input(Player player, Input input, long time) {
        if (input == Input.RELEASE) {
            pressed = false;
            return false;
        }
        if (!player.isAlive() || player.isSpectator()
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
        updateTransition(player, time);
        if (!PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())) pressed = false;
        right.tick(player, transition.amount(time), time);
        left.tick(player, transition.amount(time), time);
    }

    private void updateTransition(Player player, long time) {
        var mode = PsychicDriveGauntletsSelection.find(player, true).mode();
        // 最後のアイテムを除去しても、攻撃中の腕は納刀位置への帰還を完了させる。
        transition.update(mode == Mode.HIDDEN && active() ? Mode.SHEATHED : mode, time);
    }

    public enum Input { PRESS, REPEAT, RELEASE }
}
