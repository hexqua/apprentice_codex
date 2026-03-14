package jp.aquafactory.apprenticecodex.spell.manaslash;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ManaSlashProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(ManaSlashProjectileEntity.class, EntityDataSerializers.FLOAT);

    static final double SPEED = 1.0d;
    static final int EXPIRE_TIME_TICKS = 20;
    static final int FADE_DURATION_TICKS = 10;
    private static final float INITIAL_RADIUS = 0.5f;
    private static final float MAX_RADIUS = 5.0f;
    private static final float RADIUS_GROWTH_PER_TICK = 0.45f;

    public final int animationSeed;
    public AABB oldBB;
    public int animationTime;

    private final Set<Integer> victimIds = new HashSet<>();
    private float damage;

    public ManaSlashProjectileEntity(EntityType<? extends ManaSlashProjectileEntity> entityType, Level level) {
        super(entityType, level);
        animationSeed = level.random.nextInt(9_999);
        oldBB = getBoundingBox();
        setViewScale(8.0f);
        setNoGravity(true);
    }

    public ManaSlashProjectileEntity(EntityType<? extends ManaSlashProjectileEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
    }

    public void shoot(Vec3 rotation) {
        var direction = rotation.lengthSqr() > 1.0e-6 ? rotation.normalize() : new Vec3(0.0d, 0.0d, 1.0d);
        setDeltaMovement(direction.scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, INITIAL_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= EXPIRE_TIME_TICKS) {
            discard();
            return;
        }

        oldBB = getBoundingBox();
        setRadius(getRadius() + RADIUS_GROWTH_PER_TICK);

        if (!level().isClientSide) {
            var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, entity -> false);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                onHitBlock((BlockHitResult) hitResult);
            }

            if (isRemoved()) {
                return;
            }

            for (var rawTarget : level().getEntities(this, getBoundingBox(), this::canHitEntity)) {
                var target = CombatTools.resolutePartEntity(rawTarget);
                if (!CombatTools.isValidCombatTarget(target, getOwner())) {
                    continue;
                }

                if (!victimIds.add(target.getId())) {
                    continue;
                }

                damageEntity(target);
            }
        }

        move(MoverType.SELF, getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(getRadius() * 2.0f, 0.5f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_RADIUS.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void refreshDimensions() {
        var x = getX();
        var y = getY();
        var z = getZ();
        super.refreshDimensions();
        setPos(x, y, z);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0d);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 128.0d;
        return distanceSqr < maxDistance * maxDistance;
    }

    private void setRadius(float radius) {
        if (!level().isClientSide) {
            getEntityData().set(DATA_RADIUS, Mth.clamp(radius, 0.0f, MAX_RADIUS));
        }
    }

    private float getRadius() {
        return getEntityData().get(DATA_RADIUS);
    }

    private void damageEntity(Entity entity) {
        var owner = getOwner();
        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.MANA_SLASH);
        if (!CombatTools.applyDamage(
                entity,
                damage,
                source,
                SpellRegistry.MANA_SLASH.get().getSchoolType(),
                CombatTools.KnockbackTypes.DEFAULT
        )) {
            return;
        }

        if (owner instanceof ServerPlayer serverPlayer && isHoldingCrystalBladedStaff(serverPlayer)) {
            CrystalBladedStaff.spawnManaSiphonOrbsForSpell(serverPlayer, entity.getBoundingBox().getCenter());
        }
    }

    private static boolean isHoldingCrystalBladedStaff(ServerPlayer serverPlayer) {
        return CrystalBladedStaff.isCrystalBladedStaff(serverPlayer.getMainHandItem())
                || CrystalBladedStaff.isCrystalBladedStaff(serverPlayer.getOffhandItem());
    }
}
