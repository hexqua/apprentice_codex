package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsArm {
    public static final int HOLD_TICKS = 10;
    public static final int RETURN_TICKS = 5;
    public enum Phase { IDLE, OUTBOUND, HOLDING, RETURNING, CHARGING }
    public record Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                           Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge) {
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick, 0);
        }
    }

    private final boolean right;
    private Phase phase = Phase.IDLE;
    private Vec3 position = Vec3.ZERO;
    private Vec3 destination = Vec3.ZERO;
    private Vec3 flightStart = Vec3.ZERO;
    private long launchTick;
    private Vec3 returnStart = Vec3.ZERO;
    private ItemStack original = ItemStack.EMPTY;
    private ItemStack weapon = ItemStack.EMPTY;
    private int slot;
    private long phaseStarted;
    private long lastTick = Long.MIN_VALUE;
    private float damage;
    private float yaw;
    private float pitch;
    private float returnYaw;
    private float returnPitch;
    private float drawnAmount = 1.0F;
    private float returnDrawnAmount;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private float charge;

    public PsychicDriveGauntletsArm(boolean right) { this.right = right; }
    public boolean available() { return phase == Phase.IDLE; }
    public Phase phase() { return phase; }
    public Vec3 position() { return position; }
    public Snapshot snapshot() {
        return new Snapshot(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, lastTick, charge);
    }

    public void reserve(Player owner, InteractionHand usedHand, long time) {
        hand = usedHand;
        original = owner.getItemInHand(hand);
        slot = owner.getInventory().selected;
        phase = Phase.CHARGING;
        launchTick = time;
        lastTick = time - 1;
    }

    public boolean sourceChanged(Player owner) {
        return owner.getItemInHand(hand) != original
                || (hand == InteractionHand.MAIN_HAND && owner.getInventory().selected != slot);
    }

    public void reset() {
        phase = Phase.IDLE;
        original = ItemStack.EMPTY;
        weapon = ItemStack.EMPTY;
        charge = 0;
    }

    public void launch(Player owner, Vec3 target, float amount, long time) {
        launch(owner, target, amount, time, InteractionHand.MAIN_HAND, 0);
    }

    public void launch(Player owner, Vec3 target, float amount, long time, InteractionHand usedHand, float charged) {
        if (!available() && phase != Phase.CHARGING) return;
        hand = usedHand;
        original = owner.getItemInHand(hand);
        weapon = original.copy();
        slot = owner.getInventory().selected;
        damage = PsychicDriveGauntletsCharge.attackDamage(owner, original);
        charge = charged;
        position = PsychicDriveGauntletsPose.position(owner, amount, right);
        if (charged > 0) position = position.add(PsychicDriveGauntletsCharge.offset(charged * PsychicDriveGauntletsCharge.MAX_TICKS)
                .yRot((180 - owner.getYRot()) * Mth.DEG_TO_RAD));
        flightStart = position;
        launchTick = time;
        destination = target;
        var direction = target.subtract(position);
        yaw = (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
        pitch = (float) (-Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
        drawnAmount = 1.0F;
        phase = Phase.OUTBOUND;
        phaseStarted = time;
        lastTick = time;
    }

    public void recall(long time) {
        if (phase == Phase.IDLE || phase == Phase.RETURNING) return;
        phase = Phase.RETURNING;
        phaseStarted = time;
        returnStart = position;
        returnYaw = yaw;
        returnPitch = pitch;
        returnDrawnAmount = drawnAmount;
    }

    public void tick(Player owner, float amount, long time) {
        if (available()) return;
        if (sourceChanged(owner)) {
            if (phase == Phase.CHARGING) { reset(); return; }
            recall(time);
        }
        if (time <= lastTick) return;
        lastTick = time;
        switch (phase) {
            case CHARGING -> {
                charge = PsychicDriveGauntletsCharge.amount(time - launchTick);
                drawnAmount = 1;
                position = PsychicDriveGauntletsPose.position(owner, 1, right)
                        .add(PsychicDriveGauntletsCharge.offset(time - launchTick)
                                .yRot((180 - owner.getYRot()) * Mth.DEG_TO_RAD));
                yaw = owner.getYRot();
                pitch = 0;
            }
            case OUTBOUND -> {
                var next = PsychicDriveGauntletsMotion.outbound(flightStart, destination, (double) time - launchTick, charge);
                // 飛行中は壁を再確認しない。埋まった腕や、発射後に遮蔽へ隠れた対象への接触を許容する。
                var contact = PsychicDriveGauntletsRaycast.firstContact(owner, position, next, false);
                if (contact != null) {
                    position = contact.point();
                    hit(owner, contact.target());
                    phase = Phase.HOLDING;
                    phaseStarted = time;
                } else {
                    position = next;
                    if (position.distanceToSqr(destination) < 1.0e-12) {
                        phase = Phase.HOLDING;
                        phaseStarted = time;
                    }
                }
            }
            case HOLDING -> {
                if (time - phaseStarted >= HOLD_TICKS) recall(time);
            }
            case RETURNING -> {
                float progress = Mth.clamp((float) (time - phaseStarted) / RETURN_TICKS, 0.0F, 1.0F);
                position = returnStart.lerp(PsychicDriveGauntletsPose.position(owner, amount, right), progress);
                yaw = Mth.rotLerp(progress, returnYaw, Mth.rotLerp(amount, owner.yBodyRot, owner.getYRot()));
                pitch = Mth.lerp(progress, returnPitch, 0.0F);
                drawnAmount = Mth.lerp(progress, returnDrawnAmount, amount);
                if (progress == 1.0F) {
                    phase = Phase.IDLE;
                    original = ItemStack.EMPTY;
                    weapon = ItemStack.EMPTY;
                }
            }
            default -> {}
        }
    }

    private void hit(Player owner, Entity target) {
        var level = (ServerLevel) owner.level();
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS);
        // 実体弾を持たないため、接触位置を明示して盾の方向判定をプレイヤー位置へ戻さない。
        var source = new DamageSource(holder, owner, owner, position);
        var amount = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage)
                * PsychicDriveGauntletsCharge.damageMultiplier(charge);
        if (CombatTools.applyUnscaledDamage(target, amount, source, CombatTools.KnockbackTypes.DEFAULT)
                && target instanceof LivingEntity living) {
            // 追加分は命中成功時だけ適用し、耐性とLivingKnockBackEventは通常のknockbackへ委ねる。
            var knockback = EnchantmentHelper.modifyKnockback(level, weapon, target, source, 0) + charge;
            if (knockback > 0) {
                var direction = destination.subtract(flightStart).normalize();
                living.knockback(knockback * 0.5, -direction.x, -direction.z);
            }
        }
    }
}
