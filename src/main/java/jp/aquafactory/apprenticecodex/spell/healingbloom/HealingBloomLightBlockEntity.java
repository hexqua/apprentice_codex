package jp.aquafactory.apprenticecodex.spell.healingbloom;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class HealingBloomLightBlockEntity extends BlockEntity {
    private static final int SELF_CLEAN_INTERVAL_TICK = 20;
    private int selfCleanCooldown;
    private boolean persistentAfterBloomGone;

    public HealingBloomLightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.HEALING_BLOOM_LIGHT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HealingBloomLightBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.persistentAfterBloomGone) {
            return;
        }

        if (blockEntity.selfCleanCooldown > 0) {
            --blockEntity.selfCleanCooldown;
            return;
        }
        blockEntity.selfCleanCooldown = SELF_CLEAN_INTERVAL_TICK;

        // 花本体は保存しないため、再ログイン後に残った光源は block entity 側で掃除する。
        var bloomExists = level.getEntitiesOfClass(
                HealingBloomEntity.class,
                new AABB(pos.below()).inflate(0.75, 1.0, 0.75),
                entity -> entity.isAlive() && entity.managesLightAt(pos)
        ).stream().findAny().isPresent();

        if (!bloomExists) {
            level.removeBlock(pos, false);
        }
    }

    public void setPersistentAfterBloomGone(boolean persistentAfterBloomGone) {
        if (this.persistentAfterBloomGone == persistentAfterBloomGone) {
            return;
        }

        this.persistentAfterBloomGone = persistentAfterBloomGone;
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("PersistentAfterBloomGone", persistentAfterBloomGone);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        persistentAfterBloomGone = tag.getBoolean("PersistentAfterBloomGone");
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
}
