package jp.aquafactory.apprenticecodex.spell.lockonray;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.LockOnRayTrailPacket;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public final class LockOnRayLaserEntity extends Projectile implements AntiMagicSusceptible {
    public static final int LIFETIME = 40;
    private UUID targetId;
    private int arrivalTicks;
    private float damage;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 arrivalVelocity = Vec3.ZERO;
    // ログアウト後の発射済み弾も味方保護と攻撃者情報を維持する。弾の40tickだけ保持する。
    private LivingEntity launchOwner;

    public LockOnRayLaserEntity(EntityType<? extends LockOnRayLaserEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void launch(LivingEntity owner, Vec3 origin, Entity target, float damage) {
        setOwner(owner);
        launchOwner = owner;
        setPos(origin);
        targetId = target.getUUID();
        this.damage = damage;
        var offset = target.getBoundingBox().getCenter().subtract(origin);
        arrivalTicks = 5 + (int) Math.round(offset.length() / 16);
        var forward = offset.lengthSqr() > 1.0e-8 ? offset.normalize() : owner.getLookAngle();
        var facing = owner.getLookAngle();
        var side = facing.cross(Math.abs(facing.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
        var up = side.cross(facing).normalize();
        double angle = random.nextDouble() * Math.PI * 2;
        // 短い到達時間でも背後へ数ブロック回り込むよう、速度ではなく曲線全体の接線長を決める。
        // 後方28＋距離の半分、横方向20で、近距離から射程端まで大きな弧を維持する。
        velocity = facing.scale(-(28 + offset.length() * 0.5))
                .add(side.scale(Math.cos(angle) * 20)).add(up.scale(Math.sin(angle) * 20))
                .scale(1.0 / arrivalTicks);
        arrivalVelocity = forward.scale(Math.max(1, offset.length() / arrivalTicks * 1.5));
        setDeltaMovement(velocity);
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > LIFETIME) { discard(); return; }
        var start = position();
        var target = targetId == null ? null : server.getEntity(targetId);
        if (!LockOnRayCastData.isLoadedTarget(server, target)) targetId = null;
        LockOnRayCurve step;
        if (targetId != null && tickCount <= arrivalTicks) {
            int remaining = arrivalTicks - tickCount + 1;
            var curve = new LockOnRayCurve(start, target.getBoundingBox().getCenter(),
                    velocity.scale(remaining), arrivalVelocity.scale(remaining));
            step = curve.prefix(1.0 / remaining);
            velocity = step.endTangent();
        } else {
            // 対象消失時は接線ではなく、最後に実際に移動したtickの差分を維持する。
            targetId = null;
            velocity = getDeltaMovement();
            step = new LockOnRayCurve(start, start.add(velocity), velocity, velocity);
        }
        double fraction = moveAlongCurve(step);
        var visible = step.prefix(fraction);
        // 弾の削除パケットに依存しない描画データで、着弾地点までの残像を保証する。
        Networks.sendToPlayersNear(server, start, 160, new LockOnRayTrailPacket(visible));
        if (!isRemoved()) {
            setPos(step.end());
            setDeltaMovement(position().subtract(start));
            if (tickCount >= LIFETIME) discard();
        }
    }

    private double moveAlongCurve(LockOnRayCurve curve) {
        int samples = Mth.clamp((int) Math.ceil((curve.startTangent().length() + curve.endTangent().length()
                + curve.start().distanceTo(curve.end())) * 4), 8, 128);
        var from = curve.start();
        var ignored = new HashSet<UUID>();
        for (int i = 1; i <= samples; i++) {
            var to = curve.position(i / (double) samples);
            var hits = new ArrayList<EntityHitResult>();
            for (var raw : level().getEntities(this, new AABB(from, to).inflate(0.3), this::canHitEntity)) {
                var box = raw.getBoundingBox().inflate(0.15);
                var point = box.contains(from) ? Optional.of(from) : box.clip(from, to);
                point.ifPresent(p -> hits.add(new EntityHitResult(raw, p)));
            }
            final var segmentStart = from;
            hits.sort(Comparator.comparingDouble(hit -> segmentStart.distanceToSqr(hit.getLocation())));
            for (var hit : hits) {
                var target = CombatTools.resolutePartEntity(hit.getEntity());
                if (!ignored.add(target.getUUID()) || EventHooks.onProjectileImpact(this, hit)) continue;
                var point = hit.getLocation();
                double part = from.distanceTo(to) < 1.0e-8 ? 0 : from.distanceTo(point) / from.distanceTo(to);
                setPos(point);
                CombatTools.applyDamage(target, damage,
                        CombatTools.getDamageSource(level(), this, combatOwner(), DamageTypes.LOCK_ON_RAY),
                        SchoolRegistry.HOLY.get(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                MagicManager.spawnParticles(level(), ParticleHelper.WISP, point.x, point.y, point.z,
                        25, 0, 0, 0, 0.18, true);
                level().playSound(null, point.x, point.y, point.z, SoundRegistry.GUIDING_BOLT_IMPACT.value(),
                        SoundSource.NEUTRAL, 2, 0.9f + random.nextFloat() * 0.4f);
                discard();
                return (i - 1 + part) / samples;
            }
            from = to;
        }
        return 1;
    }

    private Entity combatOwner() { return launchOwner != null ? launchOwner : getOwner(); }

    @Override protected boolean canHitEntity(@NotNull Entity entity) {
        var target = CombatTools.resolutePartEntity(entity);
        return target.isAlive() && !target.isRemoved() && !entity.isSpectator()
                && entity.isPickable() && CombatTools.isValidCombatTarget(target, combatOwner());
    }

    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public void onAntiMagic(MagicData data) { if (!level().isClientSide) discard(); }
    @Override protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {}
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
