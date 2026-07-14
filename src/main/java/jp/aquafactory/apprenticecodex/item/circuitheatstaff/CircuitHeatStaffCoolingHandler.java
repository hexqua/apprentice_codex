package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CircuitHeatStaffCoolingHandler {
    private static final String ROOT_TAG = "ApprenticeCodexCircuitHeatStaffCooling";
    private static final String TARGET_KEY_TAG = "TargetKey";
    private static final String PROCESS_COUNT_TAG = "ProcessCount";

    private CircuitHeatStaffCoolingHandler() {
    }

    public static void onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity) {
        entity.setUnlimitedLifetime();
        if (entity.level().isClientSide || !ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingEnabled()) {
            return;
        }

        var processIntervalTicks = ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingProcessIntervalTicks();
        if (entity.tickCount % processIntervalTicks != 0) {
            return;
        }

        var level = entity.level();
        if (!CircuitHeatStaff.isStaffOverheated(stack, level)) {
            clearCoolingData(entity);
            return;
        }

        var target = findCoolingTarget(level, entity);
        if (target == null) {
            clearCoolingData(entity);
            return;
        }

        var reductionTicks = ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingReductionTicks();
        if (reductionTicks <= 0) {
            clearCoolingData(entity);
            return;
        }

        CircuitHeatStaff.reduceStaffOverheatTicks(stack, level, reductionTicks);
        entity.setItem(stack);
        playCoolingEffects(level, entity, target);

        if (!isConsumableByConfig(target.type())) {
            clearCoolingData(entity);
            return;
        }

        var processCount = updateProcessCount(entity, target);
        if (processCount >= ApprenticeCodexServerConfig.circuitHeatStaffDropCoolingWaterConsumeProcessCount()) {
            consumeCoolingTarget(level, target);
            clearCoolingData(entity);
        }
    }

    @Nullable
    private static CoolingTarget findCoolingTarget(@NotNull Level level, @NotNull ItemEntity entity) {
        var itemPos = entity.blockPosition();
        var blockTarget = findBlockTarget(level, itemPos);
        if (blockTarget != null) {
            return blockTarget;
        }

        blockTarget = findBlockTarget(level, itemPos.below());
        if (blockTarget != null) {
            return blockTarget;
        }

        return findCauldronTarget(level, itemPos, entity);
    }

    @Nullable
    private static CoolingTarget findBlockTarget(@NotNull Level level, @NotNull BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
            return new CoolingTarget(pos.immutable(), CoolingTargetType.WATER_SOURCE);
        }
        if (state.is(Blocks.POWDER_SNOW)) {
            return new CoolingTarget(pos.immutable(), CoolingTargetType.POWDER_SNOW_BLOCK);
        }

        return null;
    }

    @Nullable
    private static CoolingTarget findCauldronTarget(@NotNull Level level, @NotNull BlockPos pos, @NotNull ItemEntity entity) {
        var state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON) && isEntityInsideLayeredCauldronContent(state, pos, entity)) {
            return new CoolingTarget(pos.immutable(), CoolingTargetType.WATER_CAULDRON);
        }
        if (state.is(Blocks.POWDER_SNOW_CAULDRON) && isEntityInsideLayeredCauldronContent(state, pos, entity)) {
            return new CoolingTarget(pos.immutable(), CoolingTargetType.POWDER_SNOW_CAULDRON);
        }

        return null;
    }

    private static boolean isEntityInsideLayeredCauldronContent(@NotNull BlockState state, @NotNull BlockPos pos,
                                                                @NotNull ItemEntity entity) {
        var contentHeight = (6.0D + state.getValue(LayeredCauldronBlock.LEVEL) * 3.0D) / 16.0D;
        return entity.getY() < pos.getY() + contentHeight && entity.getBoundingBox().maxY > pos.getY() + 0.25D;
    }

    private static void playCoolingEffects(@NotNull Level level, @NotNull ItemEntity entity, @NotNull CoolingTarget target) {
        if (target.type().isPowderSnow()) {
            playPowderSnowCoolingEffects(level, entity, target.pos());
            return;
        }

        playWaterCoolingEffects(level, entity, target.pos());
    }

    private static void playWaterCoolingEffects(@NotNull Level level, @NotNull ItemEntity entity, @NotNull BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
        );
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    entity.getX(),
                    entity.getY() + 0.25D,
                    entity.getZ(),
                    8,
                    0.25D,
                    0.15D,
                    0.25D,
                    0.0D
            );
        }
    }

    private static void playPowderSnowCoolingEffects(@NotNull Level level, @NotNull ItemEntity entity, @NotNull BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.POWDER_SNOW_HIT,
                SoundSource.BLOCKS,
                0.4F,
                1.5F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F
        );
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    entity.getX(),
                    entity.getY() + 0.25D,
                    entity.getZ(),
                    8,
                    0.25D,
                    0.15D,
                    0.25D,
                    0.02D
            );
        }
    }

    private static int updateProcessCount(@NotNull ItemEntity entity, @NotNull CoolingTarget target) {
        var tag = getCoolingData(entity);
        var targetKey = target.key();
        var processCount = targetKey.equals(tag.getString(TARGET_KEY_TAG))
                ? tag.getInt(PROCESS_COUNT_TAG) + 1
                : 1;

        tag.putString(TARGET_KEY_TAG, targetKey);
        tag.putInt(PROCESS_COUNT_TAG, processCount);
        return processCount;
    }

    private static CompoundTag getCoolingData(@NotNull ItemEntity entity) {
        var persistentData = entity.getPersistentData();
        if (!persistentData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistentData.put(ROOT_TAG, new CompoundTag());
        }

        return persistentData.getCompound(ROOT_TAG);
    }

    private static void clearCoolingData(@NotNull ItemEntity entity) {
        entity.getPersistentData().remove(ROOT_TAG);
    }

    private static void consumeCoolingTarget(@NotNull Level level, @NotNull CoolingTarget target) {
        var state = level.getBlockState(target.pos());
        if (target.type() == CoolingTargetType.WATER_SOURCE) {
            if (ApprenticeCodexServerConfig.circuitHeatStaffConsumeWaterSourceOnCooling()
                    && state.is(Blocks.WATER)
                    && state.getFluidState().isSource()) {
                level.setBlockAndUpdate(target.pos(), Blocks.AIR.defaultBlockState());
            }
            return;
        }

        if (ApprenticeCodexServerConfig.circuitHeatStaffConsumeWaterCauldronOnCooling()
                && state.is(Blocks.WATER_CAULDRON)) {
            LayeredCauldronBlock.lowerFillLevel(state, level, target.pos());
        }
    }

    private static boolean isConsumableByConfig(CoolingTargetType type) {
        return switch (type) {
            case WATER_SOURCE -> ApprenticeCodexServerConfig.circuitHeatStaffConsumeWaterSourceOnCooling();
            case WATER_CAULDRON -> ApprenticeCodexServerConfig.circuitHeatStaffConsumeWaterCauldronOnCooling();
            case POWDER_SNOW_BLOCK, POWDER_SNOW_CAULDRON -> false;
        };
    }

    private enum CoolingTargetType {
        WATER_SOURCE(false),
        WATER_CAULDRON(false),
        POWDER_SNOW_BLOCK(true),
        POWDER_SNOW_CAULDRON(true);

        private final boolean powderSnow;

        CoolingTargetType(boolean powderSnow) {
            this.powderSnow = powderSnow;
        }

        private boolean isPowderSnow() {
            return powderSnow;
        }
    }

    private record CoolingTarget(BlockPos pos, CoolingTargetType type) {
        private String key() {
            return type.name() + ":" + pos.asLong();
        }
    }
}
