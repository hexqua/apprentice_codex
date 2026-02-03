package jp.aquafactory.apprenticecodex.common.entity.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class SummonWeaponEntity extends Entity implements TraceableEntity {

    // オーナー系を隠すために意図的にprivate.
    private UUID ownerUUID;
    private Entity cachedOwner;

    public SummonWeaponEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SummonWeaponEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.hasUUID("OwnerUUID")) {
            ownerUUID = pCompound.getUUID("OwnerUUID");
            cachedOwner = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        if (ownerUUID != null) {
            pCompound.putUUID("OwnerUUID", ownerUUID);
        }
    }

    @Override
    public final @Nullable Entity getOwner() {
        @SuppressWarnings("resource") var level = level();
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
        }
    }
}
