package jp.aquafactory.apprenticecodex.spell.gracedrain;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class GracedRainCloudEntity extends SummonWeaponEntity {

    public static final double HEIGHT_OFFSET = 4.0;
    private static final int BLOCK_ANCHOR_HEIGHT_LIMIT = (int) HEIGHT_OFFSET;
    private static final float CLOUD_THICKNESS = 0.8f;
    private static final float VISUAL_OVERFLOW_BLOCKS = 0.35f;
    private static final int FOLLOW_EFFECT_INTERVAL_TICKS = 10;
    private static final int SOUND_INTERVAL_TICKS = 55;
    private static final double CRAFTSMANS_DELIGHT_AGE_REDUCTION_RATIO = 0.10D;

    private static final EntityDataAccessor<Integer> EFFECT_RADIUS_BLOCKS =
            SynchedEntityData.defineId(GracedRainCloudEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID followTargetUuid;
    private @Nullable Entity cachedFollowTarget;
    private @Nullable Vec3 anchorPosition;
    private @Nullable BlockPos anchorBlockPos;
    private float healAmount;
    private int growthIntervalTicks;
    private int growthTick;
    private int entityEffectTick;
    private int soundTick;

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
        entityData.define(EFFECT_RADIUS_BLOCKS, 1);
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

    public void setAnchorBlock(Level level, BlockPos blockPos) {
        anchorBlockPos = blockPos.immutable();
        followTargetUuid = null;
        cachedFollowTarget = null;
        anchorPosition = findBlockAnchorCloudPosition(level, blockPos)
                .orElseGet(() -> toCloudPosition(Vec3.atCenterOf(blockPos)));
        setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
    }

    @Override
    public void tick() {
        // todo:実行順の変化で見え方が変わるかもなので微調整.
        var level = level();
        if (level.isClientSide) {
            spawnCloudParticles(level);
            spawnRainParticles(level);
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity)) {
            discard();
            return;
        }

        // 音はこだわらずに一定周期でフェードイン・アウトが入ったものを鳴らすだけ.
        --soundTick;
        if (soundTick <= 0) {
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.CLOUD_RAIN.get(), SoundSource.PLAYERS, 1.0f, 0.9f, 0.1f);
            soundTick = SOUND_INTERVAL_TICKS;
        }

        var targetPos = resolveTargetPosition(level);
        if (targetPos != null) {
            followTargetPosition(targetPos);
        }

        if (anchorBlockPos != null) {
            ++growthTick;
            if (growthTick >= Math.max(1, growthIntervalTicks)) {
                growthTick = 0;
                tryGrowPlant(level);
            }
        }

        ++entityEffectTick;
        if (entityEffectTick >= FOLLOW_EFFECT_INTERVAL_TICKS) {
            entityEffectTick = 0;
            applyFollowEffect(level);
        }
    }

    private void spawnCloudParticles(Level level) {
        var random = level.getRandom();
        var center = position();
        var halfExtent = getVisualHalfExtentBlocks();
        var sideBlocks = getEffectRadiusBlocks() * 2 - 1;
        var count = Mth.clamp(sideBlocks * sideBlocks, 4, 48);
        var speed = 0.01;

        for (var i = 0; i < count; i++) {
            var x = center.x + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var z = center.z + (random.nextDouble() * 2.0 - 1.0) * halfExtent;
            var y = center.y + (random.nextDouble() - 0.5) * CLOUD_THICKNESS;
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
        var sideBlocks = getEffectRadiusBlocks() * 2 - 1;
        var count = Mth.clamp(sideBlocks * 2, 2, 16);
        var baseY = center.y - CLOUD_THICKNESS * 0.5f;

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

            if (state.is(TagRegistry.Blocks.CAN_RECEIVE_GRACED_RAIN)){
                return cursor.immutable();
            }

            var block = state.getBlock();
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

    // 天井下でも雨が届くよう、4ブロック上を上限にして直下の空気ブロックへ雲を収める。
    public static Optional<Vec3> findBlockAnchorCloudPosition(Level level, BlockPos blockPos) {
        var cloudBlockPos = blockPos.above();
        if (!level.getBlockState(cloudBlockPos).isAir()) {
            return Optional.empty();
        }

        for (var offset = 2; offset <= BLOCK_ANCHOR_HEIGHT_LIMIT; offset++) {
            var candidate = blockPos.above(offset);
            if (!level.getBlockState(candidate).isAir()) {
                break;
            }
            cloudBlockPos = candidate;
        }

        return Optional.of(Vec3.atCenterOf(cloudBlockPos));
    }

    @Override
    public Vec3 getStandbyPosition() {
        return anchorPosition != null ? anchorPosition : position();
    }

    public void setHealAmount(float healAmount) {
        this.healAmount = healAmount;
    }

    public void setEffectRadiusBlocks(int radiusBlocks) {
        entityData.set(EFFECT_RADIUS_BLOCKS, Math.max(1, radiusBlocks));
    }

    public int getEffectRadiusBlocks() {
        return entityData.get(EFFECT_RADIUS_BLOCKS);
    }

    private float getEffectHalfExtentBlocks() {
        return Math.max(0.5f, getEffectRadiusBlocks() - 0.5f);
    }

    private float getVisualHalfExtentBlocks() {
        return getEffectHalfExtentBlocks() + VISUAL_OVERFLOW_BLOCKS;
    }

    private void applyFollowEffect(Level level) {
        var center = position();
        var halfExtent = getEffectHalfExtentBlocks();
        var box = new AABB(
                center.x - halfExtent, center.y - HEIGHT_OFFSET, center.z - halfExtent,
                center.x + halfExtent, center.y, center.z + halfExtent
        );
        var source = createCombatDamageSource(DamageTypes.GRACED_RAIN);
        var school = SpellRegistry.GRACED_RAIN.get().getSchoolType();
        var targets = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        var owner = getOwner();
        var ownerLiving = owner instanceof LivingEntity living ? living : null;
        var craftsmansDelightAgeEffectEnabled = CraftsmansDelight.isEquippedBy(ownerLiving);

        for (var target : targets) {
            applyHealingEffect(target, source, school, craftsmansDelightAgeEffectEnabled);
        }

        if (ownerLiving != null && ownerLiving.isAlive()
                && box.intersects(ownerLiving.getBoundingBox())
                && !targets.contains(ownerLiving)) {
            applyHealingEffect(ownerLiving, source, school, craftsmansDelightAgeEffectEnabled);
        }
    }

    private void applyHealingEffect(
            LivingEntity target,
            DamageSource source,
            SchoolType school,
            boolean craftsmansDelightAgeEffectEnabled
    ) {
        if (target.isInvertedHealAndHarm()) {
            CombatTools.applyDamage(target, healAmount, source, school, CombatTools.KnockbackTypes.NO_KNOCKBACK);
        } else {
            target.heal(healAmount);
            if (craftsmansDelightAgeEffectEnabled) {
                applyCraftsmansDelightAgeEffect(target);
            }
        }
    }

    private void applyCraftsmansDelightAgeEffect(LivingEntity target) {
        if (target.getType() == EntityType.ALLAY || !(target instanceof AgeableMob ageable)) {
            return;
        }

        var age = ageable.getAge();
        if (age < 0 && !isCraftsmansDelightGracedRainGrowthDenied(ageable)) {
            ageable.setAge(Math.min(0, age + getCraftsmansDelightAgeReductionTicks(age)));
        } else if (age > 0 && !isCraftsmansDelightGracedRainBreedingCooldownDenied(ageable)) {
            ageable.setAge(Math.max(0, age - getCraftsmansDelightAgeReductionTicks(age)));
        }
    }

    private int getCraftsmansDelightAgeReductionTicks(int age) {
        return Math.max(1, Mth.ceil(Math.abs(age) * CRAFTSMANS_DELIGHT_AGE_REDUCTION_RATIO));
    }

    private boolean isCraftsmansDelightGracedRainGrowthDenied(AgeableMob target) {
        return ApprenticeCodexServerConfig.isCraftsmansDelightGracedRainGrowthDenied(
                ForgeRegistries.ENTITY_TYPES.getKey(target.getType())
        );
    }

    private boolean isCraftsmansDelightGracedRainBreedingCooldownDenied(AgeableMob target) {
        return ApprenticeCodexServerConfig.isCraftsmansDelightGracedRainBreedingCooldownDenied(
                ForgeRegistries.ENTITY_TYPES.getKey(target.getType())
        );
    }

    public void setGrowthIntervalTicks(int ticks) {
        growthIntervalTicks = Math.max(1, ticks);
    }
}
