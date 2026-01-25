package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

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

public class ArcherMultipleBowEntity extends Entity implements TraceableEntity {

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
            cachedOwner = null;
        }

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public @Nullable Entity getOwner() {
        @SuppressWarnings("resource") var level = level();

        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        } else if (ownerUUID != null && level instanceof ServerLevel server) {
            cachedOwner = server.getEntity(ownerUUID);
            return cachedOwner;
        } else {
            return null;
        }
    }

    public void setOwner(@Nullable Entity pOwner) {
        if (pOwner != null) {
            ownerUUID = pOwner.getUUID();
            cachedOwner = pOwner;
        }

    }
}
