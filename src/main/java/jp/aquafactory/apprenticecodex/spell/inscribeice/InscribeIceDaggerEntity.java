package jp.aquafactory.apprenticecodex.spell.inscribeice;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.NotchedFrozenEffect;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class InscribeIceDaggerEntity extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 1.68D;
    private static final int LIFE_TICKS = 20 * 5;
    private static final int BLOCK_COLLISION_GRACE_TICKS = 4;
    private static final byte EVENT_IMPACT_BURST = 62;
    private static final UUID UNKNOWN_OWNER_SOUND_KEY = new UUID(0L, 0L);
    private static final Map<ServerLevel, Map<UUID, Long>> LAST_HIT_SOUND_TICKS = new WeakHashMap<>();

    private static final int IMPACT_RHOMBUS_COUNT = 2;
    private static final int IMPACT_SPARK_COUNT = 5;
    private static final float IMPACT_RHOMBUS_SIZE_MIN = 0.13F;
    private static final float IMPACT_RHOMBUS_SIZE_MAX = 0.22F;
    private static final float IMPACT_SPARK_SIZE_MIN = 0.08F;
    private static final float IMPACT_SPARK_SIZE_MAX = 0.13F;
    private static final float ICE_RED = 0.35F;
    private static final float ICE_GREEN_MIN = 0.78F;
    private static final float ICE_GREEN_MAX = 0.95F;
    private static final float ICE_BLUE = 1.0F;
    private static final int IMPACT_WHITEN_TICKS = 6;
    private static final int IMPACT_SPARK_LIFETIME = 12;
    private static final int IMPACT_SPARK_LIFETIME_VARIANCE = 4;

    private float damage;
    private float burstDamage;
    private @Nullable Entity fallbackOwner;

    public InscribeIceDaggerEntity(EntityType<? extends InscribeIceDaggerEntity> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0F);
        setNoGravity(true);
    }

    public InscribeIceDaggerEntity(EntityType<? extends InscribeIceDaggerEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        fallbackOwner = owner;
    }

    @Override
    public @Nullable Entity getOwner() {
        var owner = super.getOwner();
        return owner != null ? owner : fallbackOwner;
    }

    public void setProjectileVelocity(Vec3 direction, double speed) {
        var resolvedDirection = direction.lengthSqr() > 1.0e-8D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        setDeltaMovement(resolvedDirection.scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (!level.isClientSide) {
            if (tickCount > LIFE_TICKS) {
                discard();
                return;
            }

            var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS
                    && !EventHooks.onProjectileImpact(this, hitResult)) {
                onHit(hitResult);
            }

            if (!isRemoved()) {
                var movement = getDeltaMovement();
                move(MoverType.SELF, movement);
                // 中心線のレイキャストが外れても当たり箱が障害物を擦るため、move の物理衝突も着弾として扱う。
                if ((horizontalCollision || verticalCollision) && tickCount > BLOCK_COLLISION_GRACE_TICKS) {
                    triggerImpact();
                    playHitSound();
                    discard();
                    return;
                }
                if (tickCount <= BLOCK_COLLISION_GRACE_TICKS) {
                    setDeltaMovement(movement);
                }
                ProjectileUtil.rotateTowardsMovement(this, 1.0F);
            }
        } else {
            spawnTrailParticles();
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        setPos(hitResult.getLocation());
        triggerImpact();
        playHitSound();
        var owner = getOwner();
        var target = CombatTools.resolutePartEntity(hitResult.getEntity());
        if (!CombatTools.isValidCombatTarget(target, owner)) {
            discard();
            return;
        }

        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.INSCRIBE_ICE);
        var damaged = CombatTools.applyDamage(
                target,
                damage,
                source,
                SpellRegistry.INSCRIBE_ICE.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );
        if (damaged && target instanceof LivingEntity livingTarget) {
            applyNotchedFrozenOrBurst(serverLevel, livingTarget, this, owner, burstDamage);
        }
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        if (tickCount <= BLOCK_COLLISION_GRACE_TICKS) {
            return;
        }
        super.onHitBlock(hitResult);
        if (!level().isClientSide) {
            setPos(hitResult.getLocation());
            triggerImpact();
            playHitSound();
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var target = CombatTools.resolutePartEntity(entity);
        return entity != getOwner()
                && CombatTools.isValidCombatTarget(target, getOwner())
                && super.canHitEntity(entity);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == EVENT_IMPACT_BURST && level().isClientSide) {
            spawnImpactParticles();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("BurstDamage", burstDamage);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        burstDamage = tag.getFloat("BurstDamage");
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 128.0D;
        return distanceSqr < maxDistance * maxDistance;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setBurstDamage(float burstDamage) {
        this.burstDamage = burstDamage;
    }

    public float getDamageForGameTest() {
        return damage;
    }

    public float getBurstDamageForGameTest() {
        return burstDamage;
    }

    public static void applyNotchedFrozenOrBurst(ServerLevel serverLevel, LivingEntity target, Entity sourceEntity,
                                                 Entity owner, float burstDamage) {
        var current = target.getEffect(EffectRegistry.NOTCHED_FROZEN);
        if (current == null) {
            target.addEffect(createNotchedFrozenInstance(0));
            return;
        }

        if (current.getAmplifier() < NotchedFrozenEffect.MAX_STACK_AMPLIFIER) {
            target.addEffect(createNotchedFrozenInstance(current.getAmplifier() + 1));
            return;
        }

        InscribeIceBurst.burstFromDagger(serverLevel, target, sourceEntity, owner, burstDamage);
    }

    private static MobEffectInstance createNotchedFrozenInstance(int amplifier) {
        return new MobEffectInstance(
                EffectRegistry.NOTCHED_FROZEN,
                NotchedFrozenEffect.DURATION_TICKS,
                amplifier,
                false,
                false,
                true
        );
    }

    private void playHitSound() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var owner = getOwner();
        var ownerKey = owner != null ? owner.getUUID() : UNKNOWN_OWNER_SOUND_KEY;
        var currentTick = serverLevel.getGameTime();
        var ticksByOwner = LAST_HIT_SOUND_TICKS.computeIfAbsent(serverLevel, key -> new HashMap<>());
        var lastTick = ticksByOwner.get(ownerKey);
        if (lastTick != null && lastTick == currentTick) {
            return;
        }

        ticksByOwner.put(ownerKey, currentTick);
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.ICE_DAGGER_HIT.get(), SoundSource.PLAYERS, 0.55F, 0.86F, 0.04F);
    }

    private void triggerImpact() {
        level().broadcastEntityEvent(this, EVENT_IMPACT_BURST);
    }

    private void spawnTrailParticles() {
        var random = level().random;
        var particle = InscribeIceBurst.createTrailSparkParticle();
        var pos = position().subtract(getDeltaMovement().scale(random.nextDouble()));
        level().addParticle(
                particle,
                pos.x + (random.nextDouble() - 0.5D) * 0.12D,
                pos.y + (random.nextDouble() - 0.5D) * 0.12D,
                pos.z + (random.nextDouble() - 0.5D) * 0.12D,
                (random.nextDouble() - 0.5D) * 0.015D,
                (random.nextDouble() - 0.5D) * 0.015D,
                (random.nextDouble() - 0.5D) * 0.015D
        );
    }

    private void spawnImpactParticles() {
        var random = level().random;
        var forward = normalizeOrFallback(getDeltaMovement(), new Vec3(0.0D, 0.0D, 1.0D));
        var right = computeRightVector(forward);
        var up = right.cross(forward).normalize();

        for (var i = 0; i < IMPACT_RHOMBUS_COUNT; ++i) {
            spawnImpactRhombusParticle(random, forward, right, up);
        }
        for (var i = 0; i < IMPACT_SPARK_COUNT; ++i) {
            spawnImpactSparkParticle(random, forward, right, up);
        }
    }

    private void spawnImpactRhombusParticle(net.minecraft.util.RandomSource random, Vec3 forward, Vec3 right, Vec3 up) {
        var velocity = forward.scale(0.03D + random.nextDouble() * 0.05D)
                .add(right.scale((random.nextDouble() - 0.5D) * 0.12D))
                .add(up.scale((random.nextDouble() - 0.5D) * 0.12D));
        var size = Mth.lerp(random.nextFloat(), IMPACT_RHOMBUS_SIZE_MIN, IMPACT_RHOMBUS_SIZE_MAX);
        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                        size,
                        ICE_RED,
                        Mth.lerp(random.nextFloat(), ICE_GREEN_MIN, ICE_GREEN_MAX),
                        ICE_BLUE,
                        IMPACT_WHITEN_TICKS
                ),
                getX() + (random.nextDouble() - 0.5D) * 0.08D,
                getY() + (random.nextDouble() - 0.5D) * 0.08D,
                getZ() + (random.nextDouble() - 0.5D) * 0.08D,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void spawnImpactSparkParticle(net.minecraft.util.RandomSource random, Vec3 forward, Vec3 right, Vec3 up) {
        var velocity = forward.scale(0.06D + random.nextDouble() * 0.12D)
                .add(right.scale((random.nextDouble() - 0.5D) * 0.18D))
                .add(up.scale((random.nextDouble() - 0.5D) * 0.18D));
        var size = Mth.lerp(random.nextFloat(), IMPACT_SPARK_SIZE_MIN, IMPACT_SPARK_SIZE_MAX);
        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        ICE_RED,
                        Mth.lerp(random.nextFloat(), ICE_GREEN_MIN, ICE_GREEN_MAX),
                        ICE_BLUE,
                        IMPACT_WHITEN_TICKS,
                        IMPACT_SPARK_LIFETIME,
                        IMPACT_SPARK_LIFETIME_VARIANCE,
                        0.9F,
                        1.25F,
                        0.85F,
                        1.0F,
                        0.05F,
                        0.7F,
                        0.7F,
                        true
                ),
                getX() + (random.nextDouble() - 0.5D) * 0.08D,
                getY() + (random.nextDouble() - 0.5D) * 0.08D,
                getZ() + (random.nextDouble() - 0.5D) * 0.08D,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector != null && vector.lengthSqr() > 1.0E-8D) {
            return vector.normalize();
        }
        if (fallback != null && fallback.lengthSqr() > 1.0E-8D) {
            return fallback.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 computeRightVector(Vec3 forward) {
        var right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D).cross(forward);
        }
        return right.normalize();
    }
}
