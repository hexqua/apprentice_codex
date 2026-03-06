package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    private static final String PLACED_GAME_TIME_TAG = "PlacedGameTime";
    private static final float FILL_DURATION_TICKS = 20.0f * 10.0f;

    private long placedGameTime = -1L;

    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }

    public float getFillRatio(float partialTick) {
        if (level == null || placedGameTime < 0L) {
            return 0.0f;
        }

        var elapsed = (level.getGameTime() + partialTick) - placedGameTime;
        return Mth.clamp(elapsed / FILL_DURATION_TICKS, 0.0f, 1.0f);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcanumInAJarBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.placedGameTime >= 0L) {
            return;
        }

        // 設置起点をサーバー時刻で固定し、再読込や距離外復帰でも同じ進行にする.
        blockEntity.placedGameTime = level.getGameTime();
        blockEntity.setChanged();
        blockEntity.syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
