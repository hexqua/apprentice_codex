package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

public class CombatTools {

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
}
