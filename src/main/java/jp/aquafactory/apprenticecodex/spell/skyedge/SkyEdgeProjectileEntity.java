package jp.aquafactory.apprenticecodex.spell.skyedge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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

public class SkyEdgeProjectileEntity extends Projectile implements AntiMagicSusceptible
{
    private static final int LIFE_TICKS = 20 * 5;
    private static final RandomSource RNG = RandomSource.create();

    private float damage;
    private int standbyTick;

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
        var level = level();

        // 射出時パーティクル.
        // todo:再ログイン制御がいるかどうか.
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.4f,
                    8,
                    0.015f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
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

            if (tickCount == standbyTick){
                AudioTools.playSoundFromEntity(level, this, SoundRegistry.VANILLA_PROJECTILE_SHOOT.get(), SoundSource.PLAYERS, 0.75f, 1.5f);
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
                    EffectTools.createParticle(level, ParticleTypes.ELECTRIC_SPARK, pos, radius, speed);
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
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.SKY_EDGE);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.SKY_EDGE.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
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
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        onImpact(level(), 0.1, false);
        discard();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("damage")) {
            damage = tag.getFloat("damage");
        }
        if (tag.contains("standbyTick")) {
            standbyTick = tag.getInt("standbyTick");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("standbyTick", standbyTick);
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
        return tickCount >= standbyTick + delay;
    }

    public void setStandbyTicks(int ticks) {
        standbyTick = ticks;
    }

    private void onImpact(Level level, double impactDistance, boolean isImpactOnEntity) {
       AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);

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
