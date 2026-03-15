package jp.aquafactory.apprenticecodex.spell;

import net.minecraft.world.entity.LivingEntity;

public interface IClientBlockTargetingSpell {
    double getClientBlockTargetingRange(int spellLevel, LivingEntity entity);
}
