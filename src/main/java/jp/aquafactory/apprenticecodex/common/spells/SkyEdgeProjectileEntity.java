package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class SkyEdgeProjectileEntity extends Projectile
{
    private static final RandomSource RNG = RandomSource.create();

    private static final EntityDataAccessor<Integer> DATA_STANDBY_TICK =
            SynchedEntityData.defineId(SkyEdgeProjectileEntity.class, EntityDataSerializers.INT);

    private float damage = 0;
    private static final int LIFE_TICKS = 20 * 5;
    private static final int DEFAULT_STANDBY_TICKS = 20;

    public SkyEdgeProjectileEntity(EntityType<? extends SkyEdgeProjectileEntity> type, Level level) {
        super(type, level);
        setViewScale(8);
        setNoGravity(true);
    }

    public SkyEdgeProjectileEntity(EntityType<? extends SkyEdgeProjectileEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setViewScale(8);
        setOwner(owner);
        setNoGravity(true);
    }

    public void setProjectileVelocity(Vec3 rotation, double speed) {
        setDeltaMovement(rotation.scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    public void tick() {
        //noinspection resource
        var level = level();

        // 射出時パーティクル.
        if (level.isClientSide && firstTick) {
            // 平面基底を作る.
            var norm = getLookAngle().normalize();
            var arbitrary = Math.abs(norm.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            var u = norm.cross(arbitrary).normalize();
            var w = norm.cross(u).normalize();
            var count = 8;
            for( var i = 0; i < count; i++){
                var radius = 0.4 + 0.1 * Math.sqrt(level.random.nextDouble());
                var angle = (Math.PI * 2.0) * i / count + level.random.nextDouble() * 0.05;
                var a = Math.cos(angle) * radius;
                var b = Math.sin(angle) * radius;
                var offset = u.scale(a).add(w.scale(b));
                var pos = position().add(offset);
                level.addParticle(
                        ParticleTypes.END_ROD,
                        pos.x,
                        pos.y,
                        pos.z,
                        RNG.nextDouble() * 0.015,
                        RNG.nextDouble() * 0.015,
                        RNG.nextDouble() * 0.015
                );
            }
        }

        super.tick();

        if (!level.isClientSide) {
            if (tickCount > LIFE_TICKS) {
                discard();
            }

            if (canShooting(0)) {
                var hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
                if (hitresult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
                    onHit(hitresult);
                }

                move(MoverType.SELF, getDeltaMovement());
                ProjectileUtil.rotateTowardsMovement(this, 1);
            }

            if (isShootingJustTiming()){
                var volume = 0.75f;
                var pitch = 1.5f;
                level.playSound(
                        null,
                        getX(), getY(), getZ(),
                        SoundEvents.SHULKER_SHOOT,
                        SoundSource.PLAYERS,
                        volume,
                        pitch
                );
            }
        }

        // 軌跡はクライアントでのみ.
        if (level.isClientSide && canShooting(1)) {
            var camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (camPos.distanceToSqr(position()) < 48.0 * 48.0) {
                var radius = 0.2;
                var speed = 0.05;
                var count = 2;
                for (var i = 0; i < count; i++) {
                    var pos = position().subtract(getDeltaMovement().scale(RNG.nextDouble()));
                    level.addParticle(
                            ParticleTypes.ELECTRIC_SPARK,
                            pos.x + getRandomRange(radius),
                            pos.y + getRandomRange(radius),
                            pos.z + getRandomRange(radius),
                            getRandomRange(speed),
                            getRandomRange(speed),
                            getRandomRange(speed)
                    );
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        var level = level();
        if (level.isClientSide) {
            return;
        }

        var owner = getOwner();
        var target = CombatTools.resolutePartEntity(hit.getEntity());

        if (CombatTools.isValidCombatTarget(target, owner)) {
            var source = DamageSources.getDamageSource(level(), this, owner, "sky_edge");
            CombatTools.applyDamage(target, damage, source, SchoolRegistry.LIGHTNING.get(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            onImpact(level, 0.5 + level.random.nextDouble() * 0.25, true);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        var level = level();
        if (!level.isClientSide) {
            onImpact(level, 0.1, false);
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_STANDBY_TICK, DEFAULT_STANDBY_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("damage")) {
            damage = tag.getFloat("damage");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double max = 128.0;
        return distanceSqr < max * max;
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    private boolean canShooting(int delay){
        return tickCount >= getStandbyTicks() + delay;
    }

    private boolean isShootingJustTiming(){
        return tickCount == getStandbyTicks();
    }

    private int getStandbyTicks() {
        return this.entityData.get(DATA_STANDBY_TICK);
    }

    public void setStandbyTicks(int ticks) {
        entityData.set(DATA_STANDBY_TICK, ticks);
    }

    private double getRandomRange(double range){
        return (RNG.nextDouble() * 2 - 1) * range;
    }

    private void onImpact(Level level, double impactDistance, boolean isImpactOnEntity) {
        if (!level.isClientSide) {
            var volume = 1.0f;
            var pitch = 1.0f;
            level.playSound(
                    null,
                    getX(), getY(), getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    volume,
                    pitch
            );

            if (level instanceof ServerLevel server){
                // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
                var dir = getDeltaMovement();
                var impactPos = position().add(dir.scale(impactDistance));
                server.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        impactPos.x, impactPos.y, impactPos.z,
                        8,
                        0.2, 0.2, 0.2,
                        0.25
                );

                if (isImpactOnEntity) {
                    server.sendParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            impactPos.x, impactPos.y, impactPos.z,
                            1,
                            0.05, 0.05, 0.05,
                            0.0
                    );
                }

            }
        }
    }
}
