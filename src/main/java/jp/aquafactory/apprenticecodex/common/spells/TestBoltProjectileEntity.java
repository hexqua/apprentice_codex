package jp.aquafactory.apprenticecodex.common.spells;

import jp.aquafactory.apprenticecodex.common.effects.DisintegrateBurstEntity;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.DamageTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class TestBoltProjectileEntity extends ThrowableProjectile
{
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(TestBoltProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final float DAMAGE = 4.0f;
    private static final int LIFE_TICKS = 80;

    public TestBoltProjectileEntity(EntityType<? extends TestBoltProjectileEntity> type, Level level) {
        super(type, level);
    }

    public TestBoltProjectileEntity(EntityType<? extends TestBoltProjectileEntity> type, Level level, LivingEntity owner, ItemStack stack) {
        super(type, owner, level);
        setItem(stack);
    }

    @Override
    public void tick() {
        super.tick();

        // todo:warning fix.
        // noinspection resource
        if (!level().isClientSide && this.tickCount > LIFE_TICKS) {
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        // todo:warning fix.
        // noinspection resource
        if (level().isClientSide) return;

        Entity target = hit.getEntity();
        Entity owner = this.getOwner();

        if (target instanceof LivingEntity living && target != owner) {
            // todo: change to Iron's damage.
            DamageSource src = this.damageSources().indirectMagic(this, owner);
            DamageTools.applyDamage(living, DAMAGE, src, true, true);

            // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
            Vec3 dir = this.getDeltaMovement();
            Vec3 impactPos = this.position().add(dir.scale(0.5));
            spawnDisintegrate(impactPos);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        // todo:warning fix.
        // noinspection resource
        if (!level().isClientSide) {
            // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
            Vec3 dir = this.getDeltaMovement();
            Vec3 impactPos = this.position().add(dir.scale(0.5));
            spawnDisintegrate(impactPos);
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Item")) {
            setItem(ItemStack.of(tag.getCompound("Item")));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!getItem().isEmpty()) {
            tag.put("Item", getItem().save(new CompoundTag()));
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }


    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(DATA_ITEM, stack.copy());
    }

    private void spawnDisintegrate(Vec3 impactPos) {
        if (!(level() instanceof ServerLevel)) return;

        Vec3 dir = this.getDeltaMovement();
        if (dir.lengthSqr() < 1.0e-6) {
            dir = this.getLookAngle();
        }

        DisintegrateBurstEntity burst = new DisintegrateBurstEntity(EntityRegistry.DISINTEGRATE_BURST.get(), level());
        burst.setPos(impactPos.x, impactPos.y, impactPos.z);
        burst.setup(4, 0.2f, 4, dir);
        level().addFreshEntity(burst);
    }
}
