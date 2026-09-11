package jp.aquafactory.apprenticecodex.spell.lightningarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.LightningArrowImpactPacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LightningArrowEntity extends Projectile implements AntiMagicSusceptible {
    public static final int TRAIL_TICKS = 4;
    private static final EntityDataAccessor<CompoundTag> FLIGHT =
            SynchedEntityData.defineId(LightningArrowEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> DISTANCE =
            SynchedEntityData.defineId(LightningArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STOP_TICKS =
            SynchedEntityData.defineId(LightningArrowEntity.class, EntityDataSerializers.INT);
    private final Set<UUID> hitTargets = new HashSet<>();
    private float damage;
    private double previousVisualDistance;
    private double visualDistance;
    private double particleDistance;
    private int previousStopTicks;
    private int visualStopTicks;
    private boolean visualInitialized;

    public LightningArrowEntity(EntityType<? extends LightningArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLIGHT, new CompoundTag());
        builder.define(DISTANCE, 0.0F);
        builder.define(STOP_TICKS, 0);
    }

    public void launch(LivingEntity owner, Vec3 origin, Vec3 direction, double range, float damage) {
        setOwner(owner);
        this.damage = damage;
        var flight = new LightningArrowFlight(origin, direction, LightningArrowFlight.SPEED, range, random.nextInt());
        entityData.set(FLIGHT, flight.encode());
        setPos(origin);
        setDeltaMovement(flight.direction().scale(flight.speed()));
    }

    public @Nullable LightningArrowFlight flight() {
        var tag = entityData.get(FLIGHT);
        return tag.isEmpty() ? null : LightningArrowFlight.decode(tag);
    }

    public double traveledDistance() {
        return entityData.get(DISTANCE);
    }

    public boolean stopped() {
        return entityData.get(STOP_TICKS) > 0;
    }

    public double visualDistance(float partialTick) {
        return level().isClientSide ? Mth.lerp(partialTick, previousVisualDistance, visualDistance) : traveledDistance();
    }

    public float visualStopAge(float partialTick) {
        return Math.max(0, Mth.lerp(partialTick, previousStopTicks, visualStopTicks) - 1);
    }

    @Override
    public void tick() {
        super.tick();
        var flight = flight();
        if (flight == null) return;
        if (level().isClientSide) {
            previousVisualDistance = visualDistance;
            visualDistance = traveledDistance();
            if (!visualInitialized) {
                // 途中から追跡した観測者にも、初回の1tickで発射点から瞬間移動して見せない。
                previousVisualDistance = Math.max(0, visualDistance - flight.speed());
                particleDistance = Math.floor(Math.max(0, visualDistance - flight.speed() * TRAIL_TICKS));
                visualInitialized = true;
            }
            previousStopTicks = visualStopTicks;
            visualStopTicks = entityData.get(STOP_TICKS);
            setPos(flight.point(visualDistance));
            // 通過済み距離だけから補間するため、速度同期の切り詰めや壁の先への予測が入らない。
            spawnTrailParticles(flight);
            return;
        }
        if (stopped()) {
            int age = entityData.get(STOP_TICKS);
            if (age >= TRAIL_TICKS) discard();
            else entityData.set(STOP_TICKS, age + 1);
            return;
        }
        var start = position();
        var startBlock = BlockPos.containing(start);
        var startShape = level().getBlockState(startBlock).getCollisionShape(level(), startBlock, CollisionContext.of(this));
        // VoxelShape.clipのinside判定は移動量の0.1%だけ前進する。始点が内部なら一切進めない。
        if (startShape.toAabbs().stream().anyMatch(box -> box.move(startBlock).contains(start))) {
            stop();
            Networks.sendToPlayersNear((ServerLevel) level(), start, 96,
                    new LightningArrowImpactPacket(start, flight.direction(), true));
            return;
        }
        var distance = traveledDistance();
        var nextDistance = Math.min(flight.range(), distance + flight.speed());
        var end = flight.point(nextDistance);
        var block = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        var blocked = block.getType() == HitResult.Type.BLOCK;
        if (blocked) end = block.getLocation();
        var server = (ServerLevel) level();
        for (var hit : LightningArrowCollision.contacts(level(), this, getOwner(), start, end)) {
            if (!hitTargets.add(hit.target().getUUID())) continue;
            CombatTools.applyDamage(hit.target(), damage,
                    CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.LIGHTNING_ARROW),
                    SchoolRegistry.LIGHTNING.get(), CombatTools.KnockbackTypes.DEFAULT);
            Networks.sendToPlayersNear(server, hit.position(), 96,
                    new LightningArrowImpactPacket(hit.position(), flight.direction(), false));
            level().playSound(null, hit.position().x, hit.position().y, hit.position().z,
                    SoundRegistry.SMALL_LIGHTNING_STRIKE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        setPos(end);
        entityData.set(DISTANCE, (float) Math.min(flight.range(), distance + start.distanceTo(end)));
        if (blocked || nextDistance >= flight.range()) {
            stop();
            if (blocked) Networks.sendToPlayersNear(server, end, 96,
                    new LightningArrowImpactPacket(end, flight.direction(), true));
        }
    }

    private void stop() {
        entityData.set(STOP_TICKS, 1);
        setDeltaMovement(Vec3.ZERO);
    }

    private void spawnTrailParticles(LightningArrowFlight flight) {
        while (particleDistance + 1 <= previousVisualDistance) {
            particleDistance++;
            var point = flight.point(particleDistance);
            spawnTrailParticle(point, false);
            if ((int) particleDistance % 3 == 0) spawnTrailParticle(point, true);
        }
    }

    private void spawnTrailParticle(Vec3 point, boolean rhombus) {
        var options = new AdditiveGlowParticleOptions(
                rhombus ? ParticleRegistry.ADDITIVE_RHOMBUS.get() : ParticleRegistry.ADDITIVE_SPARK.get(),
                rhombus ? 0.16F : 0.08F, 0.42F, 0.86F, 1.0F,
                2, 6, 2, 0.9F, 1.15F, 0.86F, 1.0F, 0.02F, 0.4F, 0.52F, !rhombus);
        level().addParticle(options, point.x, point.y, point.z,
                (random.nextDouble() - 0.5) * 0.06, (random.nextDouble() - 0.5) * 0.06,
                (random.nextDouble() - 0.5) * 0.06);
    }

    // vanilla位置補間と独立した距離同期を二重適用すると、5ブロック/tickの矢が往復して見える。
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        if (!level().isClientSide && !stopped()) stop();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var flight = flight();
        return flight == null ? getBoundingBox() : new AABB(flight.point(Math.max(0, traveledDistance() - 20)), position()).inflate(1);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
