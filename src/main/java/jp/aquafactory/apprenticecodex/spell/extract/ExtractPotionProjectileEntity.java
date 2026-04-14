package jp.aquafactory.apprenticecodex.spell.extract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;

public class ExtractPotionProjectileEntity extends ThrownPotion {
    public ExtractPotionProjectileEntity(EntityType<? extends ExtractPotionProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ExtractPotionProjectileEntity(EntityType<? extends ExtractPotionProjectileEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }
}
