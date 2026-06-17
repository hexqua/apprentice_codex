package jp.aquafactory.apprenticecodex.spell.artisansmash;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;

public class ArtisanSmashShellEntity extends ThrowableProjectile implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    public static final double FLIGHT_AIR_DRAG = 0.99d;

    private static final int PHASE_FLYING = 0;
    private static final int PHASE_BURST = 1;
    private static final int MAX_LIFE_TICKS = 20 * 10;
    private static final int BURST_TICKS = 14;
    private static final float BURST_CUBE_SIZE_SCALE = 2.0f;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(ArtisanSmashShellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIN_DIRECTION =
            SynchedEntityData.defineId(ArtisanSmashShellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPLASH_RADIUS =
            SynchedEntityData.defineId(ArtisanSmashShellEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float splashRadius;
    private int phaseTicks;
    private int clientPhase = -1;
    private int clientPhaseAge;
    private int clientSpinDirection = 1;
    private float clientSpinDegrees;
    private float clientPrevSpinDegrees;
    private Vec3 burstCenter;
    @Nullable
    private UUID combatOwnerUuid;

    public ArtisanSmashShellEntity(EntityType<? extends ArtisanSmashShellEntity> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0f);
    }

    public ArtisanSmashShellEntity(EntityType<? extends ArtisanSmashShellEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, owner, level);
        setViewScale(8.0f);
        setCombatOwnerUuid(CombatOwnerResolver.captureCombatOwnerUuid(owner));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PHASE, PHASE_FLYING);
        entityData.define(DATA_SPIN_DIRECTION, 1);
        entityData.define(DATA_SPLASH_RADIUS, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            updateClientAnimationState();
            return;
        }

        if (isBursting()) {
            setDeltaMovement(Vec3.ZERO);
            if (burstCenter != null) {
                setPos(burstCenter.x, burstCenter.y, burstCenter.z);
            }
            ++phaseTicks;
            if (phaseTicks >= BURST_TICKS) {
                discard();
            }
            return;
        }

        if (tickCount > MAX_LIFE_TICKS) {
            discard();
            return;
        }

        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var owner = getOwner();
        var combatOwner = CombatOwnerResolver.resolveCombatOwner(level(), owner, combatOwnerUuid);
        var target = CombatTools.resolutePartEntity(entity);
        return getPhase() == PHASE_FLYING
                && entity != owner
                && entity != combatOwner
                && CombatTools.isValidCombatTarget(target, combatOwner)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide || isBursting()) {
            return;
        }

        var target = CombatTools.resolutePartEntity(hit.getEntity());
        var combatOwner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        if (CombatTools.isValidCombatTarget(target, combatOwner)) {
            explode(hit.getLocation());
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide && !isBursting()) {
            explode(hit.getLocation());
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved() || isBursting()) {
            return;
        }

        discard();
    }

    @Override
    protected float getGravity() {
        return isBursting() ? 0.0f : super.getGravity();
    }

    public float getFlightGravityForPrediction(Vec3 position) {
        setPos(position);
        return getGravity();
    }

    public static ArtisanSmashShellEntity createPredictionProbe(Level level) {
        return new ArtisanSmashShellEntity(EntityRegistry.ARTISAN_SMASH_SHELL.get(), level);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("SplashRadius", splashRadius);
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("SpinDirection", getSpinDirection());
        saveCombatOwnerUuid(tag);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        splashRadius = tag.getFloat("SplashRadius");
        setPhase(tag.getInt("Phase"));
        phaseTicks = tag.getInt("PhaseTicks");
        setSpinDirection(tag.getInt("SpinDirection") >= 0 ? 1 : -1);
        loadCombatOwnerUuid(tag);
        entityData.set(DATA_SPLASH_RADIUS, splashRadius);
        if (isBursting()) {
            setNoGravity(true);
            burstCenter = position();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 128.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var inflate = isBursting() ? getBurstCubeMaxSize() + 1.0 : 2.0;
        return getBoundingBox().inflate(inflate);
    }

    public void setup(float damage, float splashRadius, Vec3 direction, float speed) {
        this.damage = damage;
        this.splashRadius = Math.max(0.0f, splashRadius);
        entityData.set(DATA_SPLASH_RADIUS, this.splashRadius);
        setSpinDirection(level().random.nextBoolean() ? 1 : -1);
        var normalized = normalizeOrFallback(direction);
        setDeltaMovement(normalized.scale(speed));
        var yawPitch = jp.aquafactory.apprenticecodex.utility.RotationTools.calculateYawPitchByDirection(normalized);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    public boolean isBursting() {
        return getPhase() == PHASE_BURST;
    }

    public float getBurstCubeScale(float partialTicks) {
        if (!isBursting()) {
            return 0.0f;
        }
        var age = clientPhase == PHASE_BURST ? clientPhaseAge + Math.min(partialTicks, 1.0f) : phaseTicks + partialTicks;
        return Mth.lerp(easeOutCubic(Mth.clamp(age / 4.0f, 0.0f, 1.0f)), 0.45f, getBurstCubeMaxSize());
    }

    public float getSplashRadius() {
        return splashRadius;
    }

    public float getBurstCubeAlpha(float partialTicks) {
        if (!isBursting()) {
            return 0.0f;
        }
        var age = clientPhase == PHASE_BURST ? clientPhaseAge + Math.min(partialTicks, 1.0f) : phaseTicks + partialTicks;
        if (age <= 5.0f) {
            return 0.95f;
        }
        return 0.95f * (1.0f - easeInCubic(Mth.clamp((age - 5.0f) / (BURST_TICKS - 5.0f), 0.0f, 1.0f)));
    }

    public float getBurstSpinDegrees(float partialTicks) {
        return Mth.rotLerp(partialTicks, clientPrevSpinDegrees, clientSpinDegrees);
    }

    private void explode(Vec3 center) {
        setPhase(PHASE_BURST);
        phaseTicks = 0;
        burstCenter = center;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x, center.y, center.z);
        applyBlastDamage(center);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.95f, 1.0f + level().random.nextFloat() * 0.12f);
        }
    }

    private void applyBlastDamage(Vec3 center) {
        var source = CombatOwnerResolver.createDamageSource(level(), this, getOwner(), combatOwnerUuid, DamageTypes.ARTISAN_SMASH);
        var school = SpellRegistry.ARTISAN_SMASH.get().getSchoolType();
        var area = new AABB(center, center).inflate(splashRadius);
        var damagedIds = new HashSet<Integer>();

        for (var rawTarget : level().getEntities(this, area, Entity::isAlive)) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!CombatTools.isValidCombatTarget(target, null) || !damagedIds.add(target.getId())) {
                continue;
            }
            if (isBlockedByWall(center, target)) {
                continue;
            }

            CombatTools.applyDamage(target, damage, source, school, CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    private boolean isBlockedByWall(Vec3 center, Entity target) {
        var targetPoint = target instanceof LivingEntity living
                ? living.getEyePosition()
                : target.getBoundingBox().getCenter();
        var hit = level().clip(new ClipContext(center, targetPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private void updateClientAnimationState() {
        clientPrevSpinDegrees = clientSpinDegrees;
        if (clientPhase != getPhase()) {
            clientPhase = getPhase();
            clientPhaseAge = 0;
            clientSpinDirection = getSpinDirection();
        } else {
            ++clientPhaseAge;
        }

        var spinSpeed = clientPhase == PHASE_BURST ? 22.0f : 0.0f;
        clientSpinDegrees = Mth.wrapDegrees(clientSpinDegrees + spinSpeed * clientSpinDirection);
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

    private float getBurstCubeMaxSize() {
        return Math.max(0.45f, entityData.get(DATA_SPLASH_RADIUS) * BURST_CUBE_SIZE_SCALE);
    }

    private static Vec3 normalizeOrFallback(Vec3 vector) {
        if (vector != null && vector.lengthSqr() > 1.0e-6) {
            return vector.normalize();
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

    @Override
    public @Nullable UUID getCombatOwnerUuid() {
        return combatOwnerUuid;
    }

    @Override
    public void setCombatOwnerUuid(@Nullable UUID combatOwnerUuid) {
        this.combatOwnerUuid = combatOwnerUuid;
    }
}
