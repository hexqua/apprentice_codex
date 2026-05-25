package jp.aquafactory.apprenticecodex.remoteownercast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RemoteOwnerCastAnchorEntity extends ArmorStand {
    @Nullable
    private UUID boundOwnerId;

    public static AttributeSupplier.Builder createAttributes() {
        return RemoteOwnerCastAnchorAttributes.addSyncAttributes(LivingEntity.createLivingAttributes());
    }

    public RemoteOwnerCastAnchorEntity(EntityType<? extends RemoteOwnerCastAnchorEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setShowArms(false);
        this.setCustomNameVisible(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    public void syncFromRemoteGeometry(Vec3 eyePosition, Vec3 forward) {
        var rotation = RemoteOwnerCastGeometry.rotationFromForward(forward);
        var yaw = rotation.yaw();
        var pitch = rotation.pitch();
        var feetY = eyePosition.y - this.getEyeHeight();

        this.moveTo(eyePosition.x, feetY, eyePosition.z, yaw, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.xRotO = pitch;
        this.yRotO = yaw;
    }

    public void bindOwnerName(Entity owner) {
        this.boundOwnerId = owner.getUUID();
        Component name = owner.getDisplayName();
        this.setCustomName(name);
        this.setCustomNameVisible(false);
    }

    public boolean isBoundOwner(Entity entity) {
        return boundOwnerId != null && boundOwnerId.equals(entity.getUUID());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
