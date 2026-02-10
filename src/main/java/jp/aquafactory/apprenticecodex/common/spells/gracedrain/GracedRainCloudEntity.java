package jp.aquafactory.apprenticecodex.common.spells.gracedrain;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GracedRainCloudEntity extends SummonWeaponEntity {

    public static final double HEIGHT_OFFSET = 3.0;

    private @Nullable UUID followTargetUuid;
    private @Nullable Entity cachedFollowTarget;
    private @Nullable Vec3 anchorPosition;

    public GracedRainCloudEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public GracedRainCloudEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    public void setFollowTarget(Entity target) {
        followTargetUuid = target.getUUID();
        cachedFollowTarget = target;
        anchorPosition = toCloudPosition(RaycastTools.getEntityTargetPosition(target));
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    public void setAnchorPosition(Vec3 anchorPosition) {
        this.anchorPosition = anchorPosition;
        followTargetUuid = null;
        cachedFollowTarget = null;
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity)) {
            discard();
            return;
        }

        var targetPos = resolveTargetPosition();
        if (targetPos != null) {
            followTargetPosition(targetPos);
        }
    }

    @Nullable
    private Vec3 resolveTargetPosition() {
        var target = getFollowTarget();
        if (target != null && !target.isRemoved()) {
            var pos = toCloudPosition(RaycastTools.getEntityTargetPosition(target));
            anchorPosition = pos;
            return pos;
        }

        return anchorPosition;
    }

    @Nullable
    private Entity getFollowTarget() {
        if (cachedFollowTarget != null && !cachedFollowTarget.isRemoved()) {
            return cachedFollowTarget;
        }

        if (followTargetUuid != null && level() instanceof ServerLevel server) {
            cachedFollowTarget = server.getEntity(followTargetUuid);
            return cachedFollowTarget;
        }

        return null;
    }

    public static Vec3 toCloudPosition(Vec3 basePosition) {
        return basePosition.add(0.0, HEIGHT_OFFSET, 0.0);
    }

    @Override
    public Vec3 getStandbyPosition() {
        return anchorPosition != null ? anchorPosition : position();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("FollowTarget")) {
            followTargetUuid = tag.getUUID("FollowTarget");
            cachedFollowTarget = null;
        }

        if (tag.contains("AnchorX")) {
            var x = tag.getDouble("AnchorX");
            var y = tag.getDouble("AnchorY");
            var z = tag.getDouble("AnchorZ");
            anchorPosition = new Vec3(x, y, z);
            setPos(x, y, z);
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (followTargetUuid != null) {
            tag.putUUID("FollowTarget", followTargetUuid);
        }

        if (anchorPosition != null) {
            tag.putDouble("AnchorX", anchorPosition.x);
            tag.putDouble("AnchorY", anchorPosition.y);
            tag.putDouble("AnchorZ", anchorPosition.z);
        }
    }
}
