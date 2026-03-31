package jp.aquafactory.apprenticecodex.spell.illuminatestellar;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

// 多すぎるのでこの弾は大本で潰す.
@SuppressWarnings("resource")
public class IlluminateStellarStarEntity extends Projectile {
    private static final int PHASE_DRIFT = 0;
    private static final int PHASE_WAIT = 1;
    private static final int PHASE_LAUNCH = 2;
    private static final byte EVENT_IMPACT_BURST = 61;
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(IlluminateStellarStarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DRIFT_DURATION =
            SynchedEntityData.defineId(IlluminateStellarStarEntity.class, EntityDataSerializers.INT);

    // この辺は見栄え微調整用なので定数を大量に持つことにする.
    private static final int MIN_DRIFT_TICKS = 20;
    private static final int MAX_DRIFT_TICKS = 25;
    private static final int MIN_WAIT_TICKS = 10;
    private static final int MAX_WAIT_TICKS = 15;
    private static final int LAUNCH_LIFETIME_TICKS = 60;
    private static final double DRIFT_START_SPEED = 0.18;
    private static final double LAUNCH_SPEED = 1.48;
    private static final double SEARCH_RANGE = 32.0;
    private static final double SEARCH_HALF_WIDTH = 16.0;
    private static final double SEARCH_HALF_HEIGHT = 16.0;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0e-6;
    private static final float RHOMBUS_RED = 1.0f;
    private static final float RHOMBUS_GREEN_MIN = 0.74f;
    private static final float RHOMBUS_GREEN_MAX = 0.88f;
    private static final float RHOMBUS_BLUE_MIN = 0.09f;
    private static final float RHOMBUS_BLUE_MAX = 0.19f;
    private static final float RHOMBUS_WARM_SHIFT_MAX = 0.04f;
    private static final float SPARK_RED = 1.0f;
    private static final float SPARK_GREEN_MIN = 0.88f;
    private static final float SPARK_GREEN_MAX = 0.94f;
    private static final float SPARK_BLUE_MIN = 0.11f;
    private static final float SPARK_BLUE_MAX = 0.16f;
    private static final float SPARK_WARM_SHIFT_MAX = 0.03f;
    private static final int RHOMBUS_WHITEN_TICKS = 4;
    private static final int SPARK_WHITEN_TICKS = 10;
    private static final int SPARK_LIFETIME = 12;
    private static final int SPARK_LIFETIME_VARIANCE = 4;
    private static final float SPARK_MIN_SIZE_MULTIPLIER = 1.0f;
    private static final float SPARK_MAX_SIZE_MULTIPLIER = 1.35f;
    private static final float SPARK_MIN_ALPHA = 0.9f;
    private static final float SPARK_MAX_ALPHA = 1.0f;
    private static final float SPARK_FADE_IN_END = 0.06f;
    private static final float SPARK_FADE_OUT_START = 0.72f;
    private static final float SPARK_END_SCALE_MULTIPLIER = 0.72f;
    private static final double LAUNCH_SPARK_INTERPOLATION_STEP = 0.22;
    private static final int MAX_INTERPOLATED_LAUNCH_SPARKS = 6;
    private static final int IMPACT_RHOMBUS_COUNT = 10;
    private static final int IMPACT_SPARK_COUNT = 18;
    private static final float IMPACT_RHOMBUS_SIZE_MIN = 0.24f;
    private static final float IMPACT_RHOMBUS_SIZE_MAX = 0.4f;
    private static final float IMPACT_SPARK_SIZE_MIN = 0.14f;
    private static final float IMPACT_SPARK_SIZE_MAX = 0.2f;
    private static final int IMPACT_SPARK_LIFETIME = 16;
    private static final int IMPACT_SPARK_LIFETIME_VARIANCE = 5;
    private static final float IMPACT_SPARK_MIN_SIZE_MULTIPLIER = 0.95f;
    private static final float IMPACT_SPARK_MAX_SIZE_MULTIPLIER = 1.35f;
    private static final float IMPACT_SPARK_MIN_ALPHA = 0.95f;
    private static final float IMPACT_SPARK_MAX_ALPHA = 1.0f;
    private static final float IMPACT_SPARK_FADE_IN_END = 0.04f;
    private static final float IMPACT_SPARK_FADE_OUT_START = 0.72f;
    private static final float IMPACT_SPARK_END_SCALE_MULTIPLIER = 0.78f;

    private float damage;
    private int waitTicks;
    private int phaseTicks;
    private int targetEntityId = -1;
    private int fallbackTargetEntityId = -1;
    private boolean impactTriggered;
    private Vec3 driftDirection = new Vec3(0.0, 0.0, 1.0);
    private Vec3 launchDirection = new Vec3(0.0, 0.0, 1.0);

    private int clientPhase = -1;
    private int clientPhaseAge;
    private float clientSpinRotation;
    private float clientPrevSpinRotation;
    private Vec3 clientLastSparkTrailPosition;

    public IlluminateStellarStarEntity(EntityType<? extends IlluminateStellarStarEntity> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0f);
        setNoGravity(true);
    }

    public IlluminateStellarStarEntity(EntityType<? extends IlluminateStellarStarEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PHASE, PHASE_DRIFT);
        entityData.define(DATA_DRIFT_DURATION, MIN_DRIFT_TICKS);
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

    private void tickServer() {
        switch (getPhase()) {
            case PHASE_DRIFT -> tickDrift();
            case PHASE_WAIT -> tickWait();
            case PHASE_LAUNCH -> tickLaunch();
            default -> discard();
        }
    }

    private void tickClient() {
        updateClientSpinState();
        if (getPhase() != PHASE_LAUNCH) {
            clientLastSparkTrailPosition = position();
        }
        spawnClientParticles();
    }

    private void tickDrift() {
        var duration = Math.max(1, getDriftDurationTicks());
        var progress = Mth.clamp((float) phaseTicks / (float) duration, 0.0f, 1.0f);
        var speed = DRIFT_START_SPEED * Math.pow(1.0f - progress, 2.0f);
        var movement = speed <= 1.0e-4 ? Vec3.ZERO : driftDirection.scale(speed);
        var blockHit = findBlockCollision(movement);

        if (blockHit != null) {
            var hitPosition = blockHit.getLocation();
            var adjustedPosition = movement.lengthSqr() > MIN_DIRECTION_LENGTH_SQR
                    ? hitPosition.subtract(movement.normalize().scale(0.05))
                    : hitPosition;
            setPos(adjustedPosition);
            startWaitPhase();
            return;
        }

        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        ++phaseTicks;

        if (phaseTicks >= duration) {
            startWaitPhase();
        }
    }

    private void tickWait() {
        setDeltaMovement(Vec3.ZERO);
        ++phaseTicks;
        if (phaseTicks >= waitTicks) {
            startLaunchPhase();
        }
    }

    private void tickLaunch() {
        ++phaseTicks;
        if (phaseTicks > LAUNCH_LIFETIME_TICKS) {
            triggerImpactAndDiscard();
            return;
        }

        updateLaunchDirection();
        setDeltaMovement(launchDirection.scale(LAUNCH_SPEED));

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }

        if (isRemoved()) {
            return;
        }

        move(MoverType.SELF, getDeltaMovement());
    }

    private void startWaitPhase() {
        setPhase(PHASE_WAIT);
        phaseTicks = 0;
        setDeltaMovement(Vec3.ZERO);
    }

    private void startLaunchPhase() {
        setPhase(PHASE_LAUNCH);
        phaseTicks = 0;
        targetEntityId = -1;

        var target = pickLaunchTarget();
        if (target != null) {
            targetEntityId = target.getId();
            launchDirection = normalizeOrFallback(RaycastTools.getEntityTargetPosition(target).subtract(position()), driftDirection);
        } else if (!tryUseFallbackTarget()) {
            // フォールバックターゲットが見つからなければ即座に砕く.
            triggerImpactAndDiscard();
            return;
        }

        setDeltaMovement(launchDirection.scale(LAUNCH_SPEED));
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_LAUNCH.get(), SoundSource.PLAYERS, 0.6f, 1.0f, 0.02f);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return getPhase() == PHASE_LAUNCH && entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) {
            return;
        }

        var owner = getOwner();
        if (!CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            return;
        }

        var target = CombatTools.resolutePartEntity(hit.getEntity());
        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.ILLUMINATE_STELLAR);
        CombatTools.applyDamage(target, damage, source, SpellRegistry.ILLUMINATE_STELLAR.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        triggerImpactAndDiscard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            triggerImpactAndDiscard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == EVENT_IMPACT_BURST && level().isClientSide) {
            spawnImpactBurst();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Phase", getPhase());
        tag.putInt("DriftDurationTicks", getDriftDurationTicks());
        tag.putInt("WaitTicks", waitTicks);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("TargetEntityId", targetEntityId);
        tag.putInt("FallbackTargetEntityId", fallbackTargetEntityId);
        tag.putBoolean("ImpactTriggered", impactTriggered);
        tag.putDouble("DriftDirectionX", driftDirection.x);
        tag.putDouble("DriftDirectionY", driftDirection.y);
        tag.putDouble("DriftDirectionZ", driftDirection.z);
        tag.putDouble("LaunchDirectionX", launchDirection.x);
        tag.putDouble("LaunchDirectionY", launchDirection.y);
        tag.putDouble("LaunchDirectionZ", launchDirection.z);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
        if (tag.contains("Phase")) {
            setPhase(tag.getInt("Phase"));
        }
        if (tag.contains("DriftDurationTicks")) {
            entityData.set(DATA_DRIFT_DURATION, tag.getInt("DriftDurationTicks"));
        }
        if (tag.contains("WaitTicks")) {
            waitTicks = tag.getInt("WaitTicks");
        }
        if (tag.contains("PhaseTicks")) {
            phaseTicks = tag.getInt("PhaseTicks");
        }
        if (tag.contains("TargetEntityId")) {
            targetEntityId = tag.getInt("TargetEntityId");
        }
        if (tag.contains("FallbackTargetEntityId")) {
            fallbackTargetEntityId = tag.getInt("FallbackTargetEntityId");
        }
        if (tag.contains("ImpactTriggered")) {
            impactTriggered = tag.getBoolean("ImpactTriggered");
        }
        if (tag.contains("DriftDirectionX") && tag.contains("DriftDirectionY") && tag.contains("DriftDirectionZ")) {
            driftDirection = normalizeOrFallback(new Vec3(
                    tag.getDouble("DriftDirectionX"),
                    tag.getDouble("DriftDirectionY"),
                    tag.getDouble("DriftDirectionZ")
            ), driftDirection);
        }
        if (tag.contains("LaunchDirectionX") && tag.contains("LaunchDirectionY") && tag.contains("LaunchDirectionZ")) {
            launchDirection = normalizeOrFallback(new Vec3(
                    tag.getDouble("LaunchDirectionX"),
                    tag.getDouble("LaunchDirectionY"),
                    tag.getDouble("LaunchDirectionZ")
            ), launchDirection);
        }
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
        var maxDistance = 128.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setFallbackTarget(Entity target) {
        fallbackTargetEntityId = target != null ? target.getId() : -1;
    }

    public void setDriftProfile(Vec3 direction) {
        var normalizedDirection = normalizeOrFallback(direction, new Vec3(0.0, 0.0, 1.0));
        driftDirection = normalizedDirection;
        launchDirection = normalizedDirection;
        waitTicks = level().random.nextInt(MAX_WAIT_TICKS - MIN_WAIT_TICKS + 1) + MIN_WAIT_TICKS;
        entityData.set(DATA_DRIFT_DURATION, level().random.nextInt(MAX_DRIFT_TICKS - MIN_DRIFT_TICKS + 1) + MIN_DRIFT_TICKS);
        setDeltaMovement(normalizedDirection.scale(DRIFT_START_SPEED));
    }

    public float getSpinDegrees(float partialTicks) {
        return Mth.rotLerp(partialTicks, clientPrevSpinRotation, clientSpinRotation);
    }

    public AABB makeSpawnCheckAabb(Vec3 position) {
        var dimensions = getDimensions(getPose());
        var halfWidth = dimensions.width / 2.0f;
        return new AABB(
                position.x - halfWidth,
                position.y,
                position.z - halfWidth,
                position.x + halfWidth,
                position.y + dimensions.height,
                position.z + halfWidth
        );
    }

    private void updateLaunchDirection() {
        if (targetEntityId < 0) {
            return;
        }

        var target = level().getEntity(targetEntityId);
        if (!(getOwner() instanceof LivingEntity owner)
                || !isLiveCombatTarget(target, owner)
                || !RaycastTools.hasLineOfSight(level(), this, target)) {
            targetEntityId = -1;
            return;
        }

        launchDirection = normalizeOrFallback(RaycastTools.getEntityTargetPosition(target).subtract(position()), launchDirection);
    }

    private Entity pickLaunchTarget() {
        if (!(getOwner() instanceof LivingEntity owner)) {
            return null;
        }

        var forward = normalizeOrFallback(owner.getLookAngle(), driftDirection);
        var origin = owner.getEyePosition();
        var broadAabb = new AABB(origin, origin.add(forward.scale(SEARCH_RANGE))).inflate(SEARCH_HALF_WIDTH + 1.0, SEARCH_HALF_HEIGHT + 1.0, SEARCH_HALF_WIDTH + 1.0);
        var right = computeRightVector(forward);
        var up = right.cross(forward).normalize();
        var candidates = new ArrayList<>(level().getEntities(this, broadAabb, entity ->
                isLiveCombatTarget(entity, owner)
                        && CombatTools.canBeHostileToMe(entity, owner)
        ));

        candidates.removeIf(entity -> {
            var targetPosition = RaycastTools.getEntityTargetPosition(entity);
            var relative = targetPosition.subtract(origin);
            var x = relative.dot(right);
            var y = relative.dot(up);
            var z = relative.dot(forward);
            if (z < 0.0 || z > SEARCH_RANGE) {
                return true;
            }
            if (Math.abs(x) > SEARCH_HALF_WIDTH || Math.abs(y) > SEARCH_HALF_HEIGHT) {
                return true;
            }
            return !RaycastTools.hasLineOfSight(level(), this, entity);
        });

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(level().random.nextInt(candidates.size()));
    }

    private boolean tryUseFallbackTarget() {
        if (!(getOwner() instanceof LivingEntity owner) || fallbackTargetEntityId < 0) {
            return false;
        }

        var target = level().getEntity(fallbackTargetEntityId);
        if (!isLiveCombatTarget(target, owner)) {
            return false;
        }

        launchDirection = normalizeOrFallback(RaycastTools.getEntityTargetPosition(target).subtract(position()), driftDirection);
        return true;
    }

    private BlockHitResult findBlockCollision(Vec3 movement) {
        if (movement.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            return null;
        }

        var hit = level().clip(new ClipContext(
                position(),
                position().add(movement),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private void updateClientSpinState() {
        clientPrevSpinRotation = clientSpinRotation;
        var phase = getPhase();
        if (clientPhase != phase) {
            clientPhase = phase;
            clientPhaseAge = 0;
        } else {
            ++clientPhaseAge;
        }

        float speed;
        if (phase == PHASE_DRIFT) {
            var progress = Mth.clamp((float) clientPhaseAge / (float) Math.max(1, getDriftDurationTicks()), 0.0f, 1.0f);
            speed = Mth.lerp(progress, 28.0f, 4.0f);
        } else if (phase == PHASE_LAUNCH) {
            speed = 28.0f;
        } else {
            speed = 4.0f;
        }

        clientSpinRotation = Mth.wrapDegrees(clientSpinRotation + speed);
    }

    private void spawnClientParticles() {
        var random = level().random;
        var phase = getPhase();
        if (phase != PHASE_LAUNCH) {
            spawnRhombusParticle(random);
        }
        spawnSparkParticle(random);

        if (phase == PHASE_LAUNCH) {
            spawnSparkParticle(random);
            spawnInterpolatedLaunchSparkParticles(random);
        }
    }

    private void spawnImpactBurst() {
        var random = level().random;
        var forward = normalizeOrFallback(launchDirection, driftDirection);
        var right = computeRightVector(forward);
        var up = right.cross(forward).normalize();

        for (var i = 0; i < IMPACT_RHOMBUS_COUNT; ++i) {
            spawnImpactRhombusParticle(random, forward, right, up);
        }
        for (var i = 0; i < IMPACT_SPARK_COUNT; ++i) {
            spawnImpactSparkParticle(random, forward, right, up);
        }
    }

    private void spawnRhombusParticle(net.minecraft.util.RandomSource random) {
        var offset = new Vec3(
                (random.nextDouble() - 0.5) * 0.08,
                (random.nextDouble() - 0.5) * 0.08,
                (random.nextDouble() - 0.5) * 0.08
        );
        var velocity = getDeltaMovement().scale(-0.08).add(
                (random.nextDouble() - 0.5) * 0.004,
                (random.nextDouble() - 0.5) * 0.004 + 0.004,
                (random.nextDouble() - 0.5) * 0.004
        );
        var size = 0.225f * (0.85f + random.nextFloat() * 0.35f);
        var warmth = random.nextFloat() * RHOMBUS_WARM_SHIFT_MAX;
        var green = Mth.lerp(random.nextFloat(), RHOMBUS_GREEN_MIN, RHOMBUS_GREEN_MAX) - warmth;
        var blue = Mth.lerp(random.nextFloat(), RHOMBUS_BLUE_MIN, RHOMBUS_BLUE_MAX) - warmth * 0.5f;

        level().addParticle(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), size, RHOMBUS_RED, clampColor(green), clampColor(blue), RHOMBUS_WHITEN_TICKS),
                getX() + offset.x,
                getY() + offset.y + 0.02,
                getZ() + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void spawnSparkParticle(net.minecraft.util.RandomSource random) {
        spawnSparkParticle(random, position(), 0.05, 0.12f);
    }

    private void spawnInterpolatedLaunchSparkParticles(net.minecraft.util.RandomSource random) {
        var current = position();
        var previous = clientLastSparkTrailPosition;
        clientLastSparkTrailPosition = current;
        if (previous == null) {
            return;
        }

        var travel = current.subtract(previous);
        var distance = travel.length();
        if (distance < LAUNCH_SPARK_INTERPOLATION_STEP) {
            return;
        }

        var direction = normalizeOrFallback(travel, launchDirection);
        var right = computeRightVector(direction);
        var up = right.cross(direction).normalize();
        var extraCount = Mth.clamp((int) Math.floor(distance / LAUNCH_SPARK_INTERPOLATION_STEP), 1, MAX_INTERPOLATED_LAUNCH_SPARKS);
        for (var i = 1; i <= extraCount; ++i) {
            var t = i / (double) (extraCount + 1);
            var jitter = right.scale((random.nextDouble() - 0.5) * 0.32)
                    .add(up.scale((random.nextDouble() - 0.5) * 0.32))
                    .add(direction.scale((random.nextDouble() - 0.5) * 0.16));
            var sparkPosition = previous.lerp(current, t).add(jitter);
            spawnSparkParticle(random, sparkPosition, 0.03, 0.1f);
        }
    }

    private void spawnSparkParticle(net.minecraft.util.RandomSource random, Vec3 center, double offsetScale, float size) {
        // SPARK は preset 既定だと「小さい・短い・最後に強く縮む」が重なって RHOMBUS に埋もれやすい。
        // Illuminate 用だけ寿命と縮小率を override して、細いまま残る軌跡に寄せる。
        var offset = new Vec3(
                (random.nextDouble() - 0.5) * offsetScale,
                (random.nextDouble() - 0.5) * offsetScale,
                (random.nextDouble() - 0.5) * offsetScale
        );
        var velocity = getDeltaMovement().scale(-0.06).add(
                (random.nextDouble() - 0.5) * 0.006,
                (random.nextDouble() - 0.5) * 0.006 + 0.006,
                (random.nextDouble() - 0.5) * 0.006
        );
        var warmth = random.nextFloat() * SPARK_WARM_SHIFT_MAX;
        var green = Mth.lerp(random.nextFloat(), SPARK_GREEN_MIN, SPARK_GREEN_MAX) - warmth;
        var blue = Mth.lerp(random.nextFloat(), SPARK_BLUE_MIN, SPARK_BLUE_MAX) - warmth * 0.5f;

        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        SPARK_RED,
                        clampColor(green),
                        clampColor(blue),
                        SPARK_WHITEN_TICKS,
                        SPARK_LIFETIME,
                        SPARK_LIFETIME_VARIANCE,
                        SPARK_MIN_SIZE_MULTIPLIER,
                        SPARK_MAX_SIZE_MULTIPLIER,
                        SPARK_MIN_ALPHA,
                        SPARK_MAX_ALPHA,
                        SPARK_FADE_IN_END,
                        SPARK_FADE_OUT_START,
                        SPARK_END_SCALE_MULTIPLIER,
                        true
                ),
                center.x + offset.x,
                center.y + offset.y + 0.02,
                center.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void spawnImpactRhombusParticle(net.minecraft.util.RandomSource random, Vec3 forward, Vec3 right, Vec3 up) {
        var offset = randomOffset(random, 0.1);
        var velocity = forward.scale(0.04 + random.nextDouble() * 0.08)
                .add(right.scale((random.nextDouble() - 0.5) * 0.12))
                .add(up.scale((random.nextDouble() - 0.5) * 0.12))
                .add(randomOffset(random, 0.01));
        var size = Mth.lerp(random.nextFloat(), IMPACT_RHOMBUS_SIZE_MIN, IMPACT_RHOMBUS_SIZE_MAX);
        var warmth = random.nextFloat() * RHOMBUS_WARM_SHIFT_MAX;
        var green = Mth.lerp(random.nextFloat(), RHOMBUS_GREEN_MIN, RHOMBUS_GREEN_MAX) - warmth;
        var blue = Mth.lerp(random.nextFloat(), RHOMBUS_BLUE_MIN, RHOMBUS_BLUE_MAX) - warmth * 0.5f;

        level().addParticle(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), size, RHOMBUS_RED, clampColor(green), clampColor(blue), RHOMBUS_WHITEN_TICKS),
                getX() + offset.x,
                getY() + offset.y + 0.02,
                getZ() + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void spawnImpactSparkParticle(net.minecraft.util.RandomSource random, Vec3 forward, Vec3 right, Vec3 up) {
        var offset = randomOffset(random, 0.08);
        var velocity = forward.scale(0.08 + random.nextDouble() * 0.16)
                .add(right.scale((random.nextDouble() - 0.5) * 0.18))
                .add(up.scale((random.nextDouble() - 0.5) * 0.18))
                .add(randomOffset(random, 0.015));
        var size = Mth.lerp(random.nextFloat(), IMPACT_SPARK_SIZE_MIN, IMPACT_SPARK_SIZE_MAX);
        var warmth = random.nextFloat() * SPARK_WARM_SHIFT_MAX;
        var green = Mth.lerp(random.nextFloat(), SPARK_GREEN_MIN, SPARK_GREEN_MAX) - warmth;
        var blue = Mth.lerp(random.nextFloat(), SPARK_BLUE_MIN, SPARK_BLUE_MAX) - warmth * 0.5f;

        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        SPARK_RED,
                        clampColor(green),
                        clampColor(blue),
                        SPARK_WHITEN_TICKS,
                        IMPACT_SPARK_LIFETIME,
                        IMPACT_SPARK_LIFETIME_VARIANCE,
                        IMPACT_SPARK_MIN_SIZE_MULTIPLIER,
                        IMPACT_SPARK_MAX_SIZE_MULTIPLIER,
                        IMPACT_SPARK_MIN_ALPHA,
                        IMPACT_SPARK_MAX_ALPHA,
                        IMPACT_SPARK_FADE_IN_END,
                        IMPACT_SPARK_FADE_OUT_START,
                        IMPACT_SPARK_END_SCALE_MULTIPLIER,
                        true
                ),
                getX() + offset.x,
                getY() + offset.y + 0.02,
                getZ() + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void triggerImpactAndDiscard() {
        if (impactTriggered) {
            discard();
            return;
        }

        impactTriggered = true;
        level().broadcastEntityEvent(this, EVENT_IMPACT_BURST);
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_IMPACT.get(), SoundSource.PLAYERS, 0.7f, 1.0f, 0.04f);
        discard();
    }

    private static Vec3 randomOffset(net.minecraft.util.RandomSource random, double scale) {
        return new Vec3(
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale
        );
    }

    private static float clampColor(float value) {
        return Mth.clamp(value, 0.0f, 1.0f);
    }

    private int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    private int getDriftDurationTicks() {
        return entityData.get(DATA_DRIFT_DURATION);
    }

    private static boolean isLiveCombatTarget(Entity target, Entity owner) {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return false;
        }
        if (!CombatTools.isValidCombatTarget(target, owner)) {
            return false;
        }
        if (target instanceof LivingEntity living && (living.getHealth() <= 0.0f || living.isDeadOrDying())) {
            return false;
        }
        return true;
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

    private static Vec3 computeRightVector(Vec3 forward) {
        var right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            right = forward.cross(new Vec3(1.0, 0.0, 0.0));
        }
        return right.normalize();
    }
}
