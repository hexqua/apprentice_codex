package jp.aquafactory.apprenticecodex.entity;

import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class SummonWeaponEntity extends Entity implements TraceableEntity, CombatOwnerUuidHolder {

    private static final double FOLLOW_MAX_DISTANCE = 0.5;

    // オーナー系を隠すために意図的にprivate.
    private UUID ownerUUID;
    private Entity cachedOwner;
    private UUID combatOwnerUuid;

    public SummonWeaponEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SummonWeaponEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
        setStandbyPosition(owner);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // SummonWeaponEntity はログアウトをまたぐ所有者復元を許可しない.
        ownerUUID = null;
        cachedOwner = null;
        combatOwnerUuid = null;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // SummonWeaponEntity はオーナー情報を永続化しない.
    }

    @Override
    public void tick(){
        var level = level();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (level instanceof ServerLevel server) {
            tickOnServer(server);
        }
    }

    abstract public void tickOnServer(ServerLevel level);

    @Override
    public final @Nullable Entity getOwner() {
        var level = level();
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        if (ownerUUID != null && level instanceof ServerLevel server) {
            cachedOwner = server.getEntity(ownerUUID);
            return cachedOwner;
        }

        return null;
    }

    public final void setOwner(Entity pOwner) {
        if (pOwner != null) {
            ownerUUID = pOwner.getUUID();
            cachedOwner = pOwner;
            combatOwnerUuid = CombatOwnerResolver.captureCombatOwnerUuid(pOwner);
        }
    }

    protected final DamageSource createCombatDamageSource(ResourceKey<DamageType> damageType) {
        return createCombatDamageSource(this, damageType);
    }

    protected final DamageSource createCombatDamageSource(Entity directEntity, ResourceKey<DamageType> damageType) {
        return CombatOwnerResolver.createDamageSourcePreservingCurrentOwner(
                level(),
                directEntity,
                getOwner(),
                combatOwnerUuid,
                damageType
        );
    }

    protected final DamageSource createOwnerDirectCombatDamageSource(ResourceKey<DamageType> damageType) {
        var owner = getOwner();
        return createCombatDamageSource(owner != null ? owner : this, damageType);
    }

    public final void followTargetPosition(Vec3 targetPos){
        var targetVec = targetPos.subtract(position());
        var distance = targetVec.length();
        var step = targetVec.normalize().scale(Math.min(FOLLOW_MAX_DISTANCE, distance));

        if (distance < 0.001 || distance > FOLLOW_MAX_DISTANCE) {
            setDeltaMovement(Vec3.ZERO);
            setPos(targetPos.x, targetPos.y, targetPos.z);
        } else {
            setDeltaMovement(step);
            move(net.minecraft.world.entity.MoverType.SELF, step);
        }
    }

    protected final void setStandbyPosition(LivingEntity owner){
        var formationPosition = getStandbyPosition();
        setPos(formationPosition.x, formationPosition.y, formationPosition.z);
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    public abstract Vec3 getStandbyPosition();

    public void releaseWeapon(){
        discard();
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
