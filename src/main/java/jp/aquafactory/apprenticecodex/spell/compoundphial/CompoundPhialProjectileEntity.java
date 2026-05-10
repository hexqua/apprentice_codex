package jp.aquafactory.apprenticecodex.spell.compoundphial;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class CompoundPhialProjectileEntity extends ThrowableProjectile {
    private static final float FULL_DAMAGE_RADIUS_SCALE = 0.5f;
    private static final float MIN_SPLASH_DAMAGE_SCALE = 0.6f;

    private static final EntityDataAccessor<Integer> POTION_COLOR =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BURST_RADIUS =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ItemStack> POTION_ITEM =
            SynchedEntityData.defineId(CompoundPhialProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private float damage;
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
        if (tag.contains("damage")) {
            damage = tag.getFloat("damage");
        } else if (tag.contains("impactDamage")) {
            damage = tag.getFloat("impactDamage");
        }
        if (tag.contains("splashRadius")) {
            splashRadius = tag.getFloat("splashRadius");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putFloat("splashRadius", splashRadius);
    }

    @Override
    protected double getDefaultGravity() {
        return super.getDefaultGravity() * 1.1;
    }

    @Override
    public void tick() {
        super.tick();

        var level = level();
        if (level.isClientSide) {
            var p = position();
            var c = getColorArray();
            level.addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, c[0], c[1], c[2]),
                    p.x,
                    p.y + 0.1,
                    p.z,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        var level = level();

        if (!level.isClientSide) {
            return;
        }

        if (id == 3) {
            spawnPotionBurst(level);
        }
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSplashRadius(float splashRadius) {
        this.splashRadius = splashRadius;
        entityData.set(BURST_RADIUS, splashRadius);
    }

    public void setPotionColorRandom(Level level) {
        entityData.set(POTION_COLOR, level.random.nextInt(0xFFFFFF));

        var item = new ItemStack(Items.SPLASH_POTION);
        CustomData.update(DataComponents.CUSTOM_DATA, item, tag -> tag.putInt("CustomPotionColor", entityData.get(POTION_COLOR)));
        entityData.set(POTION_ITEM, item);
    }

    public ItemStack getPotionItem() {
        return entityData.get(POTION_ITEM);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        onImpact(level());
        super.onHitEntity(hit);
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        onImpact(level());
        super.onHitBlock(hit);
        discard();
    }

    private void onImpact(Level level) {
        var owner = getOwner();

        var color = entityData.get(POTION_COLOR);
        level.levelEvent(2002, BlockPos.containing(position()), color);
        level.broadcastEntityEvent(this, (byte) 3);

        var box = new AABB(position(), position()).inflate(splashRadius * 2);
        var entities = level.getEntitiesOfClass(Entity.class, box, e -> CombatTools.isValidCombatTarget(e, null) && e.isAlive());
        var damagedTargets = new HashSet<Entity>();

        for (var target : entities) {
            var resolvedTarget = CombatTools.resolutePartEntity(target);
            var distance = distanceToBoundingBox(target.getBoundingBox());
            if (distance <= splashRadius) {
                var scale = getSplashDamageScale(distance);
                var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.COMPOUND_PHIAL);
                if (damagedTargets.add(resolvedTarget)) {
                    CombatTools.applyDamage(resolvedTarget, damage * scale, source, SpellRegistry.COMPOUND_PHIAL.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                }
            }
        }
    }

    private float getSplashDamageScale(double distance) {
        var fullDamageRadius = splashRadius * FULL_DAMAGE_RADIUS_SCALE;
        if (distance <= fullDamageRadius) {
            return 1.0f;
        }

        var falloffRange = Math.max(splashRadius - fullDamageRadius, 0.001f);
        var falloffProgress = Math.min(1.0, (distance - fullDamageRadius) / falloffRange);
        return (float) (1.0 - (1.0 - MIN_SPLASH_DAMAGE_SCALE) * falloffProgress);
    }

    private double distanceToBoundingBox(AABB box) {
        var p = position();
        var x = distanceOutsideAxis(p.x, box.minX, box.maxX);
        var y = distanceOutsideAxis(p.y, box.minY, box.maxY);
        var z = distanceOutsideAxis(p.z, box.minZ, box.maxZ);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double distanceOutsideAxis(double point, double min, double max) {
        if (point < min) {
            return min - point;
        }
        if (point > max) {
            return point - max;
        }
        return 0.0;
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
            var angle = startAngle + Math.PI * 2 * i / count + rand.nextDouble() * 0.05;
            var d = radius * (0.75 + Math.sqrt(rand.nextDouble()) * 0.25);
            var ox = Math.cos(angle) * d;
            var oz = Math.sin(angle) * d;
            var oy = (rand.nextDouble() - 0.5) * radius * 0.4;
            level.addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color[0], color[1], color[2]),
                    p.x + ox,
                    p.y + oy,
                    p.z + oz,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }
}
