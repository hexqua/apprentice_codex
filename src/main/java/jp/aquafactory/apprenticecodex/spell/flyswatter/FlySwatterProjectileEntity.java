package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class FlySwatterProjectileEntity extends Projectile {
    private static final int LIFE_TICKS = 20 * 10;
    private static final RandomSource RNG = RandomSource.create();
    private static final double ROTATION_DEG = 4;
    private static final double EXPLOSION_KNOCKBACK = 0.5;
    private static final double EXPLOSION_KNOCKBACK_UP = 0.2;

    private float damage;
    private float radius;
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

        var level = level();
        if (level.isClientSide) {
            return;
        }

        var owner = getOwner();
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.FLY_SWATTER);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            onImpact(level, target);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        var level = level();
        if (!level.isClientSide) {
            onImpact(level, null);
            discard();
        }
    }

    private void onImpact(Level level, Entity directHitTarget){
        var position = position();

        // パーティクルと音.
        if (level instanceof ServerLevel server){
            server.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            var smokeSpread = radius * 0.35;
            server.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z,
                    25, smokeSpread, smokeSpread * 0.6, smokeSpread, 0.02);
            var poofSpread = radius * 0.2;
            server.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z,
                    18, poofSpread, poofSpread * 0.4, poofSpread, 0.12);

            server.playSound(null, BlockPos.containing(position), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        }


        // 判定.
        var aabb = new AABB(position, position).inflate(radius);
        var r2 = radius * radius;
        var targets = level.getEntitiesOfClass(Entity.class, aabb, e -> {
            if (!e.isAlive()) {
                return false;
            }

            // 直撃させた対象は爆風ダメージからは除外.
            if (e == directHitTarget) {
                return false;
            }

            // 自爆をさせるため、自分自身は判定に含められるようにする.
            return CombatTools.isValidCombatTarget(e, null);
        });

        var owner = getOwner();
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.FLY_SWATTER);
        for (var e : targets) {
            var dist2 = e.distanceToSqr(position);
            if (dist2 > r2) {
                continue;
            }

            var dist = Math.sqrt(dist2);
            var t = dist / radius;
            var scale = 1.0 - t * t;
            if (scale <= 0) {
                continue;
            }

            var eye = e.getEyePosition();
            var hit = level.clip(new ClipContext(position, eye,ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() != HitResult.Type.MISS) {
                scale *= 0.5;
            }

            var finalDamage = (float)(damage * scale);
            CombatTools.applyDamage(e, finalDamage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);

            // 爆風で吹き飛ばす.
            var dir = e.position().subtract(position);
            if (dir.lengthSqr() > 1.0e-6) {
                dir = dir.normalize();
                e.push(dir.x * EXPLOSION_KNOCKBACK * scale, EXPLOSION_KNOCKBACK_UP * scale, dir.z * EXPLOSION_KNOCKBACK * scale);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putFloat("radius", radius);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        radius = tag.getFloat("radius");
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

    public void setRadius(float newRadius) {
        radius = newRadius;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }
}
