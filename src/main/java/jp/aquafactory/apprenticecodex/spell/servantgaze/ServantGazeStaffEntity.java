package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public ServantGazeStaffEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public ServantGazeStaffEntity(EntityType<?> type, Level level, LivingEntity owner) {
        super(type, level, owner);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SHOOTING, false);
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != level) {
            discard();
            return;
        }

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

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        configure(tag.getInt("SpellLevel"), tag.getFloat("Damage"), tag.getDouble("Radius"),
                tag.getInt("AttackManaCost"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpellLevel", spellLevel);
        tag.putFloat("Damage", damage);
        tag.putDouble("Radius", radius);
        tag.putInt("AttackManaCost", attackManaCost);
    }

    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(@NotNull Entity entity) {}
    @Override public @NotNull PushReaction getPistonPushReaction() { return PushReaction.IGNORE; }
    @Override public boolean hurt(@NotNull DamageSource source, float amount) { return false; }
    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (!level().isClientSide && !isRemoved()) {
            if (getOwner() instanceof ServerPlayer owner) ServantGazeManager.deactivate(owner);
            else discard();
        }
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
