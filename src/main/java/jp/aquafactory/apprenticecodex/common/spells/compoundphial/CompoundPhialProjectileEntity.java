package jp.aquafactory.apprenticecodex.common.spells.compoundphial;

import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CompoundPhialProjectileEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> POTION_COLOR =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> BURST_RADIUS =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<ItemStack> POTION_ITEM =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(POTION_COLOR, 0);
        builder.define(BURST_RADIUS, 0.5f);
        builder.define(POTION_ITEM, ItemStack.EMPTY);
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
    protected double getDefaultGravity() {
        // ちょっと重め.
        return super.getDefaultGravity() * 1.1;
    }

    @Override
    public void tick() {
        super.tick();

        @SuppressWarnings("resource") var level = level();
        if (level.isClientSide) {
            var p = position();
            var c = getColorArray();
            var particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, c[0], c[1], c[2]);
            level.addParticle(particle,
                    p.x, p.y + 0.1, p.z,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        var level = level();

        if (!level.isClientSide){
            return;
        }

        if (id == 3) {
            spawnPotionBurst(level);
        }
    }

    public void setDamage(float impactDamage) {
        this.impactDamage = impactDamage;
    }

    public void setSplashDamage(float splashDamage) {
        this.splashDamage = splashDamage;
    }

    public void setSplashRadius(float splashRadius) {
        this.splashRadius = splashRadius;
        entityData.set(BURST_RADIUS, splashRadius);
    }

    public void setPotionColorRandom(Level level){
        entityData.set(POTION_COLOR, level.random.nextInt(0xFFFFFF));

        var item = new ItemStack(Items.SPLASH_POTION);
        var contents = new PotionContents(Optional.empty(), Optional.of(entityData.get(POTION_COLOR)), List.of());
        item.set(DataComponents.POTION_CONTENTS, contents);
        entityData.set(POTION_ITEM, item);
    }

    public ItemStack getPotionItem(){
        return entityData.get(POTION_ITEM);
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

        // バニラスプラッシュしつつおまけを追加.
        var color = entityData.get(POTION_COLOR);
        level.levelEvent(2002, BlockPos.containing(position()), color);
        level.broadcastEntityEvent(this, (byte)3);

        // 一応広めに判定を取る.
        var box = new AABB(position(), position()).inflate(splashRadius * 2);
        var entities = level.getEntitiesOfClass(Entity.class, box, e -> e != entity && e.isAlive());

        for(var target : entities){
            var center = target.getBoundingBox().getCenter();
            var distance = position().distanceTo(center) - target.getBbWidth();
            if(distance <= splashRadius) {
                var scale = 0.5 + 0.5 * (1 - distance / splashRadius);
                var source = CombatTools.getDamageSource(level(), this, owner, "compound_phial");
                CombatTools.applyDamage(target, Math.round(splashDamage * scale), source, SpellsRegistry.COMPOUND_PHIAL.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }
        }
    }

    private float[] getColorArray() {
        int color = entityData.get(POTION_COLOR);
        var r = ((color >> 16) & 255) / 255f;
        var g = ((color >> 8) & 255) / 255f;
        var b = (color & 255) / 255f;
        return new float[]{r, g, b};
    }

    private void spawnPotionBurst(Level level) {
        final var color = getColorArray();
        final float radius = entityData.get(BURST_RADIUS);
        final var count = Math.round(24 * radius);

        var rand = level.getRandom();
        var p = position();
        var startAngle = rand.nextDouble() * Math.PI * 2;

        for (var i = 0; i < count; i++) {
            // できる限り範囲がわかりやすいように端に散りやすくする.
            var angle = startAngle + Math.PI * 2 * i / count + rand.nextDouble() * 0.05;
            var d = radius * (0.75 +Math.sqrt(rand.nextDouble()) * 0.25);
            var ox = Math.cos(angle) * d;
            var oz = Math.sin(angle) * d;
            var oy = (rand.nextDouble() - 0.5) * radius * 0.4;
            var particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color[0], color[1], color[2]);
            level.addParticle(particle,
                    p.x + ox, p.y + oy, p.z + oz,
                    0.0, 0.0, 0.0);
        }
    }
}
