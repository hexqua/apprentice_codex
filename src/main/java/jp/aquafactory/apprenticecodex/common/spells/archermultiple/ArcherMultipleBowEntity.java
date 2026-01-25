package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ArcherMultipleBowEntity extends Entity implements TraceableEntity {

    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MAX_SLOT =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private UUID priorityTargetUUID;
    private Entity cachedOwner;
    private Entity cachedPriorityTarget;

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SLOT, 0);
        entityData.define(MAX_SLOT, 1);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
            cachedOwner = null;
        }
        if (tag.contains("Slot")) {
            entityData.set(SLOT, tag.getInt("Slot"));
        }
        if (tag.contains("MaxSlot")) {
            entityData.set(MAX_SLOT, tag.getInt("MaxSlot"));
        }
        if (tag.hasUUID("PriorityTargetUUID")) {
            priorityTargetUUID = tag.getUUID("PriorityTargetUUID");
        }

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        if (priorityTargetUUID != null) {
            tag.putUUID("PriorityTargetUUID", priorityTargetUUID);
        }

        tag.putInt("Slot", entityData.get(SLOT));
        tag.putInt("MaxSlot", entityData.get(MAX_SLOT));
    }

    @Override
    public @Nullable Entity getOwner() {
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

    public void setSlot(int slot) {
        entityData.set(SLOT, slot);
    }

    public void setMaxSlot(int maxSlot) {
        entityData.set(MAX_SLOT, maxSlot);
    }

    private int getSlot() {
        return entityData.get(SLOT);
    }

    private int getMaxSlot() {
        return entityData.get(MAX_SLOT);
    }

    public void setOwner(Entity pOwner) {
        if (pOwner != null) {
            ownerUUID = pOwner.getUUID();
            cachedOwner = pOwner;
        }
    }

    private Entity getPriorityTarget() {
        @SuppressWarnings("resource") var level = level();
        if (cachedPriorityTarget != null && !cachedPriorityTarget.isRemoved()) {
            return cachedPriorityTarget;
        }
        if (priorityTargetUUID != null && level instanceof ServerLevel server) {
            cachedPriorityTarget = server.getEntity(priorityTargetUUID);
            return cachedPriorityTarget;
        }

        return null;
    }

    public void setPriorityTarget(UUID pTarget) {
        priorityTargetUUID = pTarget;
    }

    @Override
    public void tick() {
        super.tick();

        var level = level();
        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var formationPosition = getFormationPosition(owner, getSlot(), getMaxSlot());
        var formationTargetVec = formationPosition.subtract(position());
        var distance = formationTargetVec.length();
        var step = formationTargetVec.normalize().scale(Math.min(0.5, distance));

        if (distance < 0.01) {
            setDeltaMovement(Vec3.ZERO);
            setPos(formationPosition.x, formationPosition.y, formationPosition.z);
        } else {
            setDeltaMovement(step);
            move(net.minecraft.world.entity.MoverType.SELF, step);
        }

        Entity target;
        if (getPriorityTarget() != null) {
            target = getPriorityTarget();

            // 優先ターゲットがいない場合、ターゲットを外す処理を入れておく.
            // このTickは自動ターゲットが無くても問題ない.
            if (target.isRemoved()) {
                setPriorityTarget(null);
                target = null;
            }
        } else {
            target = searchAutoTarget(level);
        }

        if (target != null && target.isAlive()) {
            var targetPosition = target.position().add(0, target.getBbHeight() / 2, 0);
            var targetFaceVector = targetPosition.subtract(position()).normalize();
            var yaw = (float) (Mth.atan2(-targetFaceVector.x, targetFaceVector.z) * Mth.RAD_TO_DEG);
            var xzLen = Math.sqrt(targetFaceVector.x * targetFaceVector.x + targetFaceVector.z * targetFaceVector.z);
            var pitch = (float) (Mth.atan2(-targetFaceVector.y, xzLen) * Mth.RAD_TO_DEG);

            setYRot(yaw);
            setXRot(pitch);
        } else {
            setYRot(owner.getYRot());
            setXRot(0);
        }

        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private static Vec3 getFormationPosition(LivingEntity owner, int index, int maxIndex) {
        if (maxIndex <= 1) {
            return owner.position();
        }

        // 半円で構築するようにする.
        var angle = (1 * Math.PI * index) / (maxIndex - 1);
        var radius = 1.0;

        var x = radius * Math.cos(angle);
        var y = radius * Math.sin(angle);
        var heightAdjust = -0.45;

        // 微妙に前傾配置になるようにする.
        return computeBehindPos(owner, 0.75 - y / 2, x, y + heightAdjust);
    }

    private static Vec3 computeBehindPos(LivingEntity owner, double backOffSet, double xOffset, double yOffset) {
        var yawAngle = owner.getYRot() * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);

        var back = new Vec3(-forwardX, 0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0, -back.x).normalize();

        var behindOffset = back.scale(backOffSet).add(new Vec3(0, yOffset, 0)).add(right.scale(xOffset));
        return owner.getEyePosition().add(behindOffset);
    }

    private Entity searchAutoTarget(Level level) {
        if(!(getOwner() instanceof LivingEntity owner)) {
            return null;
        }

        // 視線の上下は除外する.
        var yaw = owner.getYRot();
        var forward = Vec3.directionFromRotation(0.0f, yaw).normalize();

        // まず背後の対象を強めに取る.
        var behindTarget = RaycastTools.findNearestEntityInForwardBox(
                level, this, forward.scale(-1.0),
                3, 4, 4,
                e -> CombatTools.isValidCombatTarget(e, owner),
                true
        ).orElse(null);
        if (behindTarget != null) {
            return behindTarget;
        }

        // 背後がいなければ正面を見る.
        return RaycastTools.findNearestEntityInForwardBox(
                level, this, forward,
                24, 8, 8,
                e -> CombatTools.isValidCombatTarget(e, owner),
                true
        ).orElse(null);
    }
}
