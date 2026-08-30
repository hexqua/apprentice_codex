package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.BloodEngravedEffect;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.ForgeProjectileImpactTools;
import jp.aquafactory.apprenticecodex.utility.ProjectileCollisionTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class BloodBrandKunai extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 1.68D;
    private static final int LIFE_TICKS = 20 * 5;
    private static final double GRAVITY = 0.03D;
    private static final double AIR_DRAG = 0.99D;
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F);

    private float damage;
    private float burstDamage;
    private double burstRange;
    private @Nullable UUID casterUuid;
    private @Nullable Entity fallbackOwner;

    public BloodBrandKunai(EntityType<? extends BloodBrandKunai> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0F);
    }

    public BloodBrandKunai(EntityType<? extends BloodBrandKunai> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
        casterUuid = owner.getUUID();
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        fallbackOwner = owner;
    }

    @Override
    public @Nullable Entity getOwner() {
        var owner = super.getOwner();
        return owner != null ? owner : fallbackOwner;
    }

    public void setProjectileVelocity(Vec3 direction) {
        var resolved = direction.lengthSqr() > 1.0E-8D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        setDeltaMovement(resolved.scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        var cancelledBlockHit = (BlockHitResult) null;
        if (level().isClientSide) {
            spawnTrailParticles();
        } else if (tickCount > LIFE_TICKS) {
            discard();
            return;
        } else {
            var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                var impactAction = ForgeProjectileImpactTools.resolveImpactAction(this, hitResult);
                if (impactAction == ForgeProjectileImpactTools.ImpactAction.CONTINUE) {
                    if (hitResult instanceof BlockHitResult blockHit) {
                        cancelledBlockHit = blockHit;
                    }
                } else if (impactAction == ForgeProjectileImpactTools.ImpactAction.PROCESS) {
                    onHit(hitResult);
                } else {
                    discard();
                }
            }
            if (isRemoved()) {
                return;
            }
        }

        // Projectileは両側で同じ弾道を進め、serverの位置同期で補正する。
        var movementStart = position();
        var requestedMovement = getDeltaMovement();
        move(MoverType.SELF, requestedMovement);
        if (!level().isClientSide && (horizontalCollision || verticalCollision)) {
            // 中心線から外れた当たり箱の接触も、通常のブロック着弾と同じ経路で処理する。
            var physicalHit = cancelledBlockHit != null
                    ? cancelledBlockHit
                    : ProjectileCollisionTools.findPhysicalBlockHit(this, movementStart, requestedMovement);
            var impactAction = physicalHit == null || cancelledBlockHit != null
                    ? ForgeProjectileImpactTools.ImpactAction.CONTINUE
                    : ForgeProjectileImpactTools.resolveImpactAction(this, physicalHit);
            if (physicalHit != null && impactAction == ForgeProjectileImpactTools.ImpactAction.CONTINUE) {
                ProjectileCollisionTools.continueAfterCancelledImpact(this, movementStart, requestedMovement);
            } else if (physicalHit != null && impactAction == ForgeProjectileImpactTools.ImpactAction.PROCESS) {
                setDeltaMovement(requestedMovement);
                onHit(physicalHit);
            } else {
                discard();
            }
        }
        if (isRemoved()) {
            return;
        }
        var movement = getDeltaMovement().scale(AIR_DRAG);
        if (!isNoGravity()) {
            movement = movement.add(0.0D, -GRAVITY, 0.0D);
        }
        setDeltaMovement(movement);
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        setPos(hitResult.getLocation());
        spawnImpactParticles(serverLevel);
        var owner = getOwner();
        var target = CombatTools.resolutePartEntity(hitResult.getEntity());
        if (!CombatTools.isValidCombatTarget(target, owner)) {
            discard();
            return;
        }

        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.BLOOD_BRAND);
        var damaged = CombatTools.applyDamage(
                target,
                damage,
                source,
                SpellRegistry.BLOOD_BRAND.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );
        if (damaged && target instanceof LivingEntity livingTarget && livingTarget.isAlive()
                && !(livingTarget instanceof Player) && casterUuid != null) {
            var effect = new MobEffectInstance(
                    EffectRegistry.BLOOD_ENGRAVED.get(),
                    BloodEngravedEffect.DURATION_TICKS,
                    0,
                    false,
                    true,
                    true
            );
            livingTarget.addEffect(effect, owner);
            if (livingTarget.hasEffect(EffectRegistry.BLOOD_ENGRAVED.get())) {
                // 再刻印は最後にダメージを通した術者と威力へ置き換える。
                BloodBrandState.set(livingTarget, new BloodBrandState(casterUuid, burstDamage, burstRange));
            }
        }
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            setPos(hitResult.getLocation());
            spawnImpactParticles(serverLevel);
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var target = CombatTools.resolutePartEntity(entity);
        return entity != getOwner()
                && CombatTools.isValidCombatTarget(target, getOwner())
                && super.canHitEntity(entity);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (!level().isClientSide && !isRemoved()) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("BurstDamage", burstDamage);
        tag.putDouble("BurstRange", burstRange);
        if (casterUuid != null) {
            tag.putUUID("Caster", casterUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        burstDamage = tag.getFloat("BurstDamage");
        burstRange = tag.getDouble("BurstRange");
        casterUuid = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setBurstDamage(float burstDamage) {
        this.burstDamage = burstDamage;
    }

    public void setBurstRange(double burstRange) {
        this.burstRange = burstRange;
    }

    public float getDamageForGameTest() {
        return damage;
    }

    public float getBurstDamageForGameTest() {
        return burstDamage;
    }

    public double getBurstRangeForGameTest() {
        return burstRange;
    }

    @Nullable BloodBrandState createBurstState() {
        return casterUuid == null ? null : new BloodBrandState(casterUuid, burstDamage, burstRange);
    }

    private void spawnTrailParticles() {
        var random = level().random;
        var pos = position().subtract(getDeltaMovement().scale(random.nextDouble()));
        level().addParticle(
                BLOOD_DUST,
                pos.x + (random.nextDouble() - 0.5D) * 0.1D,
                pos.y + (random.nextDouble() - 0.5D) * 0.1D,
                pos.z + (random.nextDouble() - 0.5D) * 0.1D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private void spawnImpactParticles(ServerLevel serverLevel) {
        // vanillaのentity event IDは型ごとの専用処理と衝突し得るため、通常のparticle packetで同期する。
        serverLevel.sendParticles(BLOOD_DUST, getX(), getY(), getZ(), 12, 0.08D, 0.08D, 0.08D, 0.02D);
    }
}
