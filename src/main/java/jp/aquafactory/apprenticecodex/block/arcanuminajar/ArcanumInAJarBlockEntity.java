package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    public static final int MAX_STORED_PARAMETER = 8;
    private static final String STORED_PARAMETER_COUNT_TAG = "StoredParameterCount";
    private static final String REMAINING_OPERATION_COUNT_TAG = "RemainingOperationCount";
    private static final String PROGRESS_START_GAME_TIME_TAG = "ProgressStartGameTime";
    private static final String DISPENSING_TAG = "Dispensing";
    private static final String NEXT_RELEASE_GAME_TIME_TAG = "NextReleaseGameTime";
    private static final String LEGACY_PLACED_GAME_TIME_TAG = "PlacedGameTime";
    private static final int INITIAL_RELEASE_DELAY_TICKS = 20;
    private static final int REPEAT_RELEASE_DELAY_TICKS = 10;
    private static final ResourceLocation ARCANE_ESSENCE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_essence");

    private int storedParameterCount;
    private int remainingOperationCount;
    private long progressStartGameTime = -1L;
    private boolean dispensing;
    private long nextReleaseGameTime = -1L;
    private long legacyPlacedGameTime = -1L;

    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }

    public int getStoredParameterCount() {
        return storedParameterCount;
    }

    public float getFillRatio() {
        return getStoredParameterCount() / (float)MAX_STORED_PARAMETER;
    }

    public int getRemainingOperationCount() {
        return remainingOperationCount;
    }

    public float getRemainingOperationRatio() {
        return remainingOperationCount / (float)MAX_STORED_PARAMETER;
    }

    public boolean hasNoWorkLoaded() {
        return storedParameterCount <= 0 && remainingOperationCount <= 0 && progressStartGameTime < 0L;
    }

    public void restoreState(int storedParameterCount, int remainingOperationCount, long gameTime) {
        var clampedStoredParameterCount = Mth.clamp(storedParameterCount, 0, MAX_STORED_PARAMETER);
        var clampedRemainingOperationCount = Mth.clamp(remainingOperationCount, 0, MAX_STORED_PARAMETER);
        this.storedParameterCount = clampedStoredParameterCount;
        this.remainingOperationCount = clampedRemainingOperationCount;
        progressStartGameTime = shouldProcess() ? gameTime : -1L;
        dispensing = false;
        nextReleaseGameTime = -1L;
        legacyPlacedGameTime = -1L;
        setChanged();
        syncToClient();
    }

    public static int getStoredParameterCount(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        var tag = customData.copyTag();
        if (!tag.contains(STORED_PARAMETER_COUNT_TAG, Tag.TAG_ANY_NUMERIC)) {
            return 0;
        }

        return Mth.clamp(tag.getInt(STORED_PARAMETER_COUNT_TAG), 0, MAX_STORED_PARAMETER);
    }

    public static void setStoredParameterCount(ItemStack stack, int storedParameterCount) {
        var clampedStoredParameterCount = Mth.clamp(storedParameterCount, 0, MAX_STORED_PARAMETER);
        if (clampedStoredParameterCount <= 0) {
            removeStackTag(stack, STORED_PARAMETER_COUNT_TAG);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(STORED_PARAMETER_COUNT_TAG, clampedStoredParameterCount));
    }

    public static int getRemainingOperationCount(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        var tag = customData.copyTag();
        if (!tag.contains(REMAINING_OPERATION_COUNT_TAG, Tag.TAG_ANY_NUMERIC)) {
            return 0;
        }

        return Mth.clamp(tag.getInt(REMAINING_OPERATION_COUNT_TAG), 0, MAX_STORED_PARAMETER);
    }

    public static void setRemainingOperationCount(ItemStack stack, int remainingOperationCount) {
        var clampedRemainingOperationCount = Mth.clamp(remainingOperationCount, 0, MAX_STORED_PARAMETER);
        if (clampedRemainingOperationCount <= 0) {
            removeStackTag(stack, REMAINING_OPERATION_COUNT_TAG);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(REMAINING_OPERATION_COUNT_TAG, clampedRemainingOperationCount));
    }

    public boolean isDispensing() {
        return dispensing;
    }

    public boolean canAcceptMoreRedstone() {
        return remainingOperationCount < MAX_STORED_PARAMETER;
    }

    public int insertRedstone(long gameTime, int availableCount) {
        var capacity = MAX_STORED_PARAMETER - remainingOperationCount;
        if (capacity <= 0 || availableCount <= 0) {
            return 0;
        }

        var inserted = Math.min(1, Math.min(capacity, availableCount));
        if (inserted <= 0) {
            return 0;
        }

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
        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.BARREL_OPEN, SoundSource.BLOCKS);
        setChanged();
        syncToClient();
    }

    public void skipDispenseSequence() {
        if (!(level instanceof ServerLevel serverLevel) || !dispensing) {
            return;
        }

        updateProduction(serverLevel.getGameTime());
        var stored = storedParameterCount;
        if (stored > 0 && !spawnArcaneEssence(serverLevel, stored)) {
            return;
        }

        consumeStoredParameters(serverLevel.getGameTime(), stored);
        finishDispenseSequence();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            migrateLegacyState(level.getGameTime());
            if (shouldProcess() && progressStartGameTime < 0L) {
                progressStartGameTime = level.getGameTime();
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
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
        blockEntity.updateProduction(serverLevel.getGameTime());

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

        if (!blockEntity.spawnArcaneEssence(serverLevel, 1)) {
            return;
        }

        var remaining = blockEntity.consumeStoredParameters(serverLevel.getGameTime(), 1);
        if (remaining > 0) {
            blockEntity.nextReleaseGameTime = serverLevel.getGameTime() + REPEAT_RELEASE_DELAY_TICKS;
            blockEntity.setChanged();
            blockEntity.syncToClient();
            return;
        }

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
        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS);
        setChanged();
        syncToClient();
    }

    private boolean spawnArcaneEssence(ServerLevel serverLevel, int count) {
        var arcaneEssence = BuiltInRegistries.ITEM.getOptional(ARCANE_ESSENCE_ITEM_ID).orElse(null);
        if (arcaneEssence == null) {
            ApprenticeCodex.LOGGER.warn("Missing item: {}", ARCANE_ESSENCE_ITEM_ID);
            return false;
        }

        var spawnPos = Vec3.atCenterOf(worldPosition.above());
        var spawned = new ItemEntity(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z, new ItemStack(arcaneEssence, count));
        spawned.setDeltaMovement(Vec3.ZERO);
        serverLevel.addFreshEntity(spawned);
        AudioTools.playSoundFromPosition(serverLevel, spawnPos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.9f, 1.15f, 0.0f);
        return true;
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
        syncToClient();
    }

    private boolean shouldProcess() {
        return remainingOperationCount > 0 && storedParameterCount < MAX_STORED_PARAMETER;
    }

    private static void removeStackTag(ItemStack stack, String tagKey) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(tagKey));
    }

    private static long ticksPerStoredParameter() {
        return ApprenticeCodexServerConfig.arcanumInAJarTicksPerStoredParameter();
    }
}
