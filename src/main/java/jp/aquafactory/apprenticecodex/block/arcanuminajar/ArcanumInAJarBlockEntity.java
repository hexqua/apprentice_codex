package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    public static final int MAX_STORED_PARAMETER = 8;
    private static final long TICKS_PER_ITEM = 20L * 60L;
    private static final String PLACED_GAME_TIME_TAG = "PlacedGameTime";

    private long placedGameTime = -1L;

    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }

    public int getStoredParameterCount() {
        if (level == null || placedGameTime < 0L) {
            return 0;
        }

        var elapsedTicks = Math.max(0L, level.getGameTime() - placedGameTime);
        return Mth.clamp((int)(elapsedTicks / TICKS_PER_ITEM), 0, MAX_STORED_PARAMETER);
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
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        placedGameTime = tag.contains(PLACED_GAME_TIME_TAG) ? tag.getLong(PLACED_GAME_TIME_TAG) : -1L;
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

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
