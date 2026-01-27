package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.EffectTools;
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
    private static final int DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK = 10;
    private static final int KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET = 60;
    private static final int KEEP_LOCK_ON_TICK_IN_LOST_LOR = 20;

    private static final EntityDataAccessor<Integer> CHARGE_STAGE =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private UUID priorityTargetUUID;
    private Entity cachedOwner;
    private Entity cachedPriorityTarget;

    private int slot;
    private int maxSlot;
    private float damage;
    private int restBulletCount;
    private int currentChargeTick;
    private int currentCoolDownTick;
    private int currentLockOnTick;
    private int currentLostSightTick;
    private boolean isReadyToFire;
    private Entity autoTarget;

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
        entityData.define(CHARGE_STAGE, 0);
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
            slot = tag.getInt("Slot");
        }
        if (tag.contains("MaxSlot")) {
            maxSlot = tag.getInt("MaxSlot");
        }
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
        if (tag.contains("RestBulletCount")) {
            restBulletCount = tag.getInt("RestBulletCount");
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

        tag.putInt("Slot", slot);
        tag.putInt("MaxSlot", maxSlot);
        tag.putFloat("Damage", damage);
        tag.putInt("RestBulletCount", restBulletCount);
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
        this.slot = slot;
    }

    public void setMaxSlot(int maxSlot) {
        this.maxSlot = maxSlot;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setRestBulletCount(int count) {
        this.restBulletCount = count;
    }

    public int getStage() {
        return entityData.get(CHARGE_STAGE);
    }

    private void setStageByCurrentCharge(){
        if (currentChargeTick <= 0) {
            entityData.set(CHARGE_STAGE, 0);
        }else if (currentChargeTick <= 6) {
            entityData.set(CHARGE_STAGE, 1);
        }else if (currentChargeTick <= 9) {
            entityData.set(CHARGE_STAGE, 2);
        }else{
            entityData.set(CHARGE_STAGE, 3);
        }
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
        var level = level();

        // 射出時パーティクル.
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticleClient(position(), getLookAngle(), level);
        }

        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var formationPosition = getFormationPosition(owner, slot, maxSlot);
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


        if (getPriorityTarget() != null && (getPriorityTarget().isRemoved() || !getPriorityTarget().isAlive())){
            setPriorityTarget(null);
        }

        if (autoTarget != null && (autoTarget.isRemoved() || !autoTarget.isAlive())){
            autoTarget = null;
        }

        if ((getPriorityTarget() == null) && ((currentLockOnTick >= KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET) || (autoTarget == null))) {
            if (tickCount % 10 == 0){
                var newTarget = searchAutoTarget(level);
                if (newTarget != null && newTarget != autoTarget){
                    // 新たにロックオンを掴んだ場合のみ初回ディレイが入る.
                    if (autoTarget != null){
                        currentCoolDownTick = DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK;
                    }
                    autoTarget = newTarget;
                    currentLockOnTick = 0;
                    currentLostSightTick = 0;
                }
            }
        }

        Entity target = null;
        var canWaitIFrame = false;
        if (getPriorityTarget() != null) {
            target = getPriorityTarget();
            canWaitIFrame = true;
        } else if (autoTarget != null) {
            target = autoTarget;
            if (!RaycastTools.hasLineOfSight(level, this, target)){
                ++currentLostSightTick;
                if (currentLostSightTick >= KEEP_LOCK_ON_TICK_IN_LOST_LOR){
                    autoTarget = null;
                    target = null;
                }
            } else {
                currentLostSightTick = 0;
            }
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
        hasImpulse = true;

        if (isLockOn) {
            ++currentLockOnTick;
        } else {
            currentLockOnTick = 0;
        }

        if (currentCoolDownTick > 0) {
            --currentCoolDownTick;
        } else if (isReadyToFire && target != null && target.isAlive()){
            ++currentChargeTick;
            if (currentChargeTick >= CHARGE_TICK) {
                if (!canWaitIFrame || target.invulnerableTime <= 0) {
                    fire(target, level, restBulletCount == 1);
                    currentCoolDownTick = COOLDOWN_TICK;
                    isReadyToFire = false;
                    currentChargeTick = 0;

                    if (restBulletCount > 1) {
                        --restBulletCount;
                    } else {
                        remove(RemovalReason.DISCARDED);
                    }
                }
            }
        } else if (isLockOn && maxSlot > 0) {
            // 1tick増やして連射を最短でできるようにする.
            var interval = CHARGE_TICK + COOLDOWN_TICK + 1;
            if (owner.tickCount % interval == slot * (interval / maxSlot)){
                isReadyToFire = true;
            }
        } else if (tickCount % 2 == 0 && currentChargeTick > 0){
            // ロックオンできてない場合は少しずつ減衰する.
            --currentChargeTick;
        }

        setStageByCurrentCharge();
    }

    private void fire(Entity target, Level level, boolean isLastBullet) {
        var currentPosition = RaycastTools.getEntityTargetPosition(this);
        var targetPosition = RaycastTools.getEntityTargetPosition(target);
        var lineVector = targetPosition.subtract(currentPosition);
        var lineLength = lineVector.length();
        var lineDirection = lineVector.normalize();

        var particleType = isLastBullet ? ParticleTypes.END_ROD : ParticleTypes.CRIT;
        var soundEvent = isLastBullet ? SoundEvents.SHULKER_SHOOT : SoundEvents.ARROW_SHOOT;
        var damage = this.damage * (isLastBullet ? 2.0f : 1.0f);
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

    public void locateCurrentFormationPosition(){
        if ((getOwner() instanceof LivingEntity owner)) {
            var formationPosition = getFormationPosition(owner, slot, maxSlot);
            setPos(formationPosition.x, formationPosition.y, formationPosition.z);
            setYRot(owner.getYRot());
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }
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
        return RaycastTools.findNearestEntityInForwardBox(
                level, this, forward,
                24, 8, 8,
                e -> CombatTools.isValidCombatTarget(e, owner) && CombatTools.canBeHostileToMe(e, owner),
                true
        ).orElse(null);
    }
}
