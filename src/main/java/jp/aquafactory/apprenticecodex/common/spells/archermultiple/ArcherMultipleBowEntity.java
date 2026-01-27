package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
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
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ArcherMultipleBowEntity extends Entity implements TraceableEntity {

    private static final int CHARGE_TICK = 15;
    private static final int COOLDOWN_TICK = 8;

    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MAX_SLOT =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CURRENT_CHARGE_TICK =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CURRENT_COOLDOWN_TICK =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CURRENT_LOCK_ON_TICK =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_READY_TO_FIRE =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> REST_BULLET_COUNT =
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
        entityData.define(CURRENT_CHARGE_TICK, 0);
        entityData.define(CURRENT_COOLDOWN_TICK, 0);
        entityData.define(CURRENT_LOCK_ON_TICK, 0);
        entityData.define(IS_READY_TO_FIRE, false);
        entityData.define(DAMAGE, 0f);
        entityData.define(REST_BULLET_COUNT, 24);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
            cachedOwner = null;
        }
        if (tag.hasUUID("PriorityTargetUUID")) {
            priorityTargetUUID = tag.getUUID("PriorityTargetUUID");
            cachedPriorityTarget = null;
        }

        if (tag.contains("Slot")) {
            entityData.set(SLOT, tag.getInt("Slot"));
        }
        if (tag.contains("MaxSlot")) {
            entityData.set(MAX_SLOT, tag.getInt("MaxSlot"));
        }
        if (tag.contains("ChargeTick")) {
            entityData.set(CURRENT_CHARGE_TICK, tag.getInt("ChargeTick"));
        }
        if (tag.contains("CooldownTick")) {
            entityData.set(CURRENT_COOLDOWN_TICK, tag.getInt("CooldownTick"));
        }
        if (tag.contains("CurrentLockOnTick")) {
            entityData.set(CURRENT_LOCK_ON_TICK, tag.getInt("CurrentLockOnTick"));
        }
        if (tag.contains("IsReadyToFire")) {
            entityData.set(IS_READY_TO_FIRE, tag.getBoolean("IsReadyToFire"));
        }
        if (tag.contains("Damage")) {
            entityData.set(DAMAGE, tag.getFloat("Damage"));
        }
        if (tag.contains("RestBulletCount")) {
            entityData.set(REST_BULLET_COUNT, tag.getInt("RestBulletCount"));
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
        tag.putInt("ChargeTick", entityData.get(CURRENT_CHARGE_TICK));
        tag.putInt("CooldownTick", entityData.get(CURRENT_COOLDOWN_TICK));
        tag.putInt("CurrentLockOnTick", entityData.get(CURRENT_LOCK_ON_TICK));
        tag.putFloat("Damage", entityData.get(DAMAGE));
        tag.putInt("RestBulletCount", entityData.get(REST_BULLET_COUNT));
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

    private void setChargeTick(int tick) {
        entityData.set(CURRENT_CHARGE_TICK, tick);
    }

    private void setCooldownTick(int tick) {
        entityData.set(CURRENT_COOLDOWN_TICK, tick);
    }

    private void setCurrentLockOnTick(int tick) {
        entityData.set(CURRENT_LOCK_ON_TICK, tick);
    }

    private void setReadyToFire(boolean isReadyToFire) {
        entityData.set(IS_READY_TO_FIRE, isReadyToFire);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
    }

    public void setRestBulletCount(int count) {
        entityData.set(REST_BULLET_COUNT, count);
    }

    private int getSlot() {
        return entityData.get(SLOT);
    }

    private int getMaxSlot() {
        return entityData.get(MAX_SLOT);
    }

    private int getChargeTick() {
        return entityData.get(CURRENT_CHARGE_TICK);
    }

    private int getCooldownTick() {
        return entityData.get(CURRENT_COOLDOWN_TICK);
    }

    private int getCurrentLockOnTick() {
        return entityData.get(CURRENT_LOCK_ON_TICK);
    }

    private boolean isReadyToFire() {
        return entityData.get(IS_READY_TO_FIRE);
    }

    private float getDamage() {
        return entityData.get(DAMAGE);
    }

    private int getRestBulletCount() {
        return entityData.get(REST_BULLET_COUNT);
    }

    public int getStage() {
        var chargeTick = getChargeTick();
        if (chargeTick <= 0) {
            return 0;
        }
        if (chargeTick <= 6) {
            return 1;
        }
        if (chargeTick <= 9) {
            return 2;
        }
        return 3;
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
        cachedPriorityTarget = null;
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
        boolean canWaitIFrame;
        if (getPriorityTarget() != null) {
            target = getPriorityTarget();

            // 手動ターゲット時はIFrameを待てるようにする.
            canWaitIFrame = true;

            // 優先ターゲットがいない場合、ターゲットを外す処理を入れておく.
            // このTickは自動ターゲットが無くても問題ない.
            if (target.isRemoved()) {
                setPriorityTarget(null);
                target = null;
            }
        } else {
            target = searchAutoTarget(level);
            canWaitIFrame = false;
        }

        var isLockOn = target != null && target.isAlive();
        if (isLockOn) {
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

        if (isLockOn) {
            setCurrentLockOnTick(getCurrentLockOnTick() + 1);
        } else {
            setCurrentLockOnTick(0);
        }

        if (getCooldownTick() > 0) {
            setCooldownTick(getCooldownTick() - 1);
        } else if (isReadyToFire() && target != null){
            setChargeTick(getChargeTick() + 1);
            if (getChargeTick() >= CHARGE_TICK) {
                if (!canWaitIFrame || target.invulnerableTime <= 0) {
                    fire(target, level, getRestBulletCount() == 1);
                    setCooldownTick(COOLDOWN_TICK);
                    setChargeTick(0);
                    setReadyToFire(false);

                    if (getRestBulletCount() > 1) {
                        setRestBulletCount(getRestBulletCount() - 1);
                    } else {
                        remove(RemovalReason.DISCARDED);
                    }
                }
            }
        } else if (isLockOn && getMaxSlot() > 0) {
            // 1tick増やして連射を最短でできるようにする.
            var interval = CHARGE_TICK + COOLDOWN_TICK + 1;
            if (owner.tickCount % interval == getSlot() * (interval / getMaxSlot())){
                setReadyToFire(true);
            }
        } else {
            setChargeTick(0);
        }
    }

    private void fire(Entity target, Level level, boolean isLastBullet) {
        var currentPosition = RaycastTools.getEntityTargetPosition(this);
        var targetPosition = RaycastTools.getEntityTargetPosition(target);
        var lineVector = targetPosition.subtract(currentPosition);
        var lineLength = lineVector.length();
        var lineDirection = lineVector.normalize();

        var particleType = isLastBullet ? ParticleTypes.END_ROD : ParticleTypes.CRIT;
        var soundEvent = isLastBullet ? SoundEvents.SHULKER_SHOOT : SoundEvents.ARROW_SHOOT;
        var damage = getDamage() * (isLastBullet ? 2.0f : 1.0f);
        var sourceType = isLastBullet ? "archer_multiple_last" : "archer_multiple";
        var step = isLastBullet ? 0.2 : 0.5;

        if (level instanceof ServerLevel server) {
            for (var offset = 0.0; offset < lineLength; offset += step) {
                var pos = currentPosition.add(lineDirection.scale(offset));
                server.sendParticles(
                        particleType,
                        pos.x + server.random.nextDouble() * 0.01 - 0.005,
                        pos.y + server.random.nextDouble() * 0.01 - 0.005,
                        pos.z + server.random.nextDouble() * 0.01 - 0.005,
                        1,
                        server.random.nextDouble() * 0.1 - 0.05,
                        server.random.nextDouble() * 0.1 - 0.05,
                        server.random.nextDouble() * 0.1 - 0.05,
                        0.01
                );
            }
        }

        var source = DamageSources.getDamageSource(level, this, getOwner(), sourceType);
        CombatTools.applyDamage(target, damage, source, SchoolRegistry.EVOCATION.get(), CombatTools.KnockbackTypes.DEFAULT);
        level.playSound(null, getX(), getY(), getZ(), soundEvent, SoundSource.PLAYERS, 0.5f, 1.0f);
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
