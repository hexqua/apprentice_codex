package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    public static final int MAX_STORED_PARAMETER = 8;
    private static final long TICKS_PER_ITEM = 20L * 60L;
    private static final String PLACED_GAME_TIME_TAG = "PlacedGameTime";
    private static final String DISPENSING_TAG = "Dispensing";
    private static final String NEXT_RELEASE_GAME_TIME_TAG = "NextReleaseGameTime";
    private static final int INITIAL_RELEASE_DELAY_TICKS = 20;
    private static final int REPEAT_RELEASE_DELAY_TICKS = 10;
    private static final ResourceLocation ARCANE_ESSENCE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_essence");

    private long placedGameTime = -1L;
    private boolean dispensing;
    private long nextReleaseGameTime = -1L;

    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }

    public int getStoredParameterCount() {
        if (level == null || placedGameTime < 0L) {
            return 0;
        }

        return getStoredParameterCount(level.getGameTime());
    }

    public float getFillRatio() {
        return getStoredParameterCount() / (float)MAX_STORED_PARAMETER;
    }

    public void initializePlacedGameTime(long gameTime) {
        if (placedGameTime >= 0L) {
            return;
        }

        // tick加算ではなく設置時刻基準で扱い、距離外や再読込後も同じ蓄積量を復元する.
        placedGameTime = gameTime;
        setChanged();
        syncToClient();
    }

    public boolean isDispensing() {
        return dispensing;
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

        var stored = getStoredParameterCount();
        if (stored > 0 && !spawnArcaneEssence(serverLevel, stored)) {
            return;
        }

        consumeStoredParameters(serverLevel.getGameTime(), stored);
        finishDispenseSequence();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && placedGameTime < 0L) {
            initializePlacedGameTime(level.getGameTime());
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (placedGameTime >= 0L) {
            tag.putLong(PLACED_GAME_TIME_TAG, placedGameTime);
        }

        tag.putBoolean(DISPENSING_TAG, dispensing);
        if (nextReleaseGameTime >= 0L) {
            tag.putLong(NEXT_RELEASE_GAME_TIME_TAG, nextReleaseGameTime);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        placedGameTime = tag.contains(PLACED_GAME_TIME_TAG) ? tag.getLong(PLACED_GAME_TIME_TAG) : -1L;
        dispensing = tag.getBoolean(DISPENSING_TAG);
        nextReleaseGameTime = tag.contains(NEXT_RELEASE_GAME_TIME_TAG)
                ? tag.getLong(NEXT_RELEASE_GAME_TIME_TAG)
                : -1L;
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
        if (level.isClientSide || !blockEntity.dispensing || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (blockEntity.getStoredParameterCount() <= 0) {
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
        var elapsedTicks = getCappedElapsedTicks(gameTime);
        var stored = getStoredParameterCount(gameTime);
        if (stored <= 0) {
            return 0;
        }

        var consumed = Math.min(count, stored);
        var remainingElapsedTicks = Math.max(0L, elapsedTicks - (TICKS_PER_ITEM * consumed));
        // 蓄積の上限到達後に放置した超過分はここで捨てる.
        placedGameTime = gameTime - remainingElapsedTicks;

        setChanged();
        syncToClient();
        return (int)(remainingElapsedTicks / TICKS_PER_ITEM);
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
        var arcaneEssence = ForgeRegistries.ITEMS.getValue(ARCANE_ESSENCE_ITEM_ID);
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

    private int getStoredParameterCount(long gameTime) {
        return Mth.clamp((int)(getCappedElapsedTicks(gameTime) / TICKS_PER_ITEM), 0, MAX_STORED_PARAMETER);
    }

    private long getCappedElapsedTicks(long gameTime) {
        var maxElapsedTicks = TICKS_PER_ITEM * MAX_STORED_PARAMETER;
        return Math.max(0L, Math.min(gameTime - placedGameTime, maxElapsedTicks));
    }
}
