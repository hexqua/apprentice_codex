package jp.aquafactory.apprenticecodex.common.spells.personalshelf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.capability.Capabilities;
import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventoryMenu;
import jp.aquafactory.apprenticecodex.common.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PersonalShelfChestBlockEntity extends BlockEntity implements MenuProvider {

    private static final int EXPORT_COOLDOWN_TICK = 20;

    @Nullable
    private UUID owner;
    private boolean isExportMode;
    @Nullable
    private Direction exportFacing;
    private int exportCooldownTick = EXPORT_COOLDOWN_TICK;


    public PersonalShelfChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PERSONAL_SHELF_CHEST.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.personal_shelf");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inventory, @NotNull Player player) {
        var capability = player.getCapability(Capabilities.PERSONAL_INVENTORY);
        if (capability.isPresent()) {
            var shelf = capability.orElseThrow(IllegalStateException::new);
            return new PersonalInventoryMenu(windowId, inventory, shelf.getHandler(), getBlockPos());
        }

        return null;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setShelfData(Player player, boolean isExportMode, @Nullable Direction exportFacing) {
        this.owner = player.getUUID();
        this.isExportMode = isExportMode;
        this.exportFacing = exportFacing;

        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }

        tag.putBoolean("IsExportMode", isExportMode);

        if (exportFacing != null) {
            tag.putInt("ExportFacing", exportFacing.get3DDataValue());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        isExportMode = tag.getBoolean("IsExportMode");
        exportFacing = tag.contains("ExportFacing", Tag.TAG_INT) ? Direction.from3DDataValue(tag.getInt("ExportFacing")) : null;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, PersonalShelfChestBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.owner == null ||
                !blockEntity.isExportMode ||
                blockEntity.exportFacing == null) {
            return;
        }

        // 1秒に1スタックずつ.
        if (blockEntity.exportCooldownTick > 0) {
            --blockEntity.exportCooldownTick;
            return;
        }

        var outPos = pos.relative(blockEntity.exportFacing);
        var outBlockEntity = level.getBlockEntity(outPos);
        if (outBlockEntity == null) {
            return;
        }

        // 搬出する際は受け取りは反対向きになる.
        var toTargetSide = blockEntity.exportFacing.getOpposite();
        outBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, toTargetSide)
                .ifPresent(targetHandler -> {
                    // 受け取れる場合の処理.
                    // 近場でしか動かさないのでチャンクロード周りは考慮しない.
                    if (!(level instanceof ServerLevel serverLevel)) {
                        return;
                    }

                    var ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(blockEntity.owner);
                    if (ownerPlayer == null) {
                        // オフラインだとここに落ちる.
                        // オフライン時は動かなくてよいため落としてよい.
                        return;
                    }

                    ownerPlayer.getCapability(Capabilities.PERSONAL_INVENTORY)
                            .ifPresent(personalInventory -> {
                                var source = personalInventory.getHandler();
                                var slotLimit = Math.min(PersonalInventory.MAX_SIZE, source.getSlots());

                                for (var i = 0; i < slotLimit; ++i) {
                                    var inSlot = source.getStackInSlot(i);
                                    if (inSlot.isEmpty()) {
                                        continue;
                                    }

                                    var remainSimulate = ItemHandlerHelper.insertItem(
                                            targetHandler,
                                            inSlot.copy(),
                                            true
                                    );

                                    var inserted = inSlot.getCount() - remainSimulate.getCount();
                                    if (inserted <= 0) {
                                        // 挿入シミュレート失敗なので飛ばす.
                                        continue;
                                    }

                                    var remainReal = ItemHandlerHelper.insertItem(
                                            targetHandler,
                                            inSlot.copy(),
                                            false
                                    );

                                    var insertedReal = inSlot.getCount() - remainReal.getCount();
                                    if (insertedReal <= 0) {
                                        ApprenticeCodex.LOGGER.warn("Failed to insert item to target inventory.");
                                        continue;
                                    }

                                    source.extractItem(i, insertedReal, false);
                                    blockEntity.setChanged();
                                    blockEntity.exportCooldownTick = EXPORT_COOLDOWN_TICK;
                                    return;
                                }
                            });
                });
    }
}
