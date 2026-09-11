package jp.aquafactory.apprenticecodex.spell.bloodyarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidSource;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

public final class BloodyArrowOrbEntity extends Entity implements AntiMagicSusceptible {
    public static final int LIFETIME = 200;
    public static final int LIFETIME_VARIANCE = 40;
    private static final EntityDataAccessor<Long> EXPIRES_AT =
            SynchedEntityData.defineId(BloodyArrowOrbEntity.class, EntityDataSerializers.LONG);
    private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(1, 0.05F, 0.05F), 0.7F);
    // server thread専用。回収者の参照を強く保持せず、同時回収の音だけを間引く。
    private static final Map<LivingEntity, Long> LAST_PICKUP_SOUND = new WeakHashMap<>();
    private UUID casterUuid;
    private float healing;
    private int lifetime = LIFETIME;
    private Vec3 waypoint;
    private Vec3 destination;
    private boolean collected;
    private Vec3 lerpTarget;
    private int lerpSteps;

    public BloodyArrowOrbEntity(EntityType<? extends BloodyArrowOrbEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        entityData.set(EXPIRES_AT, level.getGameTime() + LIFETIME);
    }

    public void spawn(LivingEntity caster, Vec3 position, float healing) {
        casterUuid = caster.getUUID();
        this.healing = healing;
        lifetime = LIFETIME + random.nextInt(LIFETIME_VARIANCE + 1);
        entityData.set(EXPIRES_AT, level().getGameTime() + lifetime);
        setPos(position);
        setDeltaMovement(Vec3.ZERO);
    }

    public void scatter(Vec3 anchor, Vec3 destination) {
        this.waypoint = anchor;
        this.destination = destination;
    }

    public int lifetimeTicks() { return lifetime; }

    public float opacity(float partialTick) {
        return Mth.clamp((entityData.get(EXPIRES_AT) - level().getGameTime() - partialTick) / 12F, 0, 1);
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(position().lerp(lerpTarget, 1.0 / lerpSteps));
                lerpSteps--;
            }
            if (tickCount % 5 == 0) {
                var spark = new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.04F, 1, 0.05F, 0.05F, 0);
                level().addParticle(spark, getX() + (random.nextDouble() - 0.5) * 0.35,
                        getY() + 0.15, getZ() + (random.nextDouble() - 0.5) * 0.35, 0, 0.01, 0);
            }
            return;
        }
        if (level().getGameTime() >= entityData.get(EXPIRES_AT) || tickCount >= lifetime) {
            ((ServerLevel) level()).sendParticles(BLOOD_DUST, getX(), getY() + 0.12, getZ(),
                    10, 0.16, 0.16, 0.16, 0.03);
            discard();
            return;
        }
        if (advanceScatter()) return;
        var server = (ServerLevel) level();
        var owner = casterUuid == null ? null : server.getEntity(casterUuid);
        if (!(owner instanceof LivingEntity caster) || caster.isRemoved() || caster.level() != level()) return;
        var target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5),
                        candidate -> canCollect(caster, candidate))
                .stream().min(Comparator.<LivingEntity>comparingDouble(this::distanceToSqr)
                        .thenComparingInt(Entity::getId));
        target.ifPresent(candidate -> {
            // 回復イベントが別の処理を呼び戻しても、同じオーブを再利用させない。
            if (collected || isRemoved()) return;
            collected = true;
            CombatTools.applySpellHealing(caster, candidate, healing, SchoolRegistry.BLOOD.get());
            long now = level().getGameTime();
            var previous = LAST_PICKUP_SOUND.get(candidate);
            if (previous == null || now < previous || now - previous >= 3) {
                LAST_PICKUP_SOUND.put(candidate, now);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS, 0.25F, 1.0F + random.nextFloat() * 0.3F);
            }
            discard();
        });
    }

    private boolean advanceScatter() {
        if (waypoint == null) return false;
        var offset = waypoint.subtract(position());
        if (offset.lengthSqr() < 0.0064) {
            setDeltaMovement(Vec3.ZERO);
            if (destination != null && waypoint.distanceToSqr(destination) > 0.0064) {
                waypoint = destination;
                destination = null;
                return true;
            }
            waypoint = null;
            destination = null;
            return false;
        }
        var movement = offset.scale(Math.min(0.22, 1.2 / offset.length()));
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        if (horizontalCollision || verticalCollision) {
            // 散布後にブロックが置かれた場合も壁を抜けず、その手前で回収可能にする。
            waypoint = null;
            destination = null;
            setDeltaMovement(Vec3.ZERO);
        }
        return true;
    }

    private boolean canCollect(LivingEntity caster, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()
                || (!(target instanceof Player) && target.getHealth() >= target.getMaxHealth())) return false;
        if (target != caster && !caster.isAlliedTo(target) && !isOwnedBy(target, caster)) return false;
        // AABBの近接判定だけでは薄い壁越しにも回復するため、中心間の遮蔽を確認する。
        return level().clip(new ClipContext(getBoundingBox().getCenter(), target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private static boolean isOwnedBy(LivingEntity target, LivingEntity caster) {
        var uuid = caster.getUUID();
        if (target instanceof OwnableEntity ownable && uuid.equals(ownable.getOwnerUUID())) return true;
        if (target instanceof IMagicSummon summon && summon.getSummoner() != null
                && uuid.equals(summon.getSummoner().getUUID())) return true;
        return target instanceof CombatOwnerUuidSource source && uuid.equals(source.getCombatOwnerUuid());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpTarget = new Vec3(x, y, z);
        lerpSteps = Math.max(1, steps);
    }

    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public void onAntiMagic(MagicData data) { if (!level().isClientSide) discard(); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(EXPIRES_AT, 0L); }
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
}
