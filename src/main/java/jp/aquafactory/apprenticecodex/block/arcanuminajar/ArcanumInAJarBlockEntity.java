package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    public static final int MAX_STORED_PARAMETER = 8;
    private static final String STORED_PARAMETER_COUNT_TAG = "StoredParameterCount";
    private static final String REMAINING_OPERATION_COUNT_TAG = "RemainingOperationCount";
    private static final String PROGRESS_START_GAME_TIME_TAG = "ProgressStartGameTime";
    private static final String DISPENSING_TAG = "Dispensing";
    private static final String NEXT_RELEASE_GAME_TIME_TAG = "NextReleaseGameTime";
    private static final String LEGACY_PLACED_GAME_TIME_TAG = "PlacedGameTime";
    private static final int INITIAL_RELEASE_DELAY_TICKS = 10;


    private int storedParameterCount;
    private int remainingOperationCount;
    private long progressStartGameTime = -1L;
    private boolean dispensing;
    private long nextReleaseGameTime = -1L;
    private long legacyPlacedGameTime = -1L;

    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }

    public int getStoredProductCount() {
        return storedParameterCount;
    }

    public float getStoredProductRatio() {
        return getStoredProductCount() / (float)MAX_STORED_PARAMETER;
    }

    public int getRemainingMaterialCount() {
        return remainingOperationCount;
    }

    public float getRemainingMaterialRatio() {
        return remainingOperationCount / (float)MAX_STORED_PARAMETER;
    }

    public boolean hasNoWorkLoaded() {
        return storedParameterCount <= 0 && remainingOperationCount <= 0 && progressStartGameTime < 0L;
    }

    public boolean isDispensing() {
        return dispensing;
    }

    public boolean canAcceptMoreMaterial() {
        return remainingOperationCount < MAX_STORED_PARAMETER;
    }

    public long getRemainingTicksUntilNextConversion() {
        if (level == null || !shouldProcess() || progressStartGameTime < 0L) {
            return -1L;
        }

        var ticksPerParameter = ticksPerStoredParameter();
        var elapsedTicks = Math.max(0L, level.getGameTime() - progressStartGameTime);
        var progressedTicks = elapsedTicks % ticksPerParameter;
        var remainingTicks = ticksPerParameter - progressedTicks;
        return remainingTicks > 0L ? remainingTicks : ticksPerParameter;
    }

    public int insertMaterial(long gameTime, int availableCount) {
        var capacity = MAX_STORED_PARAMETER - remainingOperationCount;
        if (capacity <= 0 || availableCount <= 0) {
            return 0;
        }

        var inserted = 1;

        remainingOperationCount += inserted;
        if (shouldProcess() && progressStartGameTime < 0L) {
            progressStartGameTime = gameTime;
        }

        setChanged();
        syncToClient();
        return inserted;
    }

    public void startDispenseSequence() {
        if (!(level instanceof ServerLevel serverLevel) || dispensing) {
            return;
        }

        dispensing = true;
        nextReleaseGameTime = serverLevel.getGameTime() + INITIAL_RELEASE_DELAY_TICKS;
        setOpen(true);
        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundRegistry.VANILLA_JAR_OPEN.get(), SoundSource.BLOCKS);
        setChanged();
        syncToClient();
    }

    public void cancelDispenseSequence() {
        dispensing = false;
        nextReleaseGameTime = -1L;
        setOpen(false);
        setChanged();
        syncToClient();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            migrateLegacyState(level.getGameTime());
            sanitizePersistentGameTimes(level.getGameTime());
            if (dispensing && !itemSettings().isValid()) {
                cancelDispenseSequence();
            }
            if (shouldProcess() && progressStartGameTime < 0L) {
                progressStartGameTime = level.getGameTime();
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (storedParameterCount > 0) {
            tag.putInt(STORED_PARAMETER_COUNT_TAG, storedParameterCount);
        }
        if (remainingOperationCount > 0) {
            tag.putInt(REMAINING_OPERATION_COUNT_TAG, remainingOperationCount);
        }
        if (progressStartGameTime >= 0L) {
            tag.putLong(PROGRESS_START_GAME_TIME_TAG, progressStartGameTime);
        }
        tag.putBoolean(DISPENSING_TAG, dispensing);
        if (nextReleaseGameTime >= 0L) {
            tag.putLong(NEXT_RELEASE_GAME_TIME_TAG, nextReleaseGameTime);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        storedParameterCount = Mth.clamp(tag.getInt(STORED_PARAMETER_COUNT_TAG), 0, MAX_STORED_PARAMETER);
        remainingOperationCount = Mth.clamp(tag.getInt(REMAINING_OPERATION_COUNT_TAG), 0, MAX_STORED_PARAMETER);
        progressStartGameTime = tag.contains(PROGRESS_START_GAME_TIME_TAG) ? tag.getLong(PROGRESS_START_GAME_TIME_TAG) : -1L;
        dispensing = tag.getBoolean(DISPENSING_TAG);
        nextReleaseGameTime = tag.contains(NEXT_RELEASE_GAME_TIME_TAG)
                ? tag.getLong(NEXT_RELEASE_GAME_TIME_TAG)
                : -1L;
        legacyPlacedGameTime = tag.contains(LEGACY_PLACED_GAME_TIME_TAG) ? tag.getLong(LEGACY_PLACED_GAME_TIME_TAG) : -1L;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcanumInAJarBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        blockEntity.migrateLegacyState(serverLevel.getGameTime());
        blockEntity.sanitizePersistentGameTimes(serverLevel.getGameTime());
        blockEntity.updateProduction(serverLevel.getGameTime());

        if (blockEntity.dispensing && !itemSettings().isValid()) {
            blockEntity.cancelDispenseSequence();
            return;
        }

        if (!blockEntity.dispensing) {
            return;
        }

        if (blockEntity.storedParameterCount <= 0) {
            blockEntity.finishDispenseSequence();
            return;
        }

        if (serverLevel.getGameTime() < blockEntity.nextReleaseGameTime) {
            return;
        }

        var stored = blockEntity.storedParameterCount;
        if (!blockEntity.spawnProducts(serverLevel, stored)) {
            return;
        }

        blockEntity.consumeStoredParameters(serverLevel.getGameTime(), stored);
        blockEntity.finishDispenseSequence();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private int consumeStoredParameters(long gameTime, int count) {
        if (storedParameterCount <= 0) {
            return 0;
        }

        var consumed = Math.min(count, storedParameterCount);
        storedParameterCount -= consumed;
        if (shouldProcess() && progressStartGameTime < 0L) {
            progressStartGameTime = gameTime;
        }

        setChanged();
        updateComparatorOutput();
        syncToClient();
        return storedParameterCount;
    }

    private void finishDispenseSequence() {
        if (level == null) {
            return;
        }

        dispensing = false;
        nextReleaseGameTime = -1L;
        setOpen(false);
        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundRegistry.VANILLA_JAR_CLOSE.get(), SoundSource.BLOCKS);
        setChanged();
        syncToClient();
    }

    private void sanitizePersistentGameTimes(long gameTime) {
        var changed = false;
        if (progressStartGameTime >= 0L) {
            var sanitizedProgressStartGameTime = PersistentGameTimeSanitizer.clampFutureStart(gameTime, progressStartGameTime);
            if (sanitizedProgressStartGameTime != progressStartGameTime) {
                progressStartGameTime = sanitizedProgressStartGameTime;
                changed = true;
            }
        }

        if (nextReleaseGameTime >= 0L) {
            var sanitizedNextReleaseGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntil(
                    gameTime,
                    nextReleaseGameTime,
                    INITIAL_RELEASE_DELAY_TICKS
            );
            if (sanitizedNextReleaseGameTime != nextReleaseGameTime) {
                nextReleaseGameTime = sanitizedNextReleaseGameTime;
                changed = true;
            }
        }

        if (changed) {
            setChanged();
            syncToClient();
        }
    }

    private boolean spawnProducts(ServerLevel serverLevel, int count) {
        var productItem = itemSettings().productItem();
        if (productItem == null || count <= 0) {
            return false;
        }

        var spawnPos = Vec3.atCenterOf(worldPosition.above());
        for (var i = 0; i < count; i++) {
            var spawned = new ItemEntity(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z, new ItemStack(productItem));
            spawned.setDeltaMovement(Vec3.ZERO);
            serverLevel.addFreshEntity(spawned);
        }
        AudioTools.playSoundFromPosition(serverLevel, spawnPos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.9f, 1.15f, 0.0f);
        return true;
    }

    public void appendRemovalDrops(List<ItemStack> drops) {
        var settings = itemSettings();
        if (!settings.isValid()) {
            return;
        }

        var counts = getRemovalDropCounts();
        appendSingleItemDrops(drops, settings.productItem(), counts.storedParameterCount);
        appendSingleItemDrops(drops, settings.materialItem(), counts.remainingOperationCount);
    }

    private static void appendSingleItemDrops(List<ItemStack> drops, Item item, int count) {
        for (var i = 0; i < count; i++) {
            drops.add(new ItemStack(item));
        }
    }

    private void setOpen(boolean open) {
        if (level == null) {
            return;
        }

        var state = getBlockState();
        if (!state.hasProperty(ArcanumInAJar.OPEN) || state.getValue(ArcanumInAJar.OPEN) == open) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(ArcanumInAJar.OPEN, open), 3);
    }

    private void updateProduction(long gameTime) {
        if (!shouldProcess()) {
            if (progressStartGameTime >= 0L) {
                progressStartGameTime = -1L;
                setChanged();
                syncToClient();
            }
            return;
        }

        if (progressStartGameTime < 0L) {
            progressStartGameTime = gameTime;
            setChanged();
            syncToClient();
            return;
        }

        var completed = (int)((gameTime - progressStartGameTime) / ticksPerStoredParameter());
        if (completed <= 0) {
            return;
        }

        completed = Math.min(completed, remainingOperationCount);
        completed = Math.min(completed, MAX_STORED_PARAMETER - storedParameterCount);
        if (completed <= 0) {
            return;
        }

        storedParameterCount += completed;
        remainingOperationCount -= completed;
        if (shouldProcess()) {
            progressStartGameTime += ticksPerStoredParameter() * completed;
        } else {
            progressStartGameTime = -1L;
        }

        setChanged();
        updateComparatorOutput();
        syncToClient();
    }

    private void migrateLegacyState(long gameTime) {
        if (legacyPlacedGameTime < 0L) {
            return;
        }

        // 旧版は経過時間のみで在庫を表現していたため、更新時は完成済み在庫だけを救済する.
        storedParameterCount = Mth.clamp((int)((gameTime - legacyPlacedGameTime) / ticksPerStoredParameter()), 0, MAX_STORED_PARAMETER);
        remainingOperationCount = 0;
        progressStartGameTime = -1L;
        legacyPlacedGameTime = -1L;
        setChanged();
        updateComparatorOutput();
        syncToClient();
    }

    private void updateComparatorOutput() {
        if (level == null || level.isClientSide) {
            return;
        }

        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private boolean shouldProcess() {
        return remainingOperationCount > 0 && storedParameterCount < MAX_STORED_PARAMETER;
    }

    private RemovalDropCounts getRemovalDropCounts() {
        var effectiveStoredParameterCount = storedParameterCount;
        var effectiveRemainingOperationCount = remainingOperationCount;
        if (progressStartGameTime >= 0L && level != null) {
            var completed = (int)((level.getGameTime() - progressStartGameTime) / ticksPerStoredParameter());
            if (completed > 0) {
                completed = Math.min(completed, effectiveRemainingOperationCount);
                completed = Math.min(completed, MAX_STORED_PARAMETER - effectiveStoredParameterCount);
                effectiveStoredParameterCount += completed;
                effectiveRemainingOperationCount -= completed;
            }
        }

        return new RemovalDropCounts(effectiveStoredParameterCount, effectiveRemainingOperationCount);
    }

    private static jp.aquafactory.apprenticecodex.config.block.ArcanumInAJarServerConfig.ItemSettings itemSettings() {
        return ApprenticeCodexServerConfig.arcanumInAJarItemSettings();
    }

    private static long ticksPerStoredParameter() {
        return ApprenticeCodexServerConfig.arcanumInAJarTicksPerStoredParameter();
    }

    private record RemovalDropCounts(int storedParameterCount, int remainingOperationCount) {
    }
}
