package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.entity.PersistentSummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ServantGazeStaffEntity extends PersistentSummonWeaponEntity implements GeoEntity, AntiMagicSusceptible {
    private static final int ATTACK_INTERVAL = 40;
    private static final int[] SHOT_DELAYS = {0, 3, 6};
    private static final Vec3 FIRE_POSITION_OFFSET = new Vec3(0.0, 1.7, 0.0);
    private static final EntityDataAccessor<Boolean> SHOOTING =
            SynchedEntityData.defineId(ServantGazeStaffEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE_MAIN = RawAnimation.begin().thenLoop("idle_main");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOT = RawAnimation.begin().thenPlay("shot");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Vec3> pendingTargets = new ArrayList<>();
    private int attackSetTick = -1;
    private int spellLevel;
    private float damage;
    private double radius;
    private int attackManaCost;
    private UUID lifecycleOwnerUuid;
    private long expirationGameTime = -1L;
    private boolean lifecycleEnding;

    public ServantGazeStaffEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public ServantGazeStaffEntity(EntityType<?> type, Level level, LivingEntity owner) {
        super(type, level, owner);
        lifecycleOwnerUuid = owner.getUUID();
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOOTING, false);
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        var validation = ServantGazeManager.validate(this);
        if (validation == ServantGazeManager.ValidationResult.EXPIRED) {
            ServantGazeManager.expire(this);
            return;
        }
        if (validation == ServantGazeManager.ValidationResult.INVALID) {
            discardForLifecycle();
            return;
        }
        var owner = resolvePlayerOwner();
        if (owner == null) return;

        noPhysics = true;
        followTargetPosition(getStandbyPosition());
        setXRot(0.0F);
        setYRot(owner.getYRot());
        setRot(getYRot(), 0.0F);
        hasImpulse = true;

        if (attackSetTick >= 0) {
            firePendingShot(level, owner);
            attackSetTick++;
            if (attackSetTick > SHOT_DELAYS[SHOT_DELAYS.length - 1]) {
                attackSetTick = -1;
                pendingTargets.clear();
            }
        }
        if (entityData.get(SHOOTING) && attackSetTick < 0 && tickCount % 20 == 0) {
            entityData.set(SHOOTING, false);
        }
        if (attackSetTick < 0 && tickCount % ATTACK_INTERVAL == 0) selectTargets(level, owner);
    }

    private void selectTargets(ServerLevel level, ServerPlayer owner) {
        var candidates = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), target ->
                target != owner && target.isAlive() && distanceToSqr(target) <= radius * radius
                        && CombatTools.canBeHostileToMe(target, owner) && hasLineOfSight(level, target));
        candidates.sort(Comparator.comparingDouble(LivingEntity::getHealth).reversed()
                .thenComparingInt(Entity::getId));

        var magicData = MagicData.getPlayerMagicData(owner);
        for (var target : candidates) {
            if (pendingTargets.size() >= SHOT_DELAYS.length || magicData == null
                    || magicData.getMana() + 1.0e-4F < attackManaCost) break;
            magicData.setMana(Math.max(0.0F, magicData.getMana() - attackManaCost));
            pendingTargets.add(target.getEyePosition());
        }
        if (!pendingTargets.isEmpty()) {
            PacketDistributor.sendToPlayer(owner, new SyncManaPacket(magicData));
            attackSetTick = 0;
            entityData.set(SHOOTING, true);
        }
    }

    private boolean hasLineOfSight(ServerLevel level, LivingEntity target) {
        return level.clip(new ClipContext(getFirePosition(), target.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void firePendingShot(ServerLevel level, ServerPlayer owner) {
        for (var i = 0; i < pendingTargets.size(); i++) {
            if (SHOT_DELAYS[i] != attackSetTick) continue;
            var start = getFirePosition();
            var direction = pendingTargets.get(i).subtract(start);
            if (direction.lengthSqr() < 1.0e-6) return;
            var projectile = new ServantGazeProjectileEntity(EntityRegistry.SERVANT_GAZE_PROJECTILE.get(), level,
                    owner, damage);
            projectile.setPos(start);
            projectile.shoot(direction.x, direction.y, direction.z, projectile.getSpeed(), 0.0F);
            level.addFreshEntity(projectile);
            level.playSound(null, start.x, start.y, start.z,
                    io.redspace.ironsspellbooks.registries.SoundRegistry.ENDER_CAST.get(),
                    SoundSource.PLAYERS, 2.0F, 0.9F + level.random.nextFloat() * 0.2F);
            return;
        }
    }

    private Vec3 getFirePosition() {
        return position().add(FIRE_POSITION_OFFSET);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            var horizontalPosition = RotationTools.calculateBehindPosition(owner, -0.75, 0.9, -0.2);
            return new Vec3(horizontalPosition.x, owner.getY(), horizontalPosition.z);
        }
        return position();
    }

    public void configure(int spellLevel, float damage, double radius, int attackManaCost) {
        this.spellLevel = spellLevel;
        this.damage = Math.max(0.0F, damage);
        this.radius = Math.max(0.5, radius);
        this.attackManaCost = Math.max(0, attackManaCost);
    }

    void setLifecycleOwner(ServerPlayer owner) {
        lifecycleOwnerUuid = owner.getUUID();
        setOwner(owner);
    }

    void setExpirationGameTime(long expirationGameTime) {
        this.expirationGameTime = expirationGameTime;
    }

    long getExpirationGameTime() {
        return expirationGameTime;
    }

    boolean hasExpirationGameTime() {
        return expirationGameTime >= 0L;
    }

    ServerPlayer resolvePlayerOwner() {
        if (getOwner() instanceof ServerPlayer owner) return owner;
        if (lifecycleOwnerUuid == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(lifecycleOwnerUuid);
    }

    boolean isOwnedBy(ServerPlayer player) {
        if (lifecycleOwnerUuid != null) return lifecycleOwnerUuid.equals(player.getUUID());
        return getOwner() != null && getOwner().getUUID().equals(player.getUUID());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        configure(tag.getInt("SpellLevel"), tag.getFloat("Damage"), tag.getDouble("Radius"),
                tag.getInt("AttackManaCost"));
        lifecycleOwnerUuid = tag.hasUUID("LifecycleOwner") ? tag.getUUID("LifecycleOwner") : null;
        expirationGameTime = tag.contains("ExpirationGameTime") ? tag.getLong("ExpirationGameTime") : -1L;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpellLevel", spellLevel);
        tag.putFloat("Damage", damage);
        tag.putDouble("Radius", radius);
        tag.putInt("AttackManaCost", attackManaCost);
        if (lifecycleOwnerUuid != null) tag.putUUID("LifecycleOwner", lifecycleOwnerUuid);
        if (hasExpirationGameTime()) tag.putLong("ExpirationGameTime", expirationGameTime);
    }

    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(@NotNull Entity entity) {}
    @Override public @NotNull PushReaction getPistonPushReaction() { return PushReaction.IGNORE; }
    @Override public boolean hurt(@NotNull DamageSource source, float amount) { return false; }
    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (!level().isClientSide && !isRemoved()) {
            var owner = resolvePlayerOwner();
            if (owner != null) ServantGazeManager.cancel(owner,
                    io.redspace.ironsspellbooks.capabilities.magic.RecastResult.COUNTERSPELL);
            else discardForLifecycle();
        }
    }

    void discardForLifecycle() {
        lifecycleEnding = true;
        discard();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        var notifyOwner = reason.shouldDestroy() && !lifecycleEnding;
        lifecycleEnding = true;
        if (notifyOwner && !level().isClientSide) {
            ServantGazeManager.onDestroyed(this);
        }
        if (!isRemoved()) {
            super.remove(reason);
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        // 所有者の長距離teleport後も元chunkで停止せず、次tickで追従位置へ移動させる。
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        // recastと一体で管理し、logoutやserver再起動を跨いで実体を復元しない。
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_main", 0, state -> {
            state.setAnimation(IDLE_MAIN);
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "action", 0, state -> {
            state.setAnimation(entityData.get(SHOOTING) ? SHOT : IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
