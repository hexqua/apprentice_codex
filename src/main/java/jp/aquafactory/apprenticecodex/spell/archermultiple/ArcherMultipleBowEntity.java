package jp.aquafactory.apprenticecodex.spell.archermultiple;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.PersistentSummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class ArcherMultipleBowEntity extends PersistentSummonWeaponEntity {

    private static final int CHARGE_TICK = 15;
    private static final int COOLDOWN_TICK = 8;
    private static final int DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK = 10;
    private static final int KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET = 60;
    private static final int KEEP_LOCK_ON_TICK_IN_LOST_LOR = 20;
    private static final int KEEP_FIRE_CONTINUE_TICK = 40;

    private static final EntityDataAccessor<Integer> CHARGE_STAGE =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> HIT_SEQUENCE =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> HIT_POSITION_X =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> HIT_POSITION_Y =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> HIT_POSITION_Z =
            SynchedEntityData.defineId(ArcherMultipleBowEntity.class, EntityDataSerializers.FLOAT);

    private Entity priorityTarget;
    private int slot;
    private int maxSlot;
    private float damage;
    private int currentChargeTick;
    private int currentCoolDownTick;
    private int currentLockOnTick;
    private int currentLostSightTick;
    private int keepFireContinueTick;
    private boolean isReadyToFire;
    private Entity autoTarget;
    private int currentHitSequence;
    private UUID lifecycleOwnerUuid;
    private long expirationGameTime = -1L;
    private boolean lifecycleEnding;

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ArcherMultipleBowEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner,int slot, int maxSlot) {
        super(pEntityType, pLevel, owner);
        this.slot = Math.min(slot, maxSlot);
        this.maxSlot = Math.max(maxSlot, 1);
        lifecycleOwnerUuid = owner.getUUID();
        setStandbyPosition(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE_STAGE, 0);
        builder.define(HIT_SEQUENCE, 0);
        builder.define(HIT_POSITION_X, 0.0f);
        builder.define(HIT_POSITION_Y, 0.0f);
        builder.define(HIT_POSITION_Z, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Slot")) {
            slot = tag.getInt("Slot");
        }
        if (tag.contains("MaxSlot")) {
            maxSlot = tag.getInt("MaxSlot");
        }
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
        lifecycleOwnerUuid = tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : lifecycleOwnerUuid;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Slot", slot);
        tag.putInt("MaxSlot", maxSlot);
        tag.putFloat("Damage", damage);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    void setLifecycleOwner(ServerPlayer owner) {
        lifecycleOwnerUuid = owner.getUUID();
    }

    void setExpirationGameTime(long expirationGameTime) {
        this.expirationGameTime = expirationGameTime;
    }

    long getExpirationGameTime() {
        return expirationGameTime;
    }

    boolean hasExpirationGameTime() {
        return expirationGameTime >= 0L;
    }

    ServerPlayer resolvePlayerOwner() {
        if (lifecycleOwnerUuid == null || !(level() instanceof ServerLevel serverLevel)) return null;
        if (getOwner() instanceof ServerPlayer owner && lifecycleOwnerUuid.equals(owner.getUUID())) return owner;
        return serverLevel.getServer().getPlayerList().getPlayer(lifecycleOwnerUuid);
    }

    boolean isOwnedBy(Entity entity) {
        return lifecycleOwnerUuid != null && lifecycleOwnerUuid.equals(entity.getUUID());
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

    public void setPriorityTarget(Entity target) {
        priorityTarget = target;
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createRingParticle(
                position(),
                getLookAngle(),
                0.4f,
                8,
                0.015f,
                0.01,
                ParticleTypes.END_ROD,
                level
        );
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル.
        // todo:再ログインの制御をするかどうか.
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.4f,
                    8,
                    0.015f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        // ヒットスキャン攻撃のパーティクルは同期パラメータで見る.
        if (level.isClientSide){
            var hitSequence = entityData.get(HIT_SEQUENCE);
            if (currentHitSequence != hitSequence) {
                currentHitSequence = hitSequence;
                var hitPosition = new Vec3(entityData.get(HIT_POSITION_X), entityData.get(HIT_POSITION_Y), entityData.get(HIT_POSITION_Z));
                EffectTools.createLineParticle(position(), hitPosition, 0.5, 0.1, 0.1, ParticleTypes.CRIT, level);
            }
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        var validation = ArcherMultipleManager.validate(this);
        if (validation == ArcherMultipleManager.ValidationResult.EXPIRED) {
            ArcherMultipleManager.expire(this);
            return;
        }
        if (validation == ArcherMultipleManager.ValidationResult.INVALID) {
            discardForLifecycle();
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner)) {
            discardForLifecycle();
            return;
        }

        var formationPosition = getFormationPosition(owner, slot, maxSlot);
        followTargetPosition(formationPosition);

        if (priorityTarget != null && (priorityTarget.isRemoved() || !priorityTarget.isAlive())){
            setPriorityTarget(null);
        }

        if (autoTarget != null && (autoTarget.isRemoved() || !autoTarget.isAlive())){
            autoTarget = null;
        }

        if ((priorityTarget == null) && ((currentLockOnTick >= KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET) || (autoTarget == null))) {
            if (tickCount % 10 == 0){
                var newTarget = searchAutoTarget(level);
                if (newTarget != null && newTarget != autoTarget){
                    // 久々にロックを掴んだらディレイを入れる.
                    if (keepFireContinueTick == 0){
                        currentCoolDownTick = DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK;
                    }

                    autoTarget = newTarget;
                    currentLockOnTick = 0;
                    currentLostSightTick = 0;
                    keepFireContinueTick = KEEP_FIRE_CONTINUE_TICK;
                }
            }
        }

        Entity target = null;
        var canWaitIFrame = false;
        if (priorityTarget != null) {
            target = priorityTarget;
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

        var isLockOn = target != null;
        if (isLockOn) {
            var targetPosition = target.position().add(0, target.getBbHeight() / 2, 0);
            var targetFaceVector = targetPosition.subtract(position()).normalize();
            var yawPitch = RotationTools.calculateYawPitchByDirection(targetFaceVector);
            setYRot(yawPitch.yaw());
            setXRot(yawPitch.pitch());
        } else {
            setYRot(owner.getYRot());
            setXRot(0);
        }

        setRot(getYRot(), getXRot());
        hasImpulse = true;

        if (isLockOn) {
            ++currentLockOnTick;
            keepFireContinueTick = KEEP_FIRE_CONTINUE_TICK;
        } else {
            currentLockOnTick = 0;
            if (keepFireContinueTick > 0) {
                --keepFireContinueTick;
            }
        }

        if (currentCoolDownTick > 0) {
            --currentCoolDownTick;
        } else if (isReadyToFire && target != null && target.isAlive()){
            ++currentChargeTick;
            if (currentChargeTick >= CHARGE_TICK) {
                if (!canWaitIFrame || target.invulnerableTime <= 0) {
                    fire(target, level);
                    currentCoolDownTick = COOLDOWN_TICK;
                    isReadyToFire = false;
                    currentChargeTick = 0;
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

    private void fire(Entity target, Level level) {
        var targetPosition = RaycastTools.getEntityTargetPosition(target);
        var source = createCombatDamageSource(DamageTypes.ARCHER_MULTIPLE);
        CombatTools.applyDamage(target, damage, source, SpellRegistry.ARCHER_MULTIPLE.get().getSchoolType(),
                CombatTools.KnockbackTypes.DEFAULT);
        AudioTools.playSoundFromEntity(level, this, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f);

        var sequence = entityData.get(HIT_SEQUENCE);
        entityData.set(HIT_SEQUENCE, sequence + 1);
        entityData.set(HIT_POSITION_X, (float) targetPosition.x);
        entityData.set(HIT_POSITION_Y, (float) targetPosition.y);
        entityData.set(HIT_POSITION_Z, (float) targetPosition.z);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return getFormationPosition(owner, slot, maxSlot);
        }

        return Vec3.ZERO;
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
        return RotationTools.calculateBehindPosition(owner, 0.75 - y / 2, x, y + heightAdjust);
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


    void discardForLifecycle() {
        lifecycleEnding = true;
        discard();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        var notifyOwner = reason.shouldDestroy() && !lifecycleEnding;
        lifecycleEnding = true;
        if (notifyOwner && !level().isClientSide) {
            ArcherMultipleManager.onDestroyed(this);
        }
        if (!isRemoved()) {
            super.remove(reason);
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        // 所有者の長距離teleport後も元chunkで停止せず、次tickで追従位置へ移動させる。
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        // 短時間のrecastと一体で管理し、logoutやserver再起動を跨いで実体を復元しない。
        return false;
    }
}

