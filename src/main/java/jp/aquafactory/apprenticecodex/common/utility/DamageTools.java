package jp.aquafactory.apprenticecodex.common.utility;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DamageTools {
    @SuppressWarnings("UnusedReturnValue")
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource source, SchoolType magicSchool,
                                      boolean ignoreKnockback, boolean penetrateInvulnerable) {
        if (target instanceof LivingEntity livingTarget) {
            var cachedInvulnerableTime = livingTarget.invulnerableTime;
            if (penetrateInvulnerable) {
                livingTarget.invulnerableTime = 0;
            }
            if (ignoreKnockback) {
                KnockbackControl.markIgnoreNextKnockback(livingTarget);
            }

            var hit = livingTarget.hurt(source, baseAmount * getResistAttribute(livingTarget, magicSchool));

            if (penetrateInvulnerable) {
                if (hit) {
                    livingTarget.invulnerableTime = 1;
                } else {
                    livingTarget.invulnerableTime = cachedInvulnerableTime;
                }
            }
            return hit;
        } else {
            return target.hurt(source, baseAmount);
        }
    }

    private static float getResistAttribute(LivingEntity entity, SchoolType damageSchool) {
        var baseResist = entity.getAttributeValue(AttributeRegistry.SPELL_RESIST.get());
        if (damageSchool == null) {
            return 2 - (float) Utils.softCapFormula(baseResist);
        } else {
            return 2 - (float) Utils.softCapFormula(damageSchool.getResistanceFor(entity) * baseResist);
        }
    }
}
