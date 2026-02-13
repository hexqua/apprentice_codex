package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class FlySwatterProjectileEntity extends Projectile {
    private static final int LIFE_TICKS = 20 * 10;
    private static final RandomSource RNG = RandomSource.create();
    private static final double ROTATION_DEG = 4;

    private float damage;
    private double speed;
    private Entity target;

    public FlySwatterProjectileEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setViewScale(8);
        setNoGravity(true);
    }

    public FlySwatterProjectileEntity(EntityType<? extends Projectile> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setViewScale(8);
        setOwner(owner);
        setNoGravity(true);
    }

    public void setProjectileVelocity(Vec3 rotation, double speed) {
        this.speed = speed;
        setDeltaMovement(rotation.scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    protected void defineSynchedData() {
        // todo:表示同期で何か必要であれば...
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (!level.isClientSide) {
            if (tickCount > LIFE_TICKS) {
                discard();
            }

            var hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitresult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
                onHit(hitresult);
            }

            if (target != null && !target.isRemoved() && target.isAlive()) {
                var targetPos = target.getBoundingBox().getCenter();
                var targetVec = targetPos.subtract(position()).normalize();
                var newAngle = RotationTools.steerTowards(getDeltaMovement(), targetVec, ROTATION_DEG);
                setDeltaMovement(newAngle.scale(speed));
            }

            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1);
        }

        // 軌跡はクライアントでのみ.
        if (level.isClientSide) {
            var camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (camPos.distanceToSqr(position()) < 32.0 * 32.0) {
                var count = 8;
                for (var i = 0; i < count; i++) {
                    var pos = position().subtract(getDeltaMovement().scale(RNG.nextDouble()));
                    EffectTools.createParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos, 0, 0.001);
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        @SuppressWarnings("resource") var level = level();
        if (level.isClientSide) {
            return;
        }

        // todo:命中時の処理をちゃんとミサイルっぽくする.
        var owner = getOwner();
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.FLY_SWATTER);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        // todo:命中時の処理をちゃんとミサイルっぽくする.
        @SuppressWarnings("resource") var level = level();
        if (!level.isClientSide) {
            discard();
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
    public void setTarget(Entity target) {
        this.target = target;
    }
}
