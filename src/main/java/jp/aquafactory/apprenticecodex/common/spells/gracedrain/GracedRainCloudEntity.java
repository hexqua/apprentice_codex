package jp.aquafactory.apprenticecodex.common.spells.gracedrain;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GracedRainCloudEntity extends SummonWeaponEntity {

    public static final double HEIGHT_OFFSET = 3.0;
    private static final float DEFAULT_RADIUS = 2.5f;
    private static final float DEFAULT_THICKNESS = 0.8f;

    private static final EntityDataAccessor<Float> CLOUD_RADIUS =
            SynchedEntityData.defineId(GracedRainCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CLOUD_THICKNESS =
            SynchedEntityData.defineId(GracedRainCloudEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable UUID followTargetUuid;
    private @Nullable Entity cachedFollowTarget;
    private @Nullable Vec3 anchorPosition;
    private @Nullable BlockPos anchorBlockPos;
    private int growthIntervalTicks = 40;
    private int growthTick;

    public GracedRainCloudEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public GracedRainCloudEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CLOUD_RADIUS, DEFAULT_RADIUS);
        entityData.define(CLOUD_THICKNESS, DEFAULT_THICKNESS);
    }

    public void setFollowTarget(Entity target) {
        followTargetUuid = target.getUUID();
        cachedFollowTarget = target;
        anchorBlockPos = null;
        anchorPosition = toCloudPosition(RaycastTools.getEntityTargetPosition(target));
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    public void setAnchorPosition(Vec3 anchorPosition) {
        this.anchorPosition = anchorPosition;
        followTargetUuid = null;
        cachedFollowTarget = null;
        anchorBlockPos = null;
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    public void setAnchorBlock(BlockPos blockPos) {
        anchorBlockPos = blockPos.immutable();
        followTargetUuid = null;
        cachedFollowTarget = null;
        anchorPosition = toCloudPosition(Vec3.atCenterOf(blockPos));
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (level.isClientSide) {
            spawnCloudParticles(level);
            spawnRainParticles(level);
            return;
        }

        if (!(getOwner() instanceof LivingEntity)) {
            discard();
            return;
        }

        var targetPos = resolveTargetPosition(level);
        if (targetPos != null) {
            followTargetPosition(targetPos);
        }

        if (anchorBlockPos != null && level instanceof ServerLevel serverLevel) {
            if (++growthTick >= Math.max(1, growthIntervalTicks)) {
                growthTick = 0;
                tryGrowPlant(serverLevel);
            }
        }
    }

    private void spawnCloudParticles(Level level) {
        var random = level.getRandom();
        var center = position();
        var radius = Math.max(0.1f, getCloudRadius());
        var thickness = Math.max(0.1f, getCloudThickness());
        var count = Math.max(4, Math.min(40, (int) Math.round(radius * 6.0)));
        var speed = 0.01;

        for (var i = 0; i < count; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0;
            var distance = Math.sqrt(random.nextDouble()) * radius;
            var x = center.x + Math.cos(angle) * distance;
            var z = center.z + Math.sin(angle) * distance;
            var y = center.y + (random.nextDouble() - 0.5) * thickness;
            var dx = (random.nextDouble() - 0.5) * speed;
            var dy = (random.nextDouble() - 0.5) * speed * 0.2;
            var dz = (random.nextDouble() - 0.5) * speed;
            level.addParticle(ParticleTypes.CLOUD, x, y, z, dx, dy, dz);
        }
    }

    private void spawnRainParticles(Level level) {
        if ((tickCount & 1) != 0) {
            return;
        }

        var random = level.getRandom();
        var center = position();
        var radius = Math.max(0.1f, getCloudRadius());
        var thickness = Math.max(0.1f, getCloudThickness());
        var count = Math.max(2, Math.min(16, (int) Math.round(radius * 2.5)));
        var baseY = center.y - thickness * 0.5f;

        for (var i = 0; i < count; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0;
            var distance = Math.sqrt(random.nextDouble()) * radius;
            var x = center.x + Math.cos(angle) * distance;
            var z = center.z + Math.sin(angle) * distance;
            var y = baseY - random.nextDouble() * 0.2;
            var dx = (random.nextDouble() - 0.5) * 0.01;
            var dy = -0.15 - random.nextDouble() * 0.05;
            var dz = (random.nextDouble() - 0.5) * 0.01;
            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, dx, dy, dz);
        }
    }

    private void tryGrowPlant(ServerLevel level) {
        var target = findFirstBonemealableBlock(level);
        if (target == null) {
            return;
        }

        var state = level.getBlockState(target);
        if (state.isRandomlyTicking()) {
            state.randomTick(level, target, level.getRandom());
        }
    }

    @Nullable
    private BlockPos findFirstBonemealableBlock(ServerLevel level) {
        var basePos = anchorBlockPos;
        if (basePos == null) {
            return null;
        }

        var x = basePos.getX();
        var z = basePos.getZ();
        var yStart = Mth.floor(position().y);
        var yMin = level.getMinBuildHeight();
        var cursor = new BlockPos.MutableBlockPos(x, yStart, z);

        for (var y = yStart; y >= yMin; y--) {
            cursor.setY(y);
            var state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }

            if (state.getFluidState().is(FluidTags.WATER)) {
                return null;
            }

            var block = state.getBlock();
            if (block == Blocks.NETHER_WART || block == Blocks.SUGAR_CANE) {
                return cursor.immutable();
            }

            if (block instanceof BonemealableBlock bonemealable
                    && bonemealable.isValidBonemealTarget(level, cursor, state, false)) {
                return cursor.immutable();
            }

            return null;
        }

        return null;
    }

    @Nullable
    private Vec3 resolveTargetPosition(Level level) {
        var target = getFollowTarget(level);
        if (target != null && !target.isRemoved()) {
            var pos = toCloudPosition(RaycastTools.getEntityTargetPosition(target));
            anchorPosition = pos;
            return pos;
        }

        return anchorPosition;
    }

    @Nullable
    private Entity getFollowTarget(Level level) {
        if (cachedFollowTarget != null && !cachedFollowTarget.isRemoved()) {
            return cachedFollowTarget;
        }

        if (followTargetUuid != null && level instanceof ServerLevel server) {
            cachedFollowTarget = server.getEntity(followTargetUuid);
            return cachedFollowTarget;
        }

        return null;
    }

    public static Vec3 toCloudPosition(Vec3 basePosition) {
        return basePosition.add(0.0, HEIGHT_OFFSET, 0.0);
    }

    @Override
    public Vec3 getStandbyPosition() {
        return anchorPosition != null ? anchorPosition : position();
    }

    public void setCloudRadius(float radius) {
        entityData.set(CLOUD_RADIUS, Math.max(0.1f, radius));
    }

    public void setCloudThickness(float thickness) {
        entityData.set(CLOUD_THICKNESS, Math.max(0.1f, thickness));
    }

    public float getCloudRadius() {
        return entityData.get(CLOUD_RADIUS);
    }

    public float getCloudThickness() {
        return entityData.get(CLOUD_THICKNESS);
    }

    public void setGrowthIntervalTicks(int ticks) {
        growthIntervalTicks = Math.max(1, ticks);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("FollowTarget")) {
            followTargetUuid = tag.getUUID("FollowTarget");
            cachedFollowTarget = null;
        }

        if (tag.contains("AnchorX")) {
            var x = tag.getDouble("AnchorX");
            var y = tag.getDouble("AnchorY");
            var z = tag.getDouble("AnchorZ");
            anchorPosition = new Vec3(x, y, z);
            setPos(x, y, z);
        }

        if (tag.contains("AnchorBlock")) {
            anchorBlockPos = BlockPos.of(tag.getLong("AnchorBlock"));
            if (anchorPosition == null) {
                anchorPosition = toCloudPosition(Vec3.atCenterOf(anchorBlockPos));
                setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
            }
        }

        if (tag.contains("CloudRadius")) {
            setCloudRadius(tag.getFloat("CloudRadius"));
        }

        if (tag.contains("CloudThickness")) {
            setCloudThickness(tag.getFloat("CloudThickness"));
        }

        if (tag.contains("GrowthInterval")) {
            setGrowthIntervalTicks(tag.getInt("GrowthInterval"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (followTargetUuid != null) {
            tag.putUUID("FollowTarget", followTargetUuid);
        }

        if (anchorPosition != null) {
            tag.putDouble("AnchorX", anchorPosition.x);
            tag.putDouble("AnchorY", anchorPosition.y);
            tag.putDouble("AnchorZ", anchorPosition.z);
        }

        if (anchorBlockPos != null) {
            tag.putLong("AnchorBlock", anchorBlockPos.asLong());
        }

        tag.putFloat("CloudRadius", getCloudRadius());
        tag.putFloat("CloudThickness", getCloudThickness());
        tag.putInt("GrowthInterval", growthIntervalTicks);
    }
}
