package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class ServantGazeProjectileEntity extends MagicMissileProjectile {
    public ServantGazeProjectileEntity(EntityType<? extends MagicMissileProjectile> type, Level level) {
        super(type, level);
    }

    public ServantGazeProjectileEntity(EntityType<? extends MagicMissileProjectile> type, Level level,
                                       LivingEntity owner, float damage) {
        super(type, level, owner);
        setDamage(damage);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        var resolved = CombatTools.resolutePartEntity(target);
        return resolved != getOwner() && CombatTools.isValidCombatTarget(resolved, getOwner())
                && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        var target = CombatTools.resolutePartEntity(hit.getEntity());
        if (CombatTools.isValidCombatTarget(target, getOwner())) {
            var source = CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.SERVANT_GAZE);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.SERVANT_GAZE.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }
        pierceOrDiscard();
    }
}
