package jp.aquafactory.apprenticecodex.spell.combustionjet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CombustionJetWaveEntity extends Projectile
        implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    static final double SPEED = 2.25D;
    static final float WAVE_WIDTH = 9.0F;
    static final float WAVE_HEIGHT = 1.0F;
    static final float WAVE_DEPTH = 1.5F;
    private static final float KNOCKBACK_STRENGTH = 1.0F;
    private static final float BLOCK_COLLISION_SIZE = 0.5F;
    private static final double BLOCK_COLLISION_STEP = BLOCK_COLLISION_SIZE * 0.5D;
    private static final double COLLISION_EPSILON = 1.0E-7D;
    private static final EntityDataAccessor<Float> MAX_TRAVEL_DISTANCE =
            SynchedEntityData.defineId(CombustionJetWaveEntity.class, EntityDataSerializers.FLOAT);

    private final Set<UUID> victimUuids = new HashSet<>();
    private float damage;
    private int burnDuration;
    private double traveledDistance;
    private UUID combatOwnerUuid;

    public CombustionJetWaveEntity(EntityType<? extends CombustionJetWaveEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setViewScale(8.0F);
    }

    public CombustionJetWaveEntity(EntityType<? extends CombustionJetWaveEntity> entityType,
                                   Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
        setCombatOwnerUuid(CombatOwnerResolver.captureCombatOwnerUuid(owner));
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }

    public void shoot(Vec3 rotation) {
        var direction = rotation.lengthSqr() > COLLISION_EPSILON
                ? rotation.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        setDeltaMovement(direction.scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0F, damage);
    }

    public void setBurnDuration(int burnDuration) {
        this.burnDuration = Math.max(0, burnDuration);
    }

    public void setMaxTravelDistance(float maxTravelDistance) {
        entityData.set(MAX_TRAVEL_DISTANCE, Math.max(0.0F, maxTravelDistance));
    }

    void moveToCollisionLimitedSpawnPosition(Vec3 movement) {
        var collision = scanBlockCollision(movement);
        setPos(collision == null ? position().add(movement) : collision.previousPosition());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MAX_TRAVEL_DISTANCE, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        var movement = getDeltaMovement();
        var activeMovement = truncateToRemainingRange(movement);
        if (activeMovement.lengthSqr() <= COLLISION_EPSILON) {
            discard();
            return;
        }

        if (!level().isClientSide) {
            var blockHit = findBlockCollision(activeMovement);
            var shouldDiscardOnBlockHit = blockHit != null
                    && !ForgeEventFactory.onProjectileImpact(this, blockHit);
            var entityMovement = shouldDiscardOnBlockHit
                    ? blockHit.getLocation().subtract(position())
                    : activeMovement;
            affectEntities(entityMovement);
            if (shouldDiscardOnBlockHit) {
                discard();
                return;
            }
        }

        moveAndSpawnTrail(activeMovement);
        traveledDistance += activeMovement.length();
        if (traveledDistance + COLLISION_EPSILON >= entityData.get(MAX_TRAVEL_DISTANCE)) {
            discard();
        }
    }

    private Vec3 truncateToRemainingRange(Vec3 movement) {
        var remainingDistance = entityData.get(MAX_TRAVEL_DISTANCE) - traveledDistance;
        var movementLength = movement.length();
        if (remainingDistance <= 0.0D || movementLength <= COLLISION_EPSILON) {
            return Vec3.ZERO;
        }
        return movementLength <= remainingDistance
                ? movement
                : movement.scale(remainingDistance / movementLength);
    }

    private void moveAndSpawnTrail(Vec3 movement) {
        if (level().isClientSide) {
            spawnTrailParticles(movement);
        }
        setPos(position().add(movement));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    private void affectEntities(Vec3 movement) {
        if (movement.lengthSqr() <= COLLISION_EPSILON) {
            return;
        }

        var basis = WaveBasis.from(movement, getYRot());
        var volume = WaveVolume.forMovement(position(), movement, basis);
        var checkedTargetUuids = new HashSet<UUID>();
        for (var rawTarget : level().getEntities(this, volume.bounds(), this::canHitEntity)) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            var targetUuid = target.getUUID();
            if (!CombatTools.isValidCombatTarget(target, getOwner())
                    || victimUuids.contains(targetUuid)
                    || !checkedTargetUuids.add(targetUuid)
                    || !volume.intersects(target.getBoundingBox())
                    || !hasClearPathToTarget(target, movement)) {
                continue;
            }

            victimUuids.add(targetUuid);
            affectEntity(target, basis.forward());
        }
    }

    private void affectEntity(Entity target, Vec3 forward) {
        var source = CombatOwnerResolver.createDamageSourcePreservingCurrentOwner(
                level(), this, getOwner(), combatOwnerUuid, DamageTypes.COMBUSTION_JET
        );
        CombatTools.applyDamage(
                target,
                damage,
                source,
                SpellRegistry.COMBUSTION_JET.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        var horizontalDirection = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalDirection.lengthSqr() > COLLISION_EPSILON) {
            horizontalDirection = horizontalDirection.normalize();
            livingTarget.knockback(KNOCKBACK_STRENGTH, -horizontalDirection.x, -horizontalDirection.z);
        }
        if (livingTarget.getRemainingFireTicks() < burnDuration) {
            livingTarget.setRemainingFireTicks(burnDuration);
        }
    }

    private boolean hasClearPathToTarget(Entity target, Vec3 movement) {
        var movementLengthSqr = movement.lengthSqr();
        if (movementLengthSqr <= COLLISION_EPSILON) {
            return false;
        }

        var movementStart = position();
        var targetBox = target.getBoundingBox();
        var movementProgress = Mth.clamp(
                targetBox.getCenter().subtract(movementStart).dot(movement) / movementLengthSqr,
                0.0D,
                1.0D
        );
        var sourcePoint = movementStart.add(movement.scale(movementProgress));
        var targetPoint = new Vec3(
                Mth.clamp(sourcePoint.x, targetBox.minX, targetBox.maxX),
                Mth.clamp(sourcePoint.y, targetBox.minY, targetBox.maxY),
                Mth.clamp(sourcePoint.z, targetBox.minZ, targetBox.maxZ)
        );
        if (sourcePoint.distanceToSqr(targetPoint) <= COLLISION_EPSILON) {
            return true;
        }

        // 波面の側面ではなく、中央の進行経路から対象までを遮る地形だけを命中遮断として扱う。
        var blockHit = level().clip(new ClipContext(
                sourcePoint,
                targetPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return blockHit.getType() == HitResult.Type.MISS;
    }

    private BlockHitResult findBlockCollision(Vec3 movement) {
        var collision = scanBlockCollision(movement);
        return collision == null ? null : collision.hitResult();
    }

    private BlockCollision scanBlockCollision(Vec3 movement) {
        var stepCount = Math.max(1, Mth.ceil(movement.length() / BLOCK_COLLISION_STEP));
        var step = movement.scale(1.0D / stepCount);
        var previousPos = position();
        for (var i = 1; i <= stepCount; ++i) {
            var currentPos = position().add(step.scale(i));
            if (!level().getBlockCollisions(this, makeBlockCollisionBox(currentPos)).iterator().hasNext()) {
                previousPos = currentPos;
                continue;
            }

            // 幅広い熱波を地形に消されづらくするため、地形との衝突だけは中央0.5ブロックに限定する。
            var blockHit = level().clip(new ClipContext(
                    previousPos,
                    currentPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                return new BlockCollision(blockHit, previousPos);
            }
            return new BlockCollision(
                    new BlockHitResult(
                            currentPos,
                            Direction.getNearest(-movement.x, -movement.y, -movement.z),
                            BlockPos.containing(currentPos),
                            false
                    ),
                    previousPos
            );
        }
        return null;
    }

    private static AABB makeBlockCollisionBox(Vec3 position) {
        var halfSize = BLOCK_COLLISION_SIZE * 0.5D;
        return new AABB(
                position.x - halfSize, position.y - halfSize, position.z - halfSize,
                position.x + halfSize, position.y + halfSize, position.z + halfSize
        );
    }

    private void spawnTrailParticles(Vec3 movement) {
        if (movement.lengthSqr() <= COLLISION_EPSILON) {
            return;
        }

        var basis = WaveBasis.from(movement, getYRot());
        var count = Mth.clamp((int) (movement.lengthSqr() * 4.0D), 1, 4);
        var random = level().getRandom();
        for (var lane = -3; lane <= 3; ++lane) {
            for (var i = 0; i < count; ++i) {
                var progress = (i + random.nextDouble()) / count;
                var lateralOffset = lane + (random.nextDouble() - 0.5D) * 0.8D;
                var verticalOffset = (random.nextDouble() - 0.5D) * WAVE_HEIGHT;
                var particlePos = position()
                        .add(movement.scale(progress))
                        .add(basis.lateral().scale(lateralOffset))
                        .add(basis.up().scale(verticalOffset));
                var randomVelocity = Utils.getRandomVec3(1)
                        .add(basis.forward())
                        .scale(0.25D);
                level().addParticle(
                        ParticleHelper.FIRE,
                        true,
                        particlePos.x - randomVelocity.x,
                        particlePos.y - randomVelocity.y,
                        particlePos.z - randomVelocity.z,
                        randomVelocity.x * 0.5D,
                        randomVelocity.y * 0.5D,
                        randomVelocity.z * 0.5D
                );
            }
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (!level().isClientSide && !isRemoved()) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("BurnDuration", burnDuration);
        tag.putFloat("MaxTravelDistance", entityData.get(MAX_TRAVEL_DISTANCE));
        tag.putDouble("TraveledDistance", traveledDistance);
        saveCombatOwnerUuid(tag);

        var victims = new ListTag();
        for (var victimUuid : victimUuids) {
            victims.add(NbtUtils.createUUID(victimUuid));
        }
        tag.put("Victims", victims);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        burnDuration = tag.getInt("BurnDuration");
        setMaxTravelDistance(tag.getFloat("MaxTravelDistance"));
        traveledDistance = tag.getDouble("TraveledDistance");
        loadCombatOwnerUuid(tag);

        victimUuids.clear();
        var victims = tag.getList("Victims", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (net.minecraft.nbt.Tag victim : victims) {
            victimUuids.add(NbtUtils.loadUUID(victim));
        }
    }

    public int getVictimCountForGameTest() {
        return victimUuids.size();
    }

    public float getDamageForGameTest() {
        return damage;
    }

    public int getBurnDurationForGameTest() {
        return burnDuration;
    }

    public float getMaxTravelDistanceForGameTest() {
        return entityData.get(MAX_TRAVEL_DISTANCE);
    }

    @Override
    public @Nullable UUID getCombatOwnerUuid() {
        return combatOwnerUuid;
    }

    @Override
    public void setCombatOwnerUuid(@Nullable UUID combatOwnerUuid) {
        this.combatOwnerUuid = combatOwnerUuid;
    }

    private record BlockCollision(BlockHitResult hitResult, Vec3 previousPosition) {
    }

    private record WaveBasis(Vec3 forward, Vec3 lateral, Vec3 up) {
        private static WaveBasis from(Vec3 movement, float yaw) {
            var forward = movement.normalize();
            var lateral = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (lateral.lengthSqr() <= COLLISION_EPSILON) {
                var yawRadians = yaw * Mth.DEG_TO_RAD;
                lateral = new Vec3(-Mth.cos(yawRadians), 0.0D, -Mth.sin(yawRadians));
            }
            lateral = lateral.normalize();
            var up = lateral.cross(forward).normalize();
            return new WaveBasis(forward, lateral, up);
        }
    }

    private record WaveVolume(Vec3 center, WaveBasis basis, Vec3 halfExtents, AABB bounds) {
        private static WaveVolume forMovement(Vec3 start, Vec3 movement, WaveBasis basis) {
            var halfExtents = new Vec3(
                    WAVE_DEPTH * 0.5D + movement.length() * 0.5D,
                    WAVE_WIDTH * 0.5D,
                    WAVE_HEIGHT * 0.5D
            );
            var center = start.add(movement.scale(0.5D));
            var worldHalfX = projectedRadius(new Vec3(1.0D, 0.0D, 0.0D), basis, halfExtents);
            var worldHalfY = projectedRadius(new Vec3(0.0D, 1.0D, 0.0D), basis, halfExtents);
            var worldHalfZ = projectedRadius(new Vec3(0.0D, 0.0D, 1.0D), basis, halfExtents);
            var bounds = new AABB(
                    center.x - worldHalfX, center.y - worldHalfY, center.z - worldHalfZ,
                    center.x + worldHalfX, center.y + worldHalfY, center.z + worldHalfZ
            );
            return new WaveVolume(center, basis, halfExtents, bounds);
        }

        private boolean intersects(AABB target) {
            var targetCenter = target.getCenter();
            var targetHalfExtents = new Vec3(
                    target.getXsize() * 0.5D,
                    target.getYsize() * 0.5D,
                    target.getZsize() * 0.5D
            );
            var difference = targetCenter.subtract(center);
            return overlapsOnAxis(difference, targetHalfExtents, basis.forward(), halfExtents.x)
                    && overlapsOnAxis(difference, targetHalfExtents, basis.lateral(), halfExtents.y)
                    && overlapsOnAxis(difference, targetHalfExtents, basis.up(), halfExtents.z);
        }

        private static boolean overlapsOnAxis(Vec3 difference, Vec3 targetHalfExtents,
                                              Vec3 axis, double waveHalfExtent) {
            var targetRadius = Math.abs(axis.x) * targetHalfExtents.x
                    + Math.abs(axis.y) * targetHalfExtents.y
                    + Math.abs(axis.z) * targetHalfExtents.z;
            return Math.abs(difference.dot(axis)) <= waveHalfExtent + targetRadius;
        }

        private static double projectedRadius(Vec3 axis, WaveBasis basis, Vec3 halfExtents) {
            return Math.abs(axis.dot(basis.forward())) * halfExtents.x
                    + Math.abs(axis.dot(basis.lateral())) * halfExtents.y
                    + Math.abs(axis.dot(basis.up())) * halfExtents.z;
        }
    }
}
