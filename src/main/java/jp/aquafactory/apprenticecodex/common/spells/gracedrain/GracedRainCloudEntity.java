package jp.aquafactory.apprenticecodex.common.spells.gracedrain;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
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
    private static final int DEFAULT_EFFECT_RADIUS_BLOCKS = 3;
    private static final float DEFAULT_THICKNESS = 0.8f;
    private static final float VISUAL_OVERFLOW_BLOCKS = 0.35f;
    private static final int FOLLOW_EFFECT_INTERVAL_TICKS = 20;

    private static final EntityDataAccessor<Integer> EFFECT_RADIUS_BLOCKS =
            SynchedEntityData.defineId(GracedRainCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CLOUD_THICKNESS =
            SynchedEntityData.defineId(GracedRainCloudEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable UUID followTargetUuid;
    private @Nullable Entity cachedFollowTarget;
    private @Nullable Vec3 anchorPosition;
    private @Nullable BlockPos anchorBlockPos;
    private int growthIntervalTicks = 40;
    private int growthTick;
    private int followEffectTick;

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
        entityData.define(EFFECT_RADIUS_BLOCKS, DEFAULT_EFFECT_RADIUS_BLOCKS);
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

        var followTarget = getFollowTarget(level);
        if (followTarget instanceof LivingEntity livingTarget) {
            if (++followEffectTick >= FOLLOW_EFFECT_INTERVAL_TICKS) {
                followEffectTick = 0;
                // todo:数値は後で整理して渡す.
                if (livingTarget.isInvertedHealAndHarm()) {
                    var source = CombatTools.getDamageSource(level(), this, getOwner(), "graced_rain");
                    CombatTools.applyDamage(livingTarget, 1, source, SpellsRegistry.GRACED_RAIN.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                } else {
                    livingTarget.heal(1.0f);
                }
            }
        } else {
            followEffectTick = 0;
        }
    }

    private void spawnCloudParticles(Level level) {
        var random = level.getRandom();
        var center = position();
        var halfExtent = getVisualHalfExtentBlocks();
        var thickness = Math.max(0.1f, getCloudThickness());
        var sideBlocks = getEffectRadiusBlocks() * 2 - 1;
        var count = Mth.clamp(sideBlocks * sideBlocks, 4, 48);
        var speed = 0.01;

        for (var i = 0; i < count; i++) {
            var x = center.x + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var z = center.z + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var y = center.y + (random.nextDouble() - 0.5) * thickness;
            var dx = (random.nextDouble() - 0.5) * speed;
            var dy = (random.nextDouble() - 0.5) * speed * 0.2;
            var dz = (random.nextDouble() - 0.5) * speed;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, dx, dy, dz);
        }
    }

    private void spawnRainParticles(Level level) {
        if ((tickCount & 1) != 0) {
            return;
        }

        var random = level.getRandom();
        var center = position();
        var halfExtent = getVisualHalfExtentBlocks();
        var thickness = Math.max(0.1f, getCloudThickness());
        var sideBlocks = getEffectRadiusBlocks() * 2 - 1;
        var count = Mth.clamp(sideBlocks * 2, 2, 16);
        var baseY = center.y - thickness * 0.5f;

        for (var i = 0; i < count; i++) {
            var x = center.x + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var z = center.z + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var y = baseY - random.nextDouble() * 0.2;
            var dx = (random.nextDouble() - 0.5) * 0.01;
            var dy = -0.15 - random.nextDouble() * 0.05;
            var dz = (random.nextDouble() - 0.5) * 0.01;
            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, dx, dy, dz);
        }
    }

    private void tryGrowPlant(ServerLevel level) {
        var basePos = anchorBlockPos;
        if (basePos == null) {
            return;
        }

        var maxOffset = Math.max(0, getEffectRadiusBlocks() - 1);
        var baseX = basePos.getX();
        var baseZ = basePos.getZ();
        var yStart = Mth.floor(position().y);
        var yMin = level.getMinBuildHeight();
        var cursor = new BlockPos.MutableBlockPos();
        var random = level.getRandom();

        for (int dx = -maxOffset; dx <= maxOffset; dx++) {
            for (int dz = -maxOffset; dz <= maxOffset; dz++) {
                var target = findFirstBonemealableBlock(level, baseX + dx, baseZ + dz, yStart, yMin, cursor);
                if (target == null) {
                    continue;
                }

                var state = level.getBlockState(target);
                if (state.isRandomlyTicking()) {
                    state.randomTick(level, target, random);
                }
            }
        }
    }

    @Nullable
    private BlockPos findFirstBonemealableBlock(ServerLevel level, int x, int z, int yStart, int yMin, BlockPos.MutableBlockPos cursor) {
        for (var y = yStart; y >= yMin; y--) {
            cursor.set(x, y, z);
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

    public void setCloudThickness(float thickness) {
        entityData.set(CLOUD_THICKNESS, Math.max(0.1f, thickness));
    }

    public void setEffectRadiusBlocks(int radiusBlocks) {
        entityData.set(EFFECT_RADIUS_BLOCKS, Math.max(1, radiusBlocks));
    }

    public int getEffectRadiusBlocks() {
        return entityData.get(EFFECT_RADIUS_BLOCKS);
    }

    public float getCloudThickness() {
        return entityData.get(CLOUD_THICKNESS);
    }

    private float getEffectHalfExtentBlocks() {
        return Math.max(0.5f, getEffectRadiusBlocks() - 0.5f);
    }

    private float getVisualHalfExtentBlocks() {
        return getEffectHalfExtentBlocks() + VISUAL_OVERFLOW_BLOCKS;
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

        if (tag.contains("EffectRadiusBlocks")) {
            setEffectRadiusBlocks(tag.getInt("EffectRadiusBlocks"));
        } else if (tag.contains("CloudRadius")) {
            var legacyRadius = tag.getFloat("CloudRadius");
            setEffectRadiusBlocks(Math.max(1, Math.round(legacyRadius + 0.5f)));
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

        tag.putInt("EffectRadiusBlocks", getEffectRadiusBlocks());
        tag.putFloat("CloudThickness", getCloudThickness());
        tag.putInt("GrowthInterval", growthIntervalTicks);
    }
}
