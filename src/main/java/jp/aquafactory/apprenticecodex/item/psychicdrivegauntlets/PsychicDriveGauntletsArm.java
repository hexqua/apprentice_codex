package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect.Kind;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PsychicDriveGauntletsArm {
    public static final int HOLD_TICKS = 2;
    public static final int MISS_TICKS = 3;
    public static final int RETURN_TICKS = 5;
    public static final int DRILL_TICKS = 12;
    private static final int HIT_INTERVAL = 4;
    private static final double DRILL_DISTANCE = 0.2;
    public enum Phase { IDLE, OUTBOUND, HOLDING, RETURNING, CHARGING, PREPARING, FOLLOW_THROUGH }
    public enum Impact { NONE, ENTITY, BLOCK }
    // serverでは整数tick、描画用snapshotではclient時計へ写した小数tickを保持する。
    public record MotionData(double phaseStartedTick, Vec3 returnStart, Vec3 returnEnd, Vec3 returnSide,
                             float returnPower, float returnTwist) {}
    public record Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                           Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge,
                           long recoveryStartedTick, float returnRecovery, Impact impact, long impactTick, int holdTicks,
                           MotionData motion) {
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge,
                        long recoveryStartedTick, float returnRecovery, Impact impact, long impactTick, int holdTicks) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick,
                    charge, recoveryStartedTick, returnRecovery, impact, impactTick, holdTicks,
                    new MotionData(phase == Phase.HOLDING || phase == Phase.RETURNING || phase == Phase.FOLLOW_THROUGH ? recoveryStartedTick : launchTick,
                            position, position, Vec3.ZERO, 1, 0));
        }
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge,
                        long recoveryStartedTick, float returnRecovery, Impact impact, long impactTick) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick,
                    charge, recoveryStartedTick, returnRecovery, impact, impactTick, HOLD_TICKS);
        }
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge,
                        long recoveryStartedTick, float returnRecovery) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick,
                    charge, recoveryStartedTick, returnRecovery, Impact.NONE, 0);
        }
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick, float charge) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick, charge, 0, 0);
        }
        public Snapshot(Phase phase, Vec3 position, float yaw, float pitch, float drawnAmount,
                        Vec3 flightStart, Vec3 destination, long launchTick, long sampleTick) {
            this(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, sampleTick, 0);
        }
    }

    private final boolean right;
    private Phase phase = Phase.IDLE;
    private Vec3 position = Vec3.ZERO;
    private Vec3 destination = Vec3.ZERO;
    private Vec3 visibilityOrigin = Vec3.ZERO;
    private Vec3 flightStart = Vec3.ZERO;
    private long launchTick;
    private Vec3 returnStart = Vec3.ZERO;
    private Vec3 returnEnd = Vec3.ZERO;
    private Vec3 returnSide = Vec3.ZERO;
    private float returnPower;
    private float returnTwist;
    private ItemStack original = ItemStack.EMPTY;
    private ItemStack weapon = ItemStack.EMPTY;
    private int slot;
    private long phaseStarted;
    private long recoveryStartedTick;
    private float returnRecovery;
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
    private boolean aimedAtBlock;
    private Impact impact = Impact.NONE;
    private long impactTick;
    private int holdTicks = HOLD_TICKS;
    private Vec3 drillStart = Vec3.ZERO;
    private Entity previousTarget;
    private int nextHit;
    private final List<PsychicDriveGauntletsEffect> effects = new ArrayList<>();

    public List<PsychicDriveGauntletsEffect> drainEffects() {
        var result = List.copyOf(effects);
        effects.clear();
        return result;
    }

    private void effect(Kind kind) {
        effects.add(new PsychicDriveGauntletsEffect(right, kind, position,
                destination.subtract(flightStart).normalize(), charge));
    }

    public PsychicDriveGauntletsArm(boolean right) { this.right = right; }
    public boolean available() { return phase == Phase.IDLE; }
    public Phase phase() { return phase; }
    public Vec3 position() { return position; }
    public Snapshot snapshot() {
        return new Snapshot(phase, position, yaw, pitch, drawnAmount, flightStart, destination, launchTick, lastTick,
                charge, recoveryStartedTick, returnRecovery, impact, impactTick, holdTicks,
                new MotionData(phaseStarted, returnStart, returnEnd, returnSide, returnPower, returnTwist));
    }

    public void reserve(Player owner, InteractionHand usedHand, long time) {
        hand = usedHand;
        original = owner.getItemInHand(hand);
        slot = owner.getInventory().selected;
        phase = Phase.CHARGING;
        impact = Impact.NONE;
        impactTick = 0;
        recoveryStartedTick = 0;
        returnRecovery = 0;
        clearDrill();
        launchTick = time;
        phaseStarted = time;
        lastTick = time - 1;
        charge = 0;
        position = PsychicDriveGauntletsPose.position(owner, 1, right);
        effect(Kind.CHARGE_START);
    }

    public boolean sourceChanged(Player owner) {
        return owner.getItemInHand(hand) != original
                || (hand == InteractionHand.MAIN_HAND && owner.getInventory().selected != slot);
    }

    public void reset() {
        effects.clear();
        phase = Phase.IDLE;
        impact = Impact.NONE;
        impactTick = 0;
        original = ItemStack.EMPTY;
        weapon = ItemStack.EMPTY;
        charge = 0;
        recoveryStartedTick = 0;
        returnRecovery = 0;
        clearDrill();
    }

    private void clearDrill() {
        holdTicks = HOLD_TICKS;
        previousTarget = null;
        nextHit = 0;
    }

    public void launch(Player owner, Vec3 target, float amount, long time) {
        launch(owner, target, amount, time, InteractionHand.MAIN_HAND, 0);
    }

    public void prepare(Player owner, PsychicDriveGauntletsRaycast.Aim aim, float amount, long time) {
        if (!available()) return;
        begin(owner, aim, amount, time, InteractionHand.MAIN_HAND, 0, true);
    }

    public void launch(Player owner, Vec3 target, float amount, long time, InteractionHand usedHand, float charged) {
        launch(owner, new PsychicDriveGauntletsRaycast.Aim(owner.getEyePosition(), target, false), amount, time, usedHand, charged);
    }

    public void launch(Player owner, PsychicDriveGauntletsRaycast.Aim aim, float amount, long time,
                       InteractionHand usedHand, float charged) {
        begin(owner, aim, amount, time, usedHand, charged, false);
    }

    private void begin(Player owner, PsychicDriveGauntletsRaycast.Aim aim, float amount, long time,
                       InteractionHand usedHand, float charged, boolean preparing) {
        if (!available() && phase != Phase.CHARGING) return;
        var target = aim.point();
        visibilityOrigin = aim.origin();
        aimedAtBlock = aim.block();
        impact = Impact.NONE;
        impactTick = 0;
        hand = usedHand;
        clearDrill();
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
        phase = preparing ? Phase.PREPARING : Phase.OUTBOUND;
        recoveryStartedTick = 0;
        returnRecovery = 0;
        phaseStarted = time;
        lastTick = time;
        returnStart = position;
        returnEnd = position;
        returnSide = new Vec3(right ? -1 : 1, 0, 0).yRot(-owner.getYRot() * Mth.DEG_TO_RAD);
        returnPower = 0;
        returnTwist = 0;
        if (!preparing) effect(Kind.LAUNCH);
    }

    public void recall(long time) {
        if (phase == Phase.IDLE || phase == Phase.RETURNING) return;
        var pose = PsychicDriveGauntletsMotion.singlePose(snapshot(), time);
        returnPower = pose.power();
        returnTwist = pose.twist();
        // 持ち替えによる早期帰還でも、既に回復した分を戻したり飛ばしたりしない。
        returnRecovery = phase == Phase.HOLDING || phase == Phase.FOLLOW_THROUGH
                ? Mth.clamp((float) (time - recoveryStartedTick) / (holdTicks + RETURN_TICKS),
                        0, (float) holdTicks / (holdTicks + RETURN_TICKS)) : 0;
        previousTarget = null;
        recoveryStartedTick = time;
        phase = Phase.RETURNING;
        phaseStarted = time;
        returnStart = position;
        returnYaw = yaw;
        returnPitch = pitch;
        returnDrawnAmount = drawnAmount;
    }

    public void tick(Player owner, float amount, long time) {
        if (available()) return;
        if (phase != Phase.RETURNING) {
            returnEnd = PsychicDriveGauntletsPose.position(owner, amount, right);
            if (phase != Phase.FOLLOW_THROUGH) {
                returnSide = new Vec3(right ? -1 : 1, 0, 0).yRot(-owner.getYRot() * Mth.DEG_TO_RAD);
            }
        }
        if (sourceChanged(owner)) {
            if (phase == Phase.CHARGING) { reset(); return; }
            recall(time);
        }
        if (time <= lastTick) return;
        lastTick = time;
        switch (phase) {
            case PREPARING -> {
                var attachment = PsychicDriveGauntletsPose.position(owner, amount, right);
                var direction = destination.subtract(flightStart).normalize();
                position = PsychicDriveGauntletsMotion.preparing(attachment, direction,
                        (double) (time - phaseStarted) / PsychicDriveGauntletsMotion.PREPARE_TICKS);
                // 照準は入力時に固定するが、瞬間移動で長い経路を新しく掃引してはいけない。
                double maximumPath = PsychicDriveGauntletsRaycast.RANGE
                        + attachment.distanceTo(owner.getEyePosition()) + PsychicDriveGauntletsMotion.PREPARE_DISTANCE;
                if (position.distanceToSqr(destination) > maximumPath * maximumPath) {
                    recall(time);
                    break;
                }
                if (time - phaseStarted >= PsychicDriveGauntletsMotion.PREPARE_TICKS) {
                    flightStart = position;
                    phase = Phase.OUTBOUND;
                    phaseStarted = time;
                    var flightDirection = destination.subtract(flightStart);
                    yaw = (float) (Mth.atan2(-flightDirection.x, flightDirection.z) * Mth.RAD_TO_DEG);
                    pitch = (float) (-Mth.atan2(flightDirection.y, flightDirection.horizontalDistance()) * Mth.RAD_TO_DEG);
                    effect(Kind.LAUNCH);
                }
            }
            case CHARGING -> {
                boolean wasReady = charge >= 1;
                charge = PsychicDriveGauntletsCharge.amount(time - launchTick);
                drawnAmount = 1;
                position = PsychicDriveGauntletsPose.position(owner, 1, right)
                        .add(PsychicDriveGauntletsCharge.offset(time - launchTick)
                                .yRot((180 - owner.getYRot()) * Mth.DEG_TO_RAD));
                yaw = owner.getYRot();
                pitch = 0;
                if (!wasReady && charge >= 1) effect(Kind.CHARGE_READY);
            }
            case OUTBOUND -> {
                var next = PsychicDriveGauntletsMotion.outbound(flightStart, destination, (double) time - phaseStarted, charge);
                // 腕自身は地形を通過するが、初発は入力時の目から視線が通る対象だけに当てる。
                var contact = PsychicDriveGauntletsRaycast.firstContact(owner, position, next, visibilityOrigin);
                if (contact != null) {
                    position = contact.point();
                    impact = Impact.ENTITY;
                    impactTick = time;
                    phase = Phase.HOLDING;
                    phaseStarted = time;
                    recoveryStartedTick = time;
                    if (charge >= 1) {
                        holdTicks = DRILL_TICKS;
                        drillStart = position;
                        previousTarget = contact.target();
                        nextHit = 1;
                        boolean applied = hit(owner, previousTarget, true);
                        effect(applied ? Kind.DRILL_START : Kind.REJECTED);
                        if (!applied) recall(time);
                    } else {
                        effect(hit(owner, contact.target(), false) ? Kind.IMPACT : Kind.REJECTED);
                    }
                } else {
                    position = next;
                    if (position.distanceToSqr(destination) < 1.0e-12) {
                        // 発射時の照準点への到達だけを演出に使い、飛行中の壁衝突は追加しない。
                        if (aimedAtBlock) {
                            impact = Impact.BLOCK;
                            impactTick = time;
                            effect(Kind.BLOCK_IMPACT);
                        }
                        phase = Phase.HOLDING;
                        phaseStarted = time;
                        recoveryStartedTick = time;
                        if (!aimedAtBlock && charge < 1) {
                            phase = Phase.FOLLOW_THROUGH;
                            holdTicks = MISS_TICKS;
                        } else if (!aimedAtBlock) {
                            recall(time);
                            // 空振りは停止を飛ばすが、通常命中と同じ7tick分のゲージを使う。
                            returnRecovery = (float) HOLD_TICKS / (HOLD_TICKS + RETURN_TICKS);
                        }
                    }
                }
            }
            case HOLDING -> {
                if (holdTicks == DRILL_TICKS) {
                    long elapsed = time - phaseStarted;
                    position = drillStart.add(destination.subtract(flightStart).normalize()
                            .scale(DRILL_DISTANCE * Mth.clamp((double) elapsed / DRILL_TICKS, 0, 1)));
                    if (elapsed >= (long) nextHit * HIT_INTERVAL) {
                        // tickが飛んでも過去の段をまとめて与えず、現在の接触だけを判定する。
                        if (elapsed > (long) nextHit * HIT_INTERVAL) { recall(time); break; }
                        var target = PsychicDriveGauntletsRaycast.drillTarget(owner, position, previousTarget);
                        // 初発成立後は拳を判定の起点とし、別対象へ切り替わっても目からの遮蔽を再確認しない。
                        boolean finalHit = nextHit == 3;
                        if (target == null || !hit(owner, target, !finalHit)) {
                            recall(time);
                        } else {
                            effect(finalHit ? Kind.FINISH : Kind.DRILL_TICK);
                            previousTarget = target;
                            nextHit++;
                            if (finalHit) recall(time);
                        }
                    }
                } else if (time - phaseStarted >= holdTicks) recall(time);
            }
            case FOLLOW_THROUGH -> {
                // 余韻中に対象が入り込んでも接触・ダメージ判定は行わない。
                position = PsychicDriveGauntletsMotion.followingThrough(destination, returnSide, time - phaseStarted);
                if (time - phaseStarted >= MISS_TICKS) recall(time);
            }
            case RETURNING -> {
                float progress = Mth.clamp((float) (time - phaseStarted) / RETURN_TICKS, 0.0F, 1.0F);
                returnEnd = PsychicDriveGauntletsPose.position(owner, amount, right);
                position = charge >= 1 ? returnStart.lerp(returnEnd, progress)
                        : PsychicDriveGauntletsMotion.returning(returnStart, returnEnd, returnSide, time - phaseStarted);
                yaw = Mth.rotLerp(progress, returnYaw, Mth.rotLerp(amount, owner.yBodyRot, owner.getYRot()));
                pitch = Mth.lerp(progress, returnPitch, 0.0F);
                drawnAmount = Mth.lerp(progress, returnDrawnAmount, amount);
                if (progress == 1.0F) {
                    phase = Phase.IDLE;
                    original = ItemStack.EMPTY;
                    weapon = ItemStack.EMPTY;
                    previousTarget = null;
                }
            }
            default -> {}
        }
    }

    private boolean hit(Player owner, Entity target, boolean suppressKnockback) {
        var level = (ServerLevel) owner.level();
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS);
        // 実体弾を持たないため、接触位置を明示して盾の方向判定をプレイヤー位置へ戻さない。
        var source = new DamageSource(holder, owner, owner, position);
        var amount = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage);
        boolean applied = CombatTools.applyUnscaledDamage(target, amount, source, suppressKnockback
                ? CombatTools.KnockbackTypes.NO_KNOCKBACK : CombatTools.KnockbackTypes.DEFAULT);
        if (applied && target instanceof LivingEntity living) {
            int haunted = MalumCompatibility.getHauntedLevel(weapon);
            if (haunted > 0 && living.isAlive()) {
                // 独自攻撃はMalumの通常近接経路を通らないため、発射時の武器だけから追加分を補完する。
                // 主手への一時コピーは行わず、副手攻撃や多段でも各命中につき一度だけ適用する。
                var magicHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS_MAGIC);
                var magicSource = new DamageSource(magicHolder, owner, owner, position);
                CombatTools.applyUnscaledDamage(living, haunted, magicSource, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }
            // 独自攻撃はPlayer.attackを通らないため、火属性などの命中後効果も武器snapshotから適用する。
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, living, source, weapon);
            if (suppressKnockback) {
                // amplifierは0始まり。次段までつなぐ短いSlowness Vで、離脱後の拘束を残さない。
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 4), owner);
            } else {
                // 最大溜めは最終段だけ押し出す。耐性とLivingKnockBackEventは通常の経路へ委ねる。
                var knockback = EnchantmentHelper.modifyKnockback(level, weapon, target, source, 0)
                        + (charge >= 1 ? 1 : 0);
                if (knockback > 0) {
                    var direction = destination.subtract(flightStart).normalize();
                    living.knockback(knockback * 0.5, -direction.x, -direction.z);
                }
            }
        }
        return applied;
    }
}
