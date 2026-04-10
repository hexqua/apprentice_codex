package jp.aquafactory.apprenticecodex.entity.spelldispenser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserAnchorEntity extends ArmorStand {
    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    public SpellDispenserAnchorEntity(EntityType<? extends SpellDispenserAnchorEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setShowArms(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    public void syncFromCaster(Entity caster) {
        var casterEyeY = caster.getY() + caster.getEyeHeight();
        var feetY = casterEyeY - this.getEyeHeight();
        this.moveTo(caster.getX(), feetY, caster.getZ(), caster.getYRot(), caster.getXRot());
        this.setYRot(caster.getYRot());
        this.setXRot(caster.getXRot());
        this.setYHeadRot(caster.getYRot());
        this.setYBodyRot(caster.getYRot());
        this.xRotO = caster.getXRot();
        this.yRotO = caster.getYRot();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
