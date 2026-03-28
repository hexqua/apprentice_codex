package jp.aquafactory.apprenticecodex.spell.rifthole;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RiftHoleBlockEntity extends BlockEntity {
    private BlockState originalState = Blocks.AIR.defaultBlockState();
    private @Nullable UUID ownerUuid;
    private @Nullable UUID tunnelId;
    private long expireGameTime;

    public RiftHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.RIFT_HOLE.get(), pos, state);
    }

    public void initialize(BlockState originalState, @Nullable UUID ownerUuid, @Nullable UUID tunnelId, long expireGameTime) {
        this.originalState = originalState;
        this.ownerUuid = ownerUuid;
        this.tunnelId = tunnelId;
        this.expireGameTime = expireGameTime;
        setChanged();
        syncToClient();
    }

    public BlockState getOriginalState() {
        return originalState;
    }

    public boolean matchesTunnel(@Nullable UUID tunnelId) {
        return this.tunnelId != null && this.tunnelId.equals(tunnelId);
    }

    private void syncToClient() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("OriginalState", NbtUtils.writeBlockState(originalState));
        if (ownerUuid != null) {
            tag.putUUID("OwnerUuid", ownerUuid);
        }
        if (tunnelId != null) {
            tag.putUUID("TunnelId", tunnelId);
        }
        tag.putLong("ExpireGameTime", expireGameTime);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        originalState = tag.contains("OriginalState")
                ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("OriginalState"))
                : Blocks.AIR.defaultBlockState();
        ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        tunnelId = tag.hasUUID("TunnelId") ? tag.getUUID("TunnelId") : null;
        expireGameTime = tag.getLong("ExpireGameTime");
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
}
