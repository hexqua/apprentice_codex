package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.effects.DisintegrateBurstEntity;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.DamageTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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
    private static final RandomSource RNG = RandomSource.create();

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(TestBoltProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final EntityDataAccessor<Integer> DATA_STANDBY_TICK =
            SynchedEntityData.defineId(TestBoltProjectileEntity.class, EntityDataSerializers.INT);

    private float damage = 0;
    private static final int LIFE_TICKS = 80;
    private static final int DEFAULT_STANDBY_TICKS = 20;

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

        //noinspection resource
        var level = level();

        // クライアントのみで軌跡を生成.
        if (level.isClientSide && canShooting()) {

            var radius = 0.2;
            var speed = 0.05;
            var count = 3;
            for( var i = 0; i < count; i++){
                var pos = position().subtract(getDeltaMovement().scale(RNG.nextDouble()));
                level.addParticle(
                        ParticleTypes.END_ROD,
                        pos.x + getRandomRange(radius),
                        pos.y + getRandomRange(radius),
                        pos.z + getRandomRange(radius),
                        getRandomRange(speed),
                        getRandomRange(speed),
                        getRandomRange(speed)
                );
            }
        }

        if (level.isClientSide) {
            return;
        }

        if (tickCount > LIFE_TICKS) {
            discard();
        }

        if (canShooting()) {
            var hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitresult.getType() != HitResult.Type.MISS  && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
                onHit(hitresult);
            }

            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        var level = level();
        if (level.isClientSide) {
            return;
        }

        var target = hit.getEntity();
        var owner = getOwner();

        if (target instanceof LivingEntity living && target != owner) {
            var source = DamageSources.getGeneralDamageSource(level(), this, owner);
            DamageTools.applyDamage(living, damage, source, SchoolRegistry.ENDER.get(), true, true);
            onImpact(level);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        var level = level();
        if (!level.isClientSide) {
            onImpact(level);
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
        entityData.define(DATA_STANDBY_TICK, DEFAULT_STANDBY_TICKS);
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

    private boolean canShooting(){
        return tickCount >= getStandbyTicks();
    }

    private int getStandbyTicks() {
        return this.entityData.get(DATA_STANDBY_TICK);
    }

    public void setStandbyTicks(int ticks) {
        entityData.set(DATA_STANDBY_TICK, ticks);
    }

    private double getRandomRange(double range){
        return (RNG.nextDouble() * 2 - 1) * range;
    }

    private void onImpact(Level level) {
        if (!level.isClientSide) {
            // 命中位置で演出を出すと手前すぎるので少し進行方向に進める.
            var dir = getDeltaMovement();
            var impactPos = position().add(dir.scale(0.1));
            spawnDisintegrate(impactPos);

            var volume = 1.0f;
            var pitch = 1.0f;
            level.playSound(
                    null,
                    getX(), getY(), getZ(),
                    SoundEvents.PLAYER_ATTACK_WEAK,
                    SoundSource.PLAYERS,
                    volume,
                    pitch
            );
        }
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
