package jp.aquafactory.apprenticecodex.spell.uniteluna;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
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

import java.util.HashSet;

public class UniteLunaMoonEntity extends Projectile implements AntiMagicSusceptible {
    public static final float CUBE_SIZE = 1.5f;

    private static final int PHASE_DECELERATE = 0;
    private static final int PHASE_HOMING = 2;
    private static final int PHASE_BURST = 3;

    public static final int BURST_KIND_NONE = 0;
    public static final int BURST_KIND_DISSIPATE = 1;
    public static final int BURST_KIND_EXPLOSION = 2;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(UniteLunaMoonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BURST_KIND =
            SynchedEntityData.defineId(UniteLunaMoonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIN_DIRECTION =
            SynchedEntityData.defineId(UniteLunaMoonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BURST_CUBE_SIZE =
            SynchedEntityData.defineId(UniteLunaMoonEntity.class, EntityDataSerializers.FLOAT);

    private static final int DECELERATE_DURATION_TICKS = 15;
    private static final int HOMING_ACCEL_DURATION_TICKS = 80;
    private static final int HOMING_MAX_DURATION_TICKS = 120;
    private static final int DISSIPATE_DURATION_TICKS = 10;
    private static final int EXPLOSION_DURATION_TICKS = 20;

    private static final double START_SPEED = 0.5;
    private static final double GLIDE_SPEED = 0.1;
    private static final double HOMING_MAX_SPEED = 1.5;
    private static final double FLOOR_PROBE_DEPTH = 0.55;
    private static final double ENTITY_IMPACT_INFLATE = 0.5;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0e-6;
    private static final float MIN_BURST_CUBE_SIZE = 7.0f;
    private static final float MAX_BURST_CUBE_SIZE = 11.0f;
    private static final float COUNTERSPELL_BURST_CUBE_SIZE = MAX_BURST_CUBE_SIZE * 2.0f;
    private static final float COUNTERSPELL_DAMAGE_MULTIPLIER = 3.0f;
    private static final int COUNTERSPELL_BURST_PARTICLE_MULTIPLIER = 2;

    private static final float SPARK_RED = 0.9f;
    private static final float SPARK_GREEN = 0.97f;
    private static final float SPARK_BLUE = 1.0f;
    private static final int SPARK_WHITEN_TICKS = 8;
    private static final int TRAIL_SPARK_LIFETIME = 17;
    private static final int TRAIL_SPARK_LIFETIME_VARIANCE = 5;
    private static final float TRAIL_SPARK_MIN_SIZE_MULTIPLIER = 1.0f;
    private static final float TRAIL_SPARK_MAX_SIZE_MULTIPLIER = 1.28f;
    private static final float TRAIL_SPARK_MIN_ALPHA = 0.82f;
    private static final float TRAIL_SPARK_MAX_ALPHA = 0.98f;
    private static final float TRAIL_SPARK_FADE_IN_END = 0.05f;
    private static final float TRAIL_SPARK_FADE_OUT_START = 0.76f;
    private static final float TRAIL_SPARK_END_SCALE_MULTIPLIER = 0.82f;
    private static final int BURST_SPARK_LIFETIME = 28;
    private static final int BURST_SPARK_LIFETIME_VARIANCE = 8;
    private static final float BURST_SPARK_MIN_SIZE_MULTIPLIER = 1.05f;
    private static final float BURST_SPARK_MAX_SIZE_MULTIPLIER = 1.45f;
    private static final float BURST_SPARK_MIN_ALPHA = 0.88f;
    private static final float BURST_SPARK_MAX_ALPHA = 1.0f;
    private static final float BURST_SPARK_FADE_IN_END = 0.04f;
    private static final float BURST_SPARK_FADE_OUT_START = 0.78f;
    private static final float BURST_SPARK_END_SCALE_MULTIPLIER = 0.72f;
    private static final int BURST_PARTICLE_COUNT_PER_TICK = 8;
    private static final int BURST_PARTICLE_EMIT_TICKS = 10;

    private float damage;
    private int phaseTicks;
    private int spinDirection = 1;
    private float burstCubeSize = MIN_BURST_CUBE_SIZE;
    private Vec3 movementDirection = new Vec3(0.0, 0.0, 1.0);

    private int clientPhase = -1;
    private int clientPhaseAge;
    private int clientSpinDirection = 1;
    private float clientSpinDegrees;
    private float clientPrevSpinDegrees;
    private Vec3 clientLastTrailPosition;

    public UniteLunaMoonEntity(EntityType<? extends UniteLunaMoonEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setViewScale(10.0f);
        refreshDimensions();
    }

    public UniteLunaMoonEntity(EntityType<? extends UniteLunaMoonEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PHASE, PHASE_DECELERATE);
        entityData.define(DATA_BURST_KIND, BURST_KIND_NONE);
        entityData.define(DATA_SPIN_DIRECTION, 1);
        entityData.define(DATA_BURST_CUBE_SIZE, MIN_BURST_CUBE_SIZE);
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
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(CUBE_SIZE, CUBE_SIZE);
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
            explode(hit.getLocation());
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide || getPhase() == PHASE_BURST) {
            return;
        }
        explode(hit.getLocation());
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved() || getPhase() == PHASE_BURST) {
            return;
        }

        // Counterspell を受けても無害化せず、吸収した魔力で爆発を過熱させる。
        explodeByAntiMagic(position());
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("BurstKind", getBurstKind());
        tag.putInt("SpinDirection", spinDirection);
        tag.putFloat("BurstCubeSize", burstCubeSize);
        tag.putDouble("MovementDirectionX", movementDirection.x);
        tag.putDouble("MovementDirectionY", movementDirection.y);
        tag.putDouble("MovementDirectionZ", movementDirection.z);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (tag.contains("Phase")) {
            setPhase(tag.getInt("Phase"));
        }
        if (tag.contains("PhaseTicks")) {
            phaseTicks = tag.getInt("PhaseTicks");
        }
        if (tag.contains("BurstKind")) {
            setBurstKind(tag.getInt("BurstKind"));
        }
        if (tag.contains("SpinDirection")) {
            spinDirection = tag.getInt("SpinDirection") >= 0 ? 1 : -1;
            entityData.set(DATA_SPIN_DIRECTION, spinDirection);
        }
        if (tag.contains("BurstCubeSize")) {
            burstCubeSize = Mth.clamp(tag.getFloat("BurstCubeSize"), MIN_BURST_CUBE_SIZE, COUNTERSPELL_BURST_CUBE_SIZE);
            entityData.set(DATA_BURST_CUBE_SIZE, burstCubeSize);
        }
        if (tag.contains("MovementDirectionX") && tag.contains("MovementDirectionY") && tag.contains("MovementDirectionZ")) {
            movementDirection = normalizeOrFallback(new Vec3(
                    tag.getDouble("MovementDirectionX"),
                    tag.getDouble("MovementDirectionY"),
                    tag.getDouble("MovementDirectionZ")
            ), movementDirection);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 160.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var inflate = getPhase() == PHASE_BURST && getBurstKind() == BURST_KIND_EXPLOSION ? getBurstCubeSize() * 0.5f + 1.0f : 2.0f;
        return getBoundingBox().inflate(inflate);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void shoot(Vec3 direction) {
        movementDirection = normalizeOrFallback(direction, new Vec3(0.0, 0.0, 1.0));
        spinDirection = level().random.nextBoolean() ? 1 : -1;
        entityData.set(DATA_SPIN_DIRECTION, spinDirection);
        setDeltaMovement(movementDirection.scale(START_SPEED));
    }

    public float getMainCubeAlpha(float partialTicks) {
        if (clientPhase < 0) {
            return 0.0f;
        }
        var age = clientPhaseAge + Math.min(partialTicks, 1.0f);
        if (clientPhase == PHASE_DECELERATE) {
            return sineEaseOut(Mth.clamp(age / DECELERATE_DURATION_TICKS, 0.0f, 1.0f));
        }
        if (clientPhase == PHASE_BURST) {
            return 1.0f - easeOutCubic(Mth.clamp(age / DISSIPATE_DURATION_TICKS, 0.0f, 1.0f));
        }
        return 1.0f;
    }

    public float getBurstCubeScale(float partialTicks) {
        if (clientPhase != PHASE_BURST || getBurstKind() != BURST_KIND_EXPLOSION) {
            return 0.0f;
        }
        var age = clientPhaseAge + Math.min(partialTicks, 1.0f);
        return Mth.lerp(easeOutCubic(Mth.clamp(age / 5.0f, 0.0f, 1.0f)), 1.0f, getBurstCubeSize());
    }

    public float getBurstCubeAlpha(float partialTicks) {
        if (clientPhase != PHASE_BURST || getBurstKind() != BURST_KIND_EXPLOSION) {
            return 0.0f;
        }
        var age = clientPhaseAge + Math.min(partialTicks, 1.0f);
        if (age <= 10.0f) {
            return 1.0f;
        }
        return 1.0f - easeInCubic(Mth.clamp((age - 10.0f) / 10.0f, 0.0f, 1.0f));
    }

    public float getSpinDegrees(float partialTicks) {
        return Mth.rotLerp(partialTicks, clientPrevSpinDegrees, clientSpinDegrees);
    }

    public float getBurstSpinDegrees(float partialTicks) {
        return getSpinDegrees(partialTicks) * -3.0f;
    }

    public float getBurstCubeSize() {
        return entityData.get(DATA_BURST_CUBE_SIZE);
    }

    private void tickServer() {
        switch (getPhase()) {
            case PHASE_DECELERATE -> tickDecelerate();
            case PHASE_HOMING -> tickHoming();
            case PHASE_BURST -> tickBurst();
            default -> discard();
        }
    }

    private void tickClient() {
        updateClientAnimationState();
        spawnClientParticles();
    }

    private void tickDecelerate() {
        var speed = Mth.lerp(Math.min(1.0f, phaseTicks / (float) DECELERATE_DURATION_TICKS), (float) START_SPEED, (float) GLIDE_SPEED);
        movementDirection = reboundFromFloor(movementDirection, speed);
        if (!moveWithImpactCheck(speed)) {
            return;
        }
        ++phaseTicks;
        if (phaseTicks >= DECELERATE_DURATION_TICKS) {
            startGlide();
        }
    }

    private void tickHoming() {
        ++phaseTicks;
        if (phaseTicks > HOMING_MAX_DURATION_TICKS) {
            // 空へ抜けた時に entity が残り続けないよう、十分長い直進後は静かに消す。
            startBurst(BURST_KIND_DISSIPATE, position());
            return;
        }

        // 旧追尾フェーズ名を流用しているが、ここでは探索せず直進加速のみを行う。
        var speed = Mth.lerp(
                Mth.clamp(phaseTicks / (float) HOMING_ACCEL_DURATION_TICKS, 0.0f, 1.0f),
                (float) GLIDE_SPEED,
                (float) HOMING_MAX_SPEED
        );
        movementDirection = reboundFromFloor(movementDirection, speed);
        moveWithImpactCheck(speed);
    }

    private void tickBurst() {
        setDeltaMovement(Vec3.ZERO);
        ++phaseTicks;
        var duration = getBurstKind() == BURST_KIND_EXPLOSION ? EXPLOSION_DURATION_TICKS : DISSIPATE_DURATION_TICKS;
        if (phaseTicks >= duration) {
            discard();
        }
    }

    private void startGlide() {
        startHoming();
    }

    private void startHoming() {
        setPhase(PHASE_HOMING);
        phaseTicks = 0;
        setBurstKind(BURST_KIND_NONE);
        setDeltaMovement(movementDirection.scale(GLIDE_SPEED));
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_LAUNCH.get(), SoundSource.PLAYERS, 0.55f, 0.92f, 0.03f);
    }

    private void explode(Vec3 center) {
        startBurst(BURST_KIND_EXPLOSION, center, pickNormalBurstCubeSize());
        applyBurstDamage(center, 1.0f);
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_EXPLODE.get(), SoundSource.PLAYERS, 1.5f, 0.92f, 0.04f);
    }

    private void startBurst(int burstKind, Vec3 center) {
        startBurst(burstKind, center, burstKind == BURST_KIND_EXPLOSION ? pickNormalBurstCubeSize() : MIN_BURST_CUBE_SIZE);
    }

    private void explodeByAntiMagic(Vec3 center) {
        startBurst(BURST_KIND_EXPLOSION, center, COUNTERSPELL_BURST_CUBE_SIZE);
        applyBurstDamage(center, COUNTERSPELL_DAMAGE_MULTIPLIER);
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_EXPLODE.get(), SoundSource.PLAYERS, 1.8f, 0.78f, 0.04f);
    }

    private void startBurst(int burstKind, Vec3 center, float newBurstCubeSize) {
        setPhase(PHASE_BURST);
        setBurstKind(burstKind);
        phaseTicks = 0;
        burstCubeSize = Mth.clamp(newBurstCubeSize, MIN_BURST_CUBE_SIZE, COUNTERSPELL_BURST_CUBE_SIZE);
        entityData.set(DATA_BURST_CUBE_SIZE, burstCubeSize);
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x, center.y, center.z);
    }

    private float pickNormalBurstCubeSize() {
        return Mth.lerp(level().random.nextFloat(), MIN_BURST_CUBE_SIZE, MAX_BURST_CUBE_SIZE);
    }

    private boolean moveWithImpactCheck(double speed) {
        setDeltaMovement(movementDirection.scale(speed));
        var hitResult = findImpactResult(getDeltaMovement());
        if (hitResult != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }
        if (isRemoved() || getPhase() == PHASE_BURST) {
            return false;
        }

        move(MoverType.SELF, getDeltaMovement());
        return true;
    }

    private HitResult findImpactResult(Vec3 movement) {
        if (movement.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            return null;
        }

        var start = position();
        var end = start.add(movement);
        var blockHit = level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
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

    private void applyBurstDamage(Vec3 center, float damageMultiplier) {
        var owner = getOwner();
        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.UNITE_LUNA);
        var burstHalfExtent = burstCubeSize * 0.5f;
        var area = new AABB(
                center.x - burstHalfExtent, center.y - burstHalfExtent, center.z - burstHalfExtent,
                center.x + burstHalfExtent, center.y + burstHalfExtent, center.z + burstHalfExtent
        );
        var damagedIds = new HashSet<Integer>();

        for (var rawTarget : level().getEntities(this, area, entity -> entity != owner && CombatTools.isValidCombatTarget(entity, owner))) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (target == owner || !damagedIds.add(target.getId())) {
                continue;
            }
            CombatTools.applyDamage(target, damage * damageMultiplier, source, SpellRegistry.UNITE_LUNA.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }
    }

    private Vec3 reboundFromFloor(Vec3 direction, double speed) {
        var normalized = normalizeOrFallback(direction, movementDirection);
        if (normalized.y >= -1.0e-4) {
            return normalized;
        }

        var probe = level().clip(new ClipContext(
                position(),
                position().add(normalized.scale(Math.max(speed, GLIDE_SPEED))).add(0.0, -FLOOR_PROBE_DEPTH, 0.0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (probe.getType() != HitResult.Type.BLOCK || probe.getDirection().getStepY() <= 0) {
            return normalized;
        }

        // 追尾しない設計では反発よりも床沿いに滑らせた方が狙いを維持しやすい。
        return normalizeOrFallback(new Vec3(normalized.x, 0.0, normalized.z), movementDirection);
    }

    private void updateClientAnimationState() {
        clientPrevSpinDegrees = clientSpinDegrees;
        if (clientPhase != getPhase()) {
            clientPhase = getPhase();
            clientPhaseAge = 0;
            clientLastTrailPosition = position();
            clientSpinDirection = entityData.get(DATA_SPIN_DIRECTION) >= 0 ? 1 : -1;
        } else {
            ++clientPhaseAge;
        }

        float spinSpeed;
        if (clientPhase == PHASE_BURST && getBurstKind() == BURST_KIND_EXPLOSION) {
            spinSpeed = 9.0f;
        } else if (clientPhase == PHASE_HOMING) {
            var progress = Mth.clamp(clientPhaseAge / (float) HOMING_ACCEL_DURATION_TICKS, 0.0f, 1.0f);
            spinSpeed = Mth.lerp(progress, 2.4f, 7.2f);
        } else if (clientPhase == PHASE_DECELERATE) {
            spinSpeed = 2.4f;
        } else {
            spinSpeed = 1.8f;
        }
        clientSpinDegrees = Mth.wrapDegrees(clientSpinDegrees + spinSpeed * clientSpinDirection);
    }

    private void spawnClientParticles() {
        if (clientPhase < 0) {
            return;
        }
        if (clientPhase == PHASE_BURST) {
            if (getBurstKind() == BURST_KIND_EXPLOSION && clientPhaseAge < BURST_PARTICLE_EMIT_TICKS) {
                spawnBurstParticles();
            }
            return;
        }
        spawnTrailParticles();
    }

    private void spawnTrailParticles() {
        var random = level().random;
        var center = position();
        var velocityBase = getDeltaMovement().scale(-0.05);

        for (var i = 0; i < 2; ++i) {
            var offset = new Vec3(
                    (random.nextDouble() - 0.5) * CUBE_SIZE,
                    (random.nextDouble() - 0.5) * CUBE_SIZE,
                    (random.nextDouble() - 0.5) * CUBE_SIZE
            );
            var velocity = velocityBase.add(
                    (random.nextDouble() - 0.5) * 0.008,
                    (random.nextDouble() - 0.5) * 0.008 + 0.003,
                    (random.nextDouble() - 0.5) * 0.008
            );
            level().addParticle(createTrailSpark(0.16f + random.nextFloat() * 0.05f), center.x + offset.x, center.y + offset.y, center.z + offset.z,
                    velocity.x, velocity.y, velocity.z);
        }

        spawnInterpolatedTrail(center);
        clientLastTrailPosition = center;
    }

    private void spawnInterpolatedTrail(Vec3 current) {
        if (clientLastTrailPosition == null) {
            return;
        }

        var delta = current.subtract(clientLastTrailPosition);
        var distance = delta.length();
        if (distance < 0.2) {
            return;
        }

        var direction = normalizeOrFallback(delta, movementDirection);
        var steps = Mth.clamp((int) Math.floor(distance / 0.25), 1, 6);
        var random = level().random;
        for (var i = 1; i <= steps; ++i) {
            var t = i / (double) (steps + 1);
            var position = clientLastTrailPosition.lerp(current, t).add(
                    (random.nextDouble() - 0.5) * CUBE_SIZE,
                    (random.nextDouble() - 0.5) * CUBE_SIZE,
                    (random.nextDouble() - 0.5) * CUBE_SIZE
            );
            var velocity = direction.scale(-0.03).add(
                    (random.nextDouble() - 0.5) * 0.008,
                    (random.nextDouble() - 0.5) * 0.008,
                    (random.nextDouble() - 0.5) * 0.008
            );
            level().addParticle(createTrailSpark(0.13f + random.nextFloat() * 0.04f), position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private void spawnBurstParticles() {
        var random = level().random;
        var particleCount = BURST_PARTICLE_COUNT_PER_TICK;
        if (getBurstCubeSize() > MAX_BURST_CUBE_SIZE) {
            particleCount *= COUNTERSPELL_BURST_PARTICLE_MULTIPLIER;
        }
        for (var i = 0; i < particleCount; ++i) {
            var offset = createBurstShellOffset(random);
            var velocity = normalizeOrFallback(offset, movementDirection).scale(0.03 + random.nextDouble() * 0.14).add(
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015
            );
            level().addParticle(createBurstSpark(0.20f + random.nextFloat() * 0.08f), getX() + offset.x, getY() + offset.y, getZ() + offset.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private Vec3 createBurstShellOffset(net.minecraft.util.RandomSource random) {
        var halfExtent = getBurstCubeSize() * 0.5f;
        var shellExtent = halfExtent * Mth.lerp(random.nextFloat(), 0.82f, 1.0f);
        var face = random.nextInt(6);
        var x = (random.nextDouble() * 2.0 - 1.0) * shellExtent;
        var y = (random.nextDouble() * 2.0 - 1.0) * shellExtent;
        var z = (random.nextDouble() * 2.0 - 1.0) * shellExtent;

        switch (face) {
            case 0 -> x = shellExtent;
            case 1 -> x = -shellExtent;
            case 2 -> y = shellExtent;
            case 3 -> y = -shellExtent;
            case 4 -> z = shellExtent;
            default -> z = -shellExtent;
        }
        return new Vec3(x, y, z);
    }

    private static AdditiveGlowParticleOptions createTrailSpark(float size) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                size,
                SPARK_RED,
                SPARK_GREEN,
                SPARK_BLUE,
                SPARK_WHITEN_TICKS,
                TRAIL_SPARK_LIFETIME,
                TRAIL_SPARK_LIFETIME_VARIANCE,
                TRAIL_SPARK_MIN_SIZE_MULTIPLIER,
                TRAIL_SPARK_MAX_SIZE_MULTIPLIER,
                TRAIL_SPARK_MIN_ALPHA,
                TRAIL_SPARK_MAX_ALPHA,
                TRAIL_SPARK_FADE_IN_END,
                TRAIL_SPARK_FADE_OUT_START,
                TRAIL_SPARK_END_SCALE_MULTIPLIER,
                true
        );
    }

    private static AdditiveGlowParticleOptions createBurstSpark(float size) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                size,
                SPARK_RED,
                SPARK_GREEN,
                SPARK_BLUE,
                SPARK_WHITEN_TICKS,
                BURST_SPARK_LIFETIME,
                BURST_SPARK_LIFETIME_VARIANCE,
                BURST_SPARK_MIN_SIZE_MULTIPLIER,
                BURST_SPARK_MAX_SIZE_MULTIPLIER,
                BURST_SPARK_MIN_ALPHA,
                BURST_SPARK_MAX_ALPHA,
                BURST_SPARK_FADE_IN_END,
                BURST_SPARK_FADE_OUT_START,
                BURST_SPARK_END_SCALE_MULTIPLIER,
                true
        );
    }

    private int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    public int getBurstKind() {
        return entityData.get(DATA_BURST_KIND);
    }

    private void setBurstKind(int burstKind) {
        entityData.set(DATA_BURST_KIND, burstKind);
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

    private static float easeOutCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        var inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float easeInCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    private static float sineEaseOut(float value) {
        return Mth.sin(Mth.clamp(value, 0.0f, 1.0f) * (Mth.PI / 2.0f));
    }
}
