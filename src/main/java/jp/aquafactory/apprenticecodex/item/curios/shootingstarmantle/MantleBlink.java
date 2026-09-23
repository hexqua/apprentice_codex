package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import static io.redspace.ironsspellbooks.registries.SoundRegistry.ABYSSAL_TELEPORT;

/** 移動と描画で共有する時系列。回復通知のblinkTicksとは独立させる。 */
public final class MantleBlink {
    public static final int DISAPPEAR_TICKS = 3;
    public static final int APPEAR_TICK = 7;
    public static final int DURATION = 10;
    public static final double SPEED = 1.5;
    private long start = -1;
    private long previousStart = -1;
    private long sequence = -1;
    private double height;
    private Vec3 direction = Vec3.ZERO;
    private boolean blocked;
    private boolean finished;
    private boolean appearanceSound;
    private long lastMoveTime = Long.MIN_VALUE;

    public long start() { return start; }
    public long sequence() { return sequence; }
    public double height() { return height; }
    public Vec3 direction() { return direction; }

    public double elapsed(long time, float partialTick) {
        return start < 0 ? -1 : time - start + partialTick;
    }

    public double renderElapsed(long time, float partialTick) {
        double age = elapsed(time, partialTick);
        // 連続発動の通知が先に届いても、位置補間中の前回の出現演出を最後まで描く。
        return start >= 0 && age < 0 && previousStart >= 0 ? time - previousStart + partialTick : age;
    }

    public boolean active(long time) {
        double age = elapsed(time, 0);
        return age >= 0 && age < DURATION;
    }

    public static boolean moving(double age) {
        return age >= DISAPPEAR_TICKS && age < APPEAR_TICK;
    }

    public void begin(Player player, long id, Vec3 movement) {
        accept(player.level().getGameTime(), id, player.getY(), movement);
        player.setDeltaMovement(Vec3.ZERO);
        if (player instanceof ServerPlayer) playSound(player);
    }

    public void accept(long startTime, long id, double y, Vec3 movement) {
        if (sequence != id) previousStart = start;
        start = startTime;
        sequence = id;
        height = y;
        direction = movement;
        blocked = false;
        finished = false;
        appearanceSound = false;
        lastMoveTime = Long.MIN_VALUE;
    }

    public void cancel() {
        start = -1;
        previousStart = -1;
        finished = true;
        blocked = false;
    }

    public void update(Player player, ShootingStarMantleRuntime.State state) {
        if (start < 0 || finished) return;
        double age = elapsed(player.level().getGameTime(), 0);
        if (age >= APPEAR_TICK && !appearanceSound) {
            appearanceSound = true;
            if (player instanceof ServerPlayer && age < DURATION) playSound(player);
        }
        if (age >= DURATION) {
            finished = true;
            player.setDeltaMovement(Vec3.ZERO);
            state.lastPosition = null;
            state.movingTicks = 0;
        } else if (age >= 0) {
            // travelを実行しないserver側でも、被弾等から持ち越した速度を残さない。
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
        }
    }

    public boolean travel(Player player, ShootingStarMantleRuntime.State state) {
        update(player, state);
        long time = player.level().getGameTime();
        if (!active(time)) return false;
        if (lastMoveTime == time) return true;
        lastMoveTime = time;
        // 外部転送の高さを発動位置へ戻さない。通常の補正packetも専用Mixinで中断する。
        if (Math.abs(player.getY() - height) > 0.01) {
            cancel();
            return false;
        }
        if (moving(elapsed(time, 0)) && !blocked) {
            var before = player.position();
            var movement = direction.scale(SPEED);
            // 水平衝突時のstep-upを禁止し、10tickの高度固定を維持する。
            player.setOnGround(false);
            player.setDeltaMovement(movement);
            player.move(MoverType.SELF, movement);
            blocked = player.position().subtract(before).distanceToSqr(movement) > 1.0e-8;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        state.lastPosition = null;
        state.movingTicks = 0;
        return true;
    }

    public static boolean cancelIncomingDamageIfInvulnerable(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ShootingStarMantleRuntime.isHovering(player)
                && ShootingStarMantleRuntime.state(player).blink.active(player.level().getGameTime())) {
            event.setCanceled(true);
            return true;
        }
        return false;
    }

    private static void playSound(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ABYSSAL_TELEPORT.get(), SoundSource.PLAYERS, 1, 1);
    }
}
