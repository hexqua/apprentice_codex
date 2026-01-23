package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.effects.DisintegrateBurstEntity;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.DamageTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class TestBoltProjectileEntity extends Projectile
{
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(TestBoltProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private float damage = 0;
    private static final int LIFE_TICKS = 80;

    public TestBoltProjectileEntity(EntityType<? extends TestBoltProjectileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public TestBoltProjectileEntity(EntityType<? extends TestBoltProjectileEntity> type, Level level, LivingEntity owner, ItemStack stack) {
        super(type, level);
        setItem(stack);
        setOwner(owner);
    }

    public void setProjectileVelocity(Vec3 rotation, float speed) {
        setDeltaMovement(rotation.scale(speed));
    }

    @Override
    public void tick() {
        super.tick();

        // クライアントでも動かすことで滑らかにする.
        setPos(position().add(getDeltaMovement()));
        ProjectileUtil.rotateTowardsMovement(this, 1);

        // noinspection resource
        if (level().isClientSide) {
            return;
        }

        if (tickCount > LIFE_TICKS) {
            discard();
        }

        var hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS  && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
            onHit(hitresult);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        // noinspection resource
        if (level().isClientSide) return;

        var target = hit.getEntity();
        var owner = getOwner();

        if (target instanceof LivingEntity living && target != owner) {
            var source = DamageSources.getGeneralDamageSource(level(), this, owner);
            DamageTools.applyDamage(living, damage, source, SchoolRegistry.ENDER.get(), true, true);

            // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
            var dir = getDeltaMovement();
            var impactPos = position().add(dir.scale(0.5));
            spawnDisintegrate(impactPos);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        // noinspection resource
        if (!level().isClientSide) {
            // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
            var dir = getDeltaMovement();
            var impactPos = position().add(dir.scale(0.5));
            spawnDisintegrate(impactPos);
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Item")) {
            setItem(ItemStack.of(tag.getCompound("Item")));
        }
        if (tag.contains("damage")) {
            damage = tag.getFloat("damage");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        if (!getItem().isEmpty()) {
            tag.put("Item", getItem().save(new CompoundTag()));
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public ItemStack getItem() {
        return entityData.get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM, stack.copy());
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    private void spawnDisintegrate(Vec3 impactPos) {
        //noinspection resource
        var level = level();
        if (!(level instanceof ServerLevel)) return;

        var dir = getDeltaMovement();
        if (dir.lengthSqr() < 1.0e-6) {
            dir = getLookAngle();
        }

        var burst = new DisintegrateBurstEntity(EntityRegistry.DISINTEGRATE_BURST.get(), level());
        burst.setPos(impactPos.x, impactPos.y, impactPos.z);
        burst.setup(4, 0.2f, 4, dir);
        level.addFreshEntity(burst);
    }
}
