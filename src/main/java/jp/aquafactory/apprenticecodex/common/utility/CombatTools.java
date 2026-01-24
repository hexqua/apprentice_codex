package jp.aquafactory.apprenticecodex.common.utility;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

public class CombatTools {

    public enum KnockbackTypes {
        DEFAULT,
        NO_KNOCKBACK,
    }

    private CombatTools() {
        // do nothing.
    }

    public static Entity resolutePartEntity(Entity raw) {
        // パーツ系モブの解決.
        if (raw instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return raw;
    }

    public static boolean isValidCombatTarget(Entity target, @Nullable Entity owner) {
        if (target == owner) return false;

        // 例外的に対象にする特殊エンティティを指定.
        if (target instanceof EndCrystal) return true;

        // 基本的にはLivingEntityのみを対象.
        return target instanceof LivingEntity;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource source, SchoolType magicSchool,
                                      KnockbackTypes type) {
        if (target instanceof LivingEntity livingTarget) {
            if (type == KnockbackTypes.NO_KNOCKBACK) {
                KnockbackControl.markIgnoreNextKnockback(livingTarget);
            }
            return livingTarget.hurt(source, baseAmount * getResistAttribute(livingTarget, magicSchool));
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
