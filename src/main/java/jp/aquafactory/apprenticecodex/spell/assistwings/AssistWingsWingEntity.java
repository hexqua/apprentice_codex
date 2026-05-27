package jp.aquafactory.apprenticecodex.spell.assistwings;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AssistWingsWingEntity extends SummonWeaponEntity implements AntiMagicSusceptible {

    private int KeepTick = 10;

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createRingParticle(
                RotationTools.calculateBehindPosition(this, 0, -0.6, -0.2),
                getLookAngle(),
                0.2,
                6,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );
        EffectTools.createRingParticle(
                RotationTools.calculateBehindPosition(this, 0, +0.6, -0.2),
                getLookAngle(),
                0.2,
                6,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof Player owner)) {
            discard();
            return;
        }

        var locatePosition = getBackPosition(owner);
        followTargetPosition(locatePosition);

        if (KeepTick > 0) {
            --KeepTick;
        }

        if (!owner.isInWaterOrBubble() && !owner.isFallFlying() && !owner.getAbilities().flying &&
                !owner.onClimbable() && !owner.isPassenger() && !owner.isSwimming() && !owner.onGround()) {
            // 低速落下ポーション効果を使う.
            owner.fallDistance = 0;
            if (!owner.isShiftKeyDown()){
                var effect = owner.getEffect(MobEffects.SLOW_FALLING);
                if (effect == null || effect.getDuration() < 10){
                    owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false, false));
                }
            }
        }

        if (owner.onGround() && KeepTick <= 0) {
            Capabilities.withSpellData(owner, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, spell -> {
                spell.localEntityId = -1;
                spell.doneJump = 0;
                this.discard();
            }));
        }

        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return getBackPosition(owner);
        }

        return Vec3.ZERO;
    }

    public Vec3 getBackPosition(LivingEntity owner){
        return RotationTools.calculateBehindPosition(owner, 0.25, 0, -0.4);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        if (getOwner() instanceof Player owner) {
            clearManagedState(owner);
            removeOwnSlowFalling(owner);
        }
        discard();
    }

    private void clearManagedState(Player owner) {
        Capabilities.withSpellData(owner, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, state -> {
            if (state.localEntityId == getId()) {
                state.localEntityId = -1;
            }
        }));
    }

    private static void removeOwnSlowFalling(Player owner) {
        var effect = owner.getEffect(MobEffects.SLOW_FALLING);
        if (effect != null
                && effect.getAmplifier() == 0
                && effect.getDuration() <= 20
                && effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()) {
            owner.removeEffect(MobEffects.SLOW_FALLING);
        }
    }
}
