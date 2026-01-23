package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DamageTools {
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource damageSource,
                                      boolean ignoreKnockback, boolean penetrateInvulnerable) {
        if (target instanceof LivingEntity livingTarget) {
            var cachedInvulnerableTime = livingTarget.invulnerableTime;
            if (penetrateInvulnerable){
                livingTarget.invulnerableTime = 0;
            }
            if (ignoreKnockback){
                KnockbackControl.markIgnoreNextKnockback(livingTarget);
            }

            var hit = livingTarget.hurt(damageSource, baseAmount);
            if (penetrateInvulnerable) {
                if (hit) {
                    livingTarget.invulnerableTime = 1;
                } else {
                    livingTarget.invulnerableTime = cachedInvulnerableTime;
                }
            }
            return hit;
        } else {
            return target.hurt(damageSource, baseAmount);
        }
    }
}
