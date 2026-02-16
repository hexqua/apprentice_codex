package jp.aquafactory.apprenticecodex.spell.assistwings;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AssistWingsWingEntity extends SummonWeaponEntity {

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
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        @SuppressWarnings("resource") var level = level();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var locatePosition = getBackPosition(owner);
        followTargetPosition(locatePosition);

        if (KeepTick > 0) {
            --KeepTick;
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
        return RotationTools.calculateBehindPosition(owner, 0.2, 0, -0.4);
    }
}
