package jp.aquafactory.apprenticecodex.spell.fujin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FujinSlashProjectileEntity extends Projectile
        implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    static final double SPEED = 3.0D;
    static final float SLASH_WIDTH = 5.0F;
    static final float HITBOX_HEIGHT = 1.5F;
    // 実効射程は維持したまま、Shockwave風の消失演出だけを射程の途中から先行させる。
    static final float SQUASH_START_DISTANCE_RATIO = 0.75F;
    static final float DISCARD_DISTANCE_RATIO = 1.25F;
    private static final float BLOCK_COLLISION_SIZE = 0.25F;
    private static final double BLOCK_COLLISION_STEP = BLOCK_COLLISION_SIZE * 0.5D;
    private static final EntityDataAccessor<Float> MAX_TRAVEL_DISTANCE =
            SynchedEntityData.defineId(FujinSlashProjectileEntity.class, EntityDataSerializers.FLOAT);

    private final Set<UUID> victimUuids = new HashSet<>();
    private float damage;
    private double traveledDistance;
    private double previousTraveledDistance;
    private UUID combatOwnerUuid;

    public FujinSlashProjectileEntity(EntityType<? extends FujinSlashProjectileEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        setViewScale(8.0F);
        refreshDimensions();
    }

    public FujinSlashProjectileEntity(EntityType<? extends FujinSlashProjectileEntity> entityType,
                                      Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
        setCombatOwnerUuid(CombatOwnerResolver.captureCombatOwnerUuid(owner));
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }

    public void shoot(Vec3 rotation) {
        var direction = rotation.lengthSqr() > 1.0E-6D
                ? rotation.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        setDeltaMovement(direction.scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setMaxTravelDistance(float maxTravelDistance) {
        entityData.set(MAX_TRAVEL_DISTANCE, Math.max(0.0F, maxTravelDistance));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MAX_TRAVEL_DISTANCE, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        var movement = getDeltaMovement();

        var damageMovement = getDamageMovement(movement);
        if (!level().isClientSide && damageMovement.lengthSqr() > 1.0E-8D) {
            var blockHit = findBlockCollision(damageMovement);
            var shouldDiscardOnBlockHit = blockHit != null
                    && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, blockHit);
            var entityDamageMovement = shouldDiscardOnBlockHit
                    ? blockHit.getLocation().subtract(position())
                    : damageMovement;
            damageEntities(entityDamageMovement);
            if (shouldDiscardOnBlockHit) {
                discard();
                return;
            }
        }

        setPos(position().add(movement));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        previousTraveledDistance = traveledDistance;
        traveledDistance += movement.length();

        if (!level().isClientSide && traveledDistance >= getDiscardDistance()) {
            discard();
        }
    }

    private void damageEntities(Vec3 movement) {
        var sweptBounds = getBoundingBox().expandTowards(movement);
        for (var rawTarget : level().getEntities(this, sweptBounds, this::canHitEntity)) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!CombatTools.isValidCombatTarget(target, getOwner())
                    || !victimUuids.add(target.getUUID())) {
                continue;
            }
            damageEntity(target);
        }
    }

    private void damageEntity(Entity entity) {
        var source = CombatOwnerResolver.createDamageSourcePreservingCurrentOwner(
                level(), this, getOwner(), combatOwnerUuid, DamageTypes.FUJIN
        );
        CombatTools.applyDamage(
                entity,
                damage,
                source,
                SpellRegistry.FUJIN.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );
    }

    private Vec3 getDamageMovement(Vec3 movement) {
        var remainingDistance = entityData.get(MAX_TRAVEL_DISTANCE) - traveledDistance;
        var movementLength = movement.length();
        if (remainingDistance <= 0.0D || movementLength <= 1.0E-8D) {
            return Vec3.ZERO;
        }
        return movementLength <= remainingDistance
                ? movement
                : movement.scale(remainingDistance / movementLength);
    }

    private double getDiscardDistance() {
        return entityData.get(MAX_TRAVEL_DISTANCE) * DISCARD_DISTANCE_RATIO;
    }

    public float getSquashProgress(float partialTick) {
        float maxTravelDistance = entityData.get(MAX_TRAVEL_DISTANCE);
        if (maxTravelDistance <= 0.0F) {
            return 1.0F;
        }
        var distance = Mth.lerp(
                Mth.clamp(partialTick, 0.0F, 1.0F),
                previousTraveledDistance,
                traveledDistance
        );
        var startDistance = maxTravelDistance * SQUASH_START_DISTANCE_RATIO;
        var endDistance = maxTravelDistance * DISCARD_DISTANCE_RATIO;
        return (float) Mth.clamp(
                (distance - startDistance) / (endDistance - startDistance),
                0.0D,
                1.0D
        );
    }

    public int getVictimCountForGameTest() {
        return victimUuids.size();
    }

    private BlockHitResult findBlockCollision(Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-8D) {
            return null;
        }

        var stepCount = Math.max(1, Mth.ceil(movement.length() / BLOCK_COLLISION_STEP));
        var step = movement.scale(1.0D / stepCount);
        var previousPos = position();
        for (var i = 1; i <= stepCount; ++i) {
            var currentPos = position().add(step.scale(i));
            if (!level().getBlockCollisions(this, makeBlockCollisionBox(currentPos)).iterator().hasNext()) {
                previousPos = currentPos;
                continue;
            }

            // 大きな斬撃の側面が壁へ触れただけで消えないよう、地形判定だけを小さく保つ。
            var blockHit = level().clip(new ClipContext(
                    previousPos,
                    currentPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                return blockHit;
            }
            return new BlockHitResult(
                    currentPos,
                    Direction.getNearest(-movement.x, -movement.y, -movement.z),
                    BlockPos.containing(currentPos),
                    false
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

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(SLASH_WIDTH, HITBOX_HEIGHT);
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
        setMaxTravelDistance(tag.getFloat("MaxTravelDistance"));
        traveledDistance = tag.getDouble("TraveledDistance");
        previousTraveledDistance = traveledDistance;
        loadCombatOwnerUuid(tag);

        victimUuids.clear();
        var victims = tag.getList("Victims", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (net.minecraft.nbt.Tag victim : victims) {
            victimUuids.add(NbtUtils.loadUUID(victim));
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
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
