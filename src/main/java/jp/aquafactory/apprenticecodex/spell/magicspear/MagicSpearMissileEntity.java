package jp.aquafactory.apprenticecodex.spell.magicspear;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.UUID;

public class MagicSpearMissileEntity extends Projectile implements GeoEntity, AntiMagicSusceptible {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private static final int PHASE_RELEASE = 0;
    private static final int PHASE_BOOST = 1;
    private static final int PHASE_BURST = 2;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(MagicSpearMissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIN_DIRECTION =
            SynchedEntityData.defineId(MagicSpearMissileEntity.class, EntityDataSerializers.INT);

    private static final int RELEASE_TICKS = 5;
    private static final int BURST_TICKS = 14;
    private static final int MAX_LIFE_TICKS = 120;
    private static final double RELEASE_SIDE_SPEED = 0.045;
    private static final double RELEASE_FORWARD_SPEED = 0.02;
    private static final double BOOST_SPEED = 2.2;
    private static final double HOMING_TURN_DEGREES = 5.0;
    private static final double ENTITY_IMPACT_INFLATE = 0.35;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0e-6;
    private static final double BLAST_RADIUS = 1.5;
    private static final float BURST_CUBE_SIZE = 3.0f;

    private static final float BURNER_RED = 1.0f;
    private static final float BURNER_GREEN = 0.56f;
    private static final float BURNER_BLUE = 0.12f;
    private static final int BURNER_WHITEN_TICKS = 2;
    private static final int BURNER_LIFETIME = 7;
    private static final int BURNER_LIFETIME_VARIANCE = 3;
    private static final float BURNER_MIN_SIZE_MULTIPLIER = 0.55f;
    private static final float BURNER_MAX_SIZE_MULTIPLIER = 1.1f;
    private static final float BURNER_MIN_ALPHA = 0.72f;
    private static final float BURNER_MAX_ALPHA = 0.96f;
    private static final float BURNER_FADE_IN_END = 0.05f;
    private static final float BURNER_FADE_OUT_START = 0.55f;
    private static final float BURNER_END_SCALE_MULTIPLIER = 0.35f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int phaseTicks;
    private Vec3 movementDirection = new Vec3(0.0, 0.0, 1.0);
    private Vec3 releaseSideDirection = new Vec3(1.0, 0.0, 0.0);
    private @Nullable UUID targetId;
    private @Nullable Entity cachedTarget;

    private int clientPhase = -1;
    private int clientPhaseAge;
    private int clientSpinDirection = 1;
    private float clientSpinDegrees;
    private float clientPrevSpinDegrees;
    private Vec3 clientLastTrailPosition;

    public MagicSpearMissileEntity(EntityType<? extends MagicSpearMissileEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setViewScale(8.0f);
    }

    public MagicSpearMissileEntity(EntityType<? extends MagicSpearMissileEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PHASE, PHASE_RELEASE);
        builder.define(DATA_SPIN_DIRECTION, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickClient();
            return;
        }
        tickServer();
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return getPhase() != PHASE_BURST
                && entity != getOwner()
                && CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(entity), getOwner())
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide || getPhase() == PHASE_BURST) {
            return;
        }

        var target = CombatTools.resolutePartEntity(hit.getEntity());
        if (CombatTools.isValidCombatTarget(target, getOwner())) {
            // 命中判定は移動前に次tick位置まで先読みするため、爆発中心も1tick分進めて見た目とダメージを揃える。
            explode(position().add(getDeltaMovement()));
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide && getPhase() != PHASE_BURST) {
            explode(hit.getLocation());
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved() || getPhase() == PHASE_BURST) {
            return;
        }

        // burst 表示は同一エンティティで残るため、二発目以降の Counterspell では再初期化しない。
        burstWithoutDamage(position());
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("SpinDirection", getSpinDirection());
        tag.putDouble("MovementDirectionX", movementDirection.x);
        tag.putDouble("MovementDirectionY", movementDirection.y);
        tag.putDouble("MovementDirectionZ", movementDirection.z);
        tag.putDouble("ReleaseSideDirectionX", releaseSideDirection.x);
        tag.putDouble("ReleaseSideDirectionY", releaseSideDirection.y);
        tag.putDouble("ReleaseSideDirectionZ", releaseSideDirection.z);
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        setPhase(tag.getInt("Phase"));
        phaseTicks = tag.getInt("PhaseTicks");
        setSpinDirection(tag.getInt("SpinDirection") >= 0 ? 1 : -1);
        movementDirection = readDirection(tag, "MovementDirection", movementDirection);
        releaseSideDirection = readDirection(tag, "ReleaseSideDirection", releaseSideDirection);
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 160.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var inflate = getPhase() == PHASE_BURST ? BURST_CUBE_SIZE + 1.0 : 2.0;
        return getBoundingBox().inflate(inflate);
    }

    public void setup(float damage, Vec3 forwardDirection, Vec3 sideDirection, @Nullable Entity target) {
        this.damage = damage;
        movementDirection = normalizeOrFallback(forwardDirection, movementDirection);
        releaseSideDirection = normalizeOrFallback(sideDirection, releaseSideDirection);
        if (target != null) {
            targetId = target.getUUID();
            cachedTarget = target;
        }
        setSpinDirection(level().random.nextBoolean() ? 1 : -1);
        setRotationByDirection(movementDirection);
        setDeltaMovement(releaseSideDirection.scale(RELEASE_SIDE_SPEED).add(movementDirection.scale(RELEASE_FORWARD_SPEED)));
    }

    public boolean isBoosting() {
        return getPhase() == PHASE_BOOST;
    }

    public boolean isBursting() {
        return getPhase() == PHASE_BURST;
    }

    public float getBurstCubeScale(float partialTicks) {
        if (!isBursting()) {
            return 0.0f;
        }
        var age = clientPhase == PHASE_BURST ? clientPhaseAge + Math.min(partialTicks, 1.0f) : 0.0f;
        return Mth.lerp(easeOutCubic(Mth.clamp(age / 4.0f, 0.0f, 1.0f)), 0.45f, BURST_CUBE_SIZE);
    }

    public float getBurstCubeAlpha(float partialTicks) {
        if (!isBursting()) {
            return 0.0f;
        }
        var age = clientPhase == PHASE_BURST ? clientPhaseAge + Math.min(partialTicks, 1.0f) : 0.0f;
        if (age <= 5.0f) {
            return 0.95f;
        }
        return 0.95f * (1.0f - easeInCubic(Mth.clamp((age - 5.0f) / (BURST_TICKS - 5.0f), 0.0f, 1.0f)));
    }

    public float getBurstSpinDegrees(float partialTicks) {
        return Mth.rotLerp(partialTicks, clientPrevSpinDegrees, clientSpinDegrees);
    }

    private void tickServer() {
        if (tickCount > MAX_LIFE_TICKS && getPhase() != PHASE_BURST) {
            discard();
            return;
        }

        switch (getPhase()) {
            case PHASE_RELEASE -> tickRelease();
            case PHASE_BOOST -> tickBoost();
            case PHASE_BURST -> tickBurst();
            default -> discard();
        }
    }

    private void tickRelease() {
        var movement = releaseSideDirection.scale(RELEASE_SIDE_SPEED).add(movementDirection.scale(RELEASE_FORWARD_SPEED));
        if (!moveWithImpactCheck(movement)) {
            return;
        }
        ++phaseTicks;
        if (phaseTicks >= RELEASE_TICKS) {
            startBoost();
        }
    }

    private void tickBoost() {
        var target = getLockedTarget();
        if (isValidTarget(target)) {
            var targetDirection = RaycastTools.getEntityTargetPosition(target).subtract(position());
            movementDirection = RotationTools.steerTowards(
                    movementDirection,
                    normalizeOrFallback(targetDirection, movementDirection),
                    HOMING_TURN_DEGREES
            );
        }
        moveWithImpactCheck(movementDirection.scale(BOOST_SPEED));
        ++phaseTicks;
    }

    private void tickBurst() {
        setDeltaMovement(Vec3.ZERO);
        ++phaseTicks;
        if (phaseTicks >= BURST_TICKS) {
            discard();
        }
    }

    private void tickClient() {
        updateClientAnimationState();
        if (getPhase() == PHASE_BOOST) {
            spawnBoostParticles();
        }
    }

    private void startBoost() {
        setPhase(PHASE_BOOST);
        phaseTicks = 0;
        setDeltaMovement(movementDirection.scale(BOOST_SPEED));
        setRotationByDirection(movementDirection);
    }

    private void explode(Vec3 center) {
        setPhase(PHASE_BURST);
        phaseTicks = 0;
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x, center.y, center.z);
        applyBlastDamage(center);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 0.95f, 1.15f + level().random.nextFloat() * 0.12f);
        }
    }

    private void burstWithoutDamage(Vec3 center) {
        setPhase(PHASE_BURST);
        phaseTicks = 0;
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x, center.y, center.z);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.playSound(null, BlockPos.containing(center), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.85f, 1.1f + level().random.nextFloat() * 0.12f);
        }
    }

    private boolean moveWithImpactCheck(Vec3 movement) {
        setDeltaMovement(movement);
        var hitResult = findImpactResult(movement);
        if (hitResult != null && !EventHooks.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }
        if (isRemoved() || getPhase() == PHASE_BURST) {
            return false;
        }

        move(MoverType.SELF, movement);
        setRotationByDirection(movement);
        return true;
    }

    private @Nullable HitResult findImpactResult(Vec3 movement) {
        if (movement.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            return null;
        }

        var start = position();
        var end = start.add(movement);
        var blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                start,
                end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));
        var entitySearchEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
        var entityHit = ProjectileUtil.getEntityHitResult(
                level(),
                this,
                start,
                entitySearchEnd,
                getBoundingBox().expandTowards(movement).inflate(ENTITY_IMPACT_INFLATE),
                this::canHitEntity
        );
        if (entityHit != null) {
            if (blockHit.getType() != HitResult.Type.BLOCK
                    || start.distanceToSqr(entityHit.getLocation()) <= start.distanceToSqr(blockHit.getLocation())) {
                return entityHit;
            }
        }

        return blockHit.getType() == HitResult.Type.BLOCK ? blockHit : null;
    }

    private void applyBlastDamage(Vec3 center) {
        var owner = getOwner();
        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.MAGIC_SPEAR);
        var area = new AABB(center, center).inflate(BLAST_RADIUS);
        var damagedIds = new HashSet<Integer>();

        for (var rawTarget : level().getEntities(this, area, Entity::isAlive)) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!(target instanceof LivingEntity) || !CombatTools.isValidCombatTarget(target, owner) || !damagedIds.add(target.getId())) {
                continue;
            }
            CombatTools.applyDamage(
                    target,
                    damage,
                    source,
                    SpellRegistry.MAGIC_SPEAR.get().getSchoolType(),
                    CombatTools.KnockbackTypes.DEFAULT
            );
        }
    }

    private @Nullable Entity getLockedTarget() {
        if (targetId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (isValidTarget(cachedTarget)) {
            return cachedTarget;
        }
        cachedTarget = serverLevel.getEntity(targetId);
        return cachedTarget;
    }

    private boolean isValidTarget(@Nullable Entity target) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == level()
                && CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(target), getOwner());
    }

    private void setRotationByDirection(Vec3 direction) {
        var normalized = normalizeOrFallback(direction, movementDirection);
        var yawPitch = RotationTools.calculateYawPitchByDirection(normalized);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void updateClientAnimationState() {
        clientPrevSpinDegrees = clientSpinDegrees;
        if (clientPhase != getPhase()) {
            clientPhase = getPhase();
            clientPhaseAge = 0;
            clientLastTrailPosition = position();
            clientSpinDirection = getSpinDirection();
        } else {
            ++clientPhaseAge;
        }

        var spinSpeed = clientPhase == PHASE_BURST ? 22.0f : 0.0f;
        clientSpinDegrees = Mth.wrapDegrees(clientSpinDegrees + spinSpeed * clientSpinDirection);
    }

    private void spawnBoostParticles() {
        var random = level().random;
        var current = position();
        var direction = normalizeOrFallback(getDeltaMovement(), movementDirection);
        var nozzle = current.subtract(direction.scale(0.55));
        var flameVelocity = direction.scale(-0.08);

        level().addParticle(createBurnerRhombus(0.18f + random.nextFloat() * 0.05f),
                nozzle.x, nozzle.y, nozzle.z, flameVelocity.x, flameVelocity.y, flameVelocity.z);
        for (var i = 0; i < 2; ++i) {
            var jitter = randomOffset(0.12);
            level().addParticle(createBurnerSpark(0.10f + random.nextFloat() * 0.04f),
                    nozzle.x + jitter.x, nozzle.y + jitter.y, nozzle.z + jitter.z,
                    flameVelocity.x + (random.nextDouble() - 0.5) * 0.02,
                    flameVelocity.y + (random.nextDouble() - 0.5) * 0.02,
                    flameVelocity.z + (random.nextDouble() - 0.5) * 0.02);
        }

        spawnSmokeTrail(current, direction);
        clientLastTrailPosition = current;
    }

    private void spawnSmokeTrail(Vec3 current, Vec3 direction) {
        if (clientLastTrailPosition == null) {
            clientLastTrailPosition = current;
            return;
        }

        var delta = current.subtract(clientLastTrailPosition);
        var distance = delta.length();
        var steps = Mth.clamp((int) Math.ceil(distance / 0.35), 1, 8);
        var random = level().random;
        for (var i = 0; i < steps; ++i) {
            var t = (i + random.nextDouble()) / steps;
            var base = clientLastTrailPosition.lerp(current, t).subtract(direction.scale(0.7 + random.nextDouble() * 0.6));
            var jitter = randomOffset(0.18);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    base.x + jitter.x,
                    base.y + jitter.y,
                    base.z + jitter.z,
                    -direction.x * 0.006 + (random.nextDouble() - 0.5) * 0.01,
                    0.003 + random.nextDouble() * 0.006,
                    -direction.z * 0.006 + (random.nextDouble() - 0.5) * 0.01);
        }
    }

    private Vec3 randomOffset(double scale) {
        var random = level().random;
        return new Vec3(
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale
        );
    }

    private int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    private int getSpinDirection() {
        return entityData.get(DATA_SPIN_DIRECTION) >= 0 ? 1 : -1;
    }

    private void setSpinDirection(int spinDirection) {
        entityData.set(DATA_SPIN_DIRECTION, spinDirection >= 0 ? 1 : -1);
    }

    private static Vec3 readDirection(CompoundTag tag, String prefix, Vec3 fallback) {
        if (tag.contains(prefix + "X") && tag.contains(prefix + "Y") && tag.contains(prefix + "Z")) {
            return normalizeOrFallback(new Vec3(
                    tag.getDouble(prefix + "X"),
                    tag.getDouble(prefix + "Y"),
                    tag.getDouble(prefix + "Z")
            ), fallback);
        }
        return fallback;
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector != null && vector.lengthSqr() > MIN_DIRECTION_LENGTH_SQR) {
            return vector.normalize();
        }
        if (fallback != null && fallback.lengthSqr() > MIN_DIRECTION_LENGTH_SQR) {
            return fallback.normalize();
        }
        return new Vec3(0.0, 0.0, 1.0);
    }

    private static AdditiveGlowParticleOptions createBurnerRhombus(float size) {
        return createBurnerParticle(ParticleRegistry.ADDITIVE_RHOMBUS.get(), size);
    }

    private static AdditiveGlowParticleOptions createBurnerSpark(float size) {
        return createBurnerParticle(ParticleRegistry.ADDITIVE_SPARK.get(), size);
    }

    private static AdditiveGlowParticleOptions createBurnerParticle(net.minecraft.core.particles.ParticleType<AdditiveGlowParticleOptions> type,
                                                                    float size) {
        return new AdditiveGlowParticleOptions(
                type,
                size,
                BURNER_RED,
                BURNER_GREEN,
                BURNER_BLUE,
                BURNER_WHITEN_TICKS,
                BURNER_LIFETIME,
                BURNER_LIFETIME_VARIANCE,
                BURNER_MIN_SIZE_MULTIPLIER,
                BURNER_MAX_SIZE_MULTIPLIER,
                BURNER_MIN_ALPHA,
                BURNER_MAX_ALPHA,
                BURNER_FADE_IN_END,
                BURNER_FADE_OUT_START,
                BURNER_END_SCALE_MULTIPLIER,
                true
        );
    }

    private static float easeOutCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        var inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float easeInCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
