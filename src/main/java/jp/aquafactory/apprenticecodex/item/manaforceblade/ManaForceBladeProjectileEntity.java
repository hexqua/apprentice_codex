package jp.aquafactory.apprenticecodex.item.manaforceblade;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class ManaForceBladeProjectileEntity extends Projectile {
    private static final int LIFE_TICKS = 20 * 3;
    private static final RandomSource RNG = RandomSource.create();
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(ManaForceBladeProjectileEntity.class, EntityDataSerializers.INT);

    private float damage;

    public ManaForceBladeProjectileEntity(EntityType<? extends ManaForceBladeProjectileEntity> type, Level level) {
        super(type, level);
        setViewScale(8);
        setNoGravity(true);
    }

    public ManaForceBladeProjectileEntity(EntityType<? extends ManaForceBladeProjectileEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        setOwner(owner);
    }

    public void setProjectileVelocity(Vec3 rotation, double speed) {
        setDeltaMovement(rotation.scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.35f,
                    8,
                    0.015f,
                    0.01D,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();

        if (!level.isClientSide) {
            if (tickCount > LIFE_TICKS) {
                discard();
                return;
            }

            var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
                onHit(hitResult);
            }

            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1);
        }

        if (level.isClientSide) {
            var camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (camPos.distanceToSqr(position()) < 48.0D * 48.0D) {
                var pos = position().subtract(getDeltaMovement().scale(RNG.nextDouble()));
                EffectTools.createParticle(level, ParticleTypes.ELECTRIC_SPARK, pos, 0.18D, 0.04D);
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) {
            return;
        }

        var owner = getOwner();
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.MANA_FORCE_BLADE);
            CombatTools.applyDamage(target, damage, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, 0xFFFFFF);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("damage")) {
            damage = tag.getFloat("damage");
        }
        if (tag.contains("color")) {
            setColor(tag.getInt("color"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("color", getColor());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double max = 128.0D;
        return distanceSqr < max * max;
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    public void setColor(int rgb) {
        entityData.set(COLOR, rgb & 0xFFFFFF);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }
}
