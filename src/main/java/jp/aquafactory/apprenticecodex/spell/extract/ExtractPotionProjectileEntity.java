package jp.aquafactory.apprenticecodex.spell.extract;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;

public class ExtractPotionProjectileEntity extends ThrownPotion implements AntiMagicSusceptible {
    public ExtractPotionProjectileEntity(EntityType<? extends ExtractPotionProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ExtractPotionProjectileEntity(EntityType<? extends ExtractPotionProjectileEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        discard();
    }
}
