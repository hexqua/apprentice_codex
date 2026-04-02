package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DemicreatorWingsWingEntity extends SummonWeaponEntity {
    public DemicreatorWingsWingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public DemicreatorWingsWingEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof Player owner) || !owner.isAlive()) {
            discard();
            return;
        }

        if (owner.level() != level) {
            discard();
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(owner);
        if (spellData == null) {
            discard();
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        if (!state.active || state.wingEntityId != getId()) {
            discard();
            return;
        }

        followTargetPosition(getBackPosition(owner));
        owner.fallDistance = 0.0f;
        setYRot(owner.getYRot());
        setXRot(0.0f);
        setRot(getYRot(), getXRot());
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return getBackPosition(owner);
        }
        return Vec3.ZERO;
    }

    private Vec3 getBackPosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, 0.25, 0, -0.4);
    }
}
