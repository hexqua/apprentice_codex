package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.EffectTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

public class CommenceFireGunEntity extends Entity implements TraceableEntity {

    private UUID ownerUUID;
    private Entity cachedOwner;
    private Vec3 aimPosition;
    private float damage;
    private int castingTick;

    public CommenceFireGunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public CommenceFireGunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
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
        damage = tag.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        tag.putFloat("Damage", damage);
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

    public void setOwner(Entity pOwner) {
        if (pOwner != null) {
            ownerUUID = pOwner.getUUID();
            cachedOwner = pOwner;
        }
    }

    @Override
    public void tick() {
        super.tick();

        @SuppressWarnings("resource") var level = level();
        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var locatePosition = getAimingPosition(owner);
        var targetVec = locatePosition.subtract(position());
        var distance = targetVec.length();
        var step = targetVec.normalize().scale(Math.min(0.5, distance));

        if (distance < 0.001 || distance > 0.5) {
            setDeltaMovement(Vec3.ZERO);
            setPos(locatePosition.x, locatePosition.y, locatePosition.z);
        } else {
            setDeltaMovement(step);
            move(net.minecraft.world.entity.MoverType.SELF, step);
        }

        if (aimPosition != null) {
            var targetFaceVector = aimPosition.subtract(position()).normalize();
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

    public void setDamage(float newDamage){
        damage = newDamage;
    }

    public void setCastingTick(int tick){
        // todo:詠唱中演出.
        ApprenticeCodex.LOGGER.debug("setCastingTick: {}", tick);
        castingTick = tick;
    }


    public void setLookTarget(Vec3 target){
        aimPosition = target;
    }

    public void fire(Entity target, Level level){
        var resoluteTarget = CombatTools.resolutePartEntity(target);
        var source = DamageSources.getDamageSource(level, getOwner(), "commence_fire");
        CombatTools.applyDamage(resoluteTarget, damage, source, SchoolRegistry.LIGHTNING.get(), CombatTools.KnockbackTypes.DEFAULT);

        // todo:重くならないようにクライアントフェーズにエフェクトを送れるようにする(今は仮でサーバー処理)
        var targetVec = target.position().subtract(position());
        var length = targetVec.length();
        var normal = targetVec.normalize();
        EffectTools.createLineParticleServer(position(), normal, length, 0.3, ParticleRegistry.TRACER_DOT.get(), level);

        // todo:音は恐らく外部素材を使う.
        AudioTools.playSoundFromEntity(level, this, SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f, 0.1f);
        aimPosition = null;
    }

    public void fireOnlyEffect(Vec3 target, Level level){
        // todo:重くならないようにクライアントフェーズにエフェクトを送れるようにする(今は仮でサーバー処理)
        var targetVec = target.subtract(position());
        var length = targetVec.length();
        var normal = targetVec.normalize();
        EffectTools.createLineParticleServer(position(), normal, length, 0.3, ParticleRegistry.TRACER_DOT.get(), level);

        // todo:音は恐らく外部素材を使う.
        AudioTools.playSoundFromEntity(level, this, SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f, 0.1f);
        aimPosition = null;
    }

    public void locateAimingPosition(){
        if ((getOwner() instanceof LivingEntity owner)) {
            var formationPosition = getAimingPosition(owner);
            setPos(formationPosition.x, formationPosition.y, formationPosition.z);
            setYRot(owner.getYRot());
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        var yawAngle = owner.getYRot() * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);

        var back = new Vec3(-forwardX, 0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0, -back.x).normalize();

        var behindOffset = back.scale(-0.7).add(new Vec3(0, 0.3, 0)).add(right.scale(-0.7));
        return owner.getEyePosition().add(behindOffset);
    }
}
