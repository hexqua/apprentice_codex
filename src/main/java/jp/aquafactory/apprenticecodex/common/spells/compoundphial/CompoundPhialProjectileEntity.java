package jp.aquafactory.apprenticecodex.common.spells.compoundphial;

import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundPhialProjectileEntity extends ThrowableProjectile {

    private float impactDamage;
    private float splashDamage;
    private float splashRadius;


    public CompoundPhialProjectileEntity(EntityType<? extends CompoundPhialProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public CompoundPhialProjectileEntity(EntityType<? extends CompoundPhialProjectileEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, owner, level);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("impactDamage")) {
            impactDamage = tag.getFloat("impactDamage");
        }
        if(tag.contains("splashDamage")) {
            splashDamage = tag.getFloat("splashDamage");
        }
        if(tag.contains("splashRadius")) {
            splashRadius = tag.getFloat("splashRadius");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("impactDamage", impactDamage);
        tag.putFloat("splashDamage", splashDamage);
        tag.putFloat("splashRadius", splashRadius);
    }

    @Override
    protected float getGravity() {
        // ちょっと重め.
        return super.getGravity() * 1.1f;
    }

    @Override
    public void tick() {
        super.tick();

        // todo:実装.
    }

    public void setDamage(float impactDamage) {
        this.impactDamage = impactDamage;
    }

    public void setSplashDamage(float splashDamage) {
        this.splashDamage = splashDamage;
    }

    public void setSplashRadius(float splashRadius) {
        this.splashRadius = splashRadius;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        onImpact(hit.getEntity(), level());
        super.onHitEntity(hit);
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        onImpact(null, level());
        super.onHitBlock(hit);
        discard();
    }

    private void onImpact(@Nullable Entity entity, Level level) {
        var owner = getOwner();

        if (entity != null && entity.isAlive()) {
            var target = CombatTools.resolutePartEntity(entity);
            var source = CombatTools.getDamageSource(level(), this, owner, "compound_phial");
            CombatTools.applyDamage(target, impactDamage, source, SpellsRegistry.COMPOUND_PHIAL.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }

        // 一応広めに判定を取る.
        var box = new AABB(position(), position()).inflate(splashRadius * 2);
        var entities = level.getEntitiesOfClass(Entity.class, box, e -> e != entity && e.isAlive());

        for(var target : entities){
            var center = target.getBoundingBox().getCenter();
            var distance = position().distanceTo(center) - target.getBbWidth();
            if(distance <= splashRadius) {
                var scale = 0.5 + 0.5 * distance / (1 - distance / splashRadius);
                var source = CombatTools.getDamageSource(level(), this, owner, "compound_phial");
                CombatTools.applyDamage(target, Math.round(splashDamage * scale), source, SpellsRegistry.COMPOUND_PHIAL.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }
        }
    }
}
