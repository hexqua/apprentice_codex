package jp.aquafactory.apprenticecodex.spell.personalshelf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PersonalShelfChestBlockEntity extends BlockEntity implements MenuProvider {
    private static final int EXPORT_COOLDOWN_TICK = 5;
    private static final int FALLBACK_LIFE_TIME_TICKS = 20 * 60;
    private static final double FALLBACK_KEEP_OWNER_RANGE = 10.0;

    private UUID owner;
    private Player cachedOwner;
    private boolean isExportMode;
    private Direction exportFacing;
    private int exportCooldownTick = EXPORT_COOLDOWN_TICK;
    private int lifeTimeTicks = FALLBACK_LIFE_TIME_TICKS;
    private double keepOwnerRange = FALLBACK_KEEP_OWNER_RANGE;
    private final Set<UUID> openers = new HashSet<>();

    public PersonalShelfChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PERSONAL_SHELF_CHEST.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.personal_shelf");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inventory, @NotNull Player player) {
        var capability = Capabilities.getPersonalInventory(player);
        if (capability.isPresent()) {
            var shelf = capability.orElseThrow(IllegalStateException::new);
            return ChestMenu.sixRows(windowId, inventory, new PersonalShelfContainer(this, shelf.getHandler()));
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

    public void setLifeData(int lifeTimeTicks, double keepOwnerRange) {
        this.lifeTimeTicks = lifeTimeTicks;
        this.keepOwnerRange = keepOwnerRange;
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }

        tag.putBoolean("IsExportMode", isExportMode);

        if (exportFacing != null) {
            tag.putInt("ExportFacing", exportFacing.get3DDataValue());
        }

        tag.putInt("LifeTimeTicks", lifeTimeTicks);
        tag.putDouble("KeepOwnerRange", keepOwnerRange);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        isExportMode = tag.getBoolean("IsExportMode");
        exportFacing = tag.contains("ExportFacing", Tag.TAG_INT) ? Direction.from3DDataValue(tag.getInt("ExportFacing")) : null;
        lifeTimeTicks = tag.getInt("LifeTimeTicks");
        keepOwnerRange = tag.getDouble("KeepOwnerRange");
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

    public void onOpenedBy(Player player, Level level) {
        if (!level.isClientSide) {
            openers.add(player.getUUID());
            setChanged();
        }
    }

    public void onClosedBy(Player player, Level level) {
        if (!level.isClientSide) {
            openers.remove(player.getUUID());
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PersonalShelfChestBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.owner == null) {
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        }

        var owner = level.getPlayerByUUID(blockEntity.owner);
        if (owner == null || owner.isRemoved()) {
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        }

        if (blockEntity.cachedOwner != owner) {
            blockEntity.cachedOwner = owner;
            blockEntity.setChanged();
        }

        --blockEntity.lifeTimeTicks;
        if (blockEntity.lifeTimeTicks <= 0) {
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        }

        var xDistance = Math.abs(blockEntity.worldPosition.getX() - blockEntity.cachedOwner.position().x);
        var yDistance = Math.abs(blockEntity.worldPosition.getY() - blockEntity.cachedOwner.position().y);
        var zDistance = Math.abs(blockEntity.worldPosition.getZ() - blockEntity.cachedOwner.position().z);
        if (xDistance > blockEntity.keepOwnerRange || yDistance > blockEntity.keepOwnerRange || zDistance > blockEntity.keepOwnerRange) {
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        }

        if (blockEntity.isExportMode && blockEntity.exportFacing != null) {
            blockEntity.exportItem(level, pos);
        }
    }

    private void expireAndCloseOpenMenus(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (var id : List.copyOf(openers)) {
            var serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (serverPlayer == null) {
                continue;
            }

            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.closeContainer();
            }
        }

        openers.clear();
        serverLevel.removeBlock(pos, false);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getCenter().x, pos.getCenter().y, pos.getCenter().z, 32, 0.2, 0.2, 0.2, 0);
        AudioTools.playSoundFromPosition(level, pos.getCenter(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS);
    }

    private void exportItem(Level level, BlockPos pos) {
        if (exportCooldownTick > 0) {
            --exportCooldownTick;
            return;
        }

        var outPos = pos.relative(exportFacing);
        var outBlockEntity = level.getBlockEntity(outPos);
        if (outBlockEntity == null) {
            return;
        }

        var toTargetSide = exportFacing.getOpposite();
        var targetHandler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, outPos, toTargetSide);
        if (targetHandler == null) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (ownerPlayer == null) {
            return;
        }

        Capabilities.getPersonalInventory(ownerPlayer).ifPresent(personalInventory -> {
            var source = personalInventory.getHandler();
            var slotLimit = Math.min(PersonalInventory.MAX_SIZE, source.getSlots());

            for (var i = 0; i < slotLimit; ++i) {
                var inSlot = source.getStackInSlot(i);
                if (inSlot.isEmpty()) {
                    continue;
                }

                var remainSimulate = ItemHandlerHelper.insertItem(targetHandler, inSlot.copy(), true);
                var inserted = inSlot.getCount() - remainSimulate.getCount();
                if (inserted <= 0) {
                    continue;
                }

                var remainReal = ItemHandlerHelper.insertItem(targetHandler, inSlot.copy(), false);
                var insertedReal = inSlot.getCount() - remainReal.getCount();
                if (insertedReal <= 0) {
                    ApprenticeCodex.LOGGER.warn("Failed to insert item to target inventory.");
                    continue;
                }

                source.extractItem(i, insertedReal, false);
                setChanged();
                exportCooldownTick = EXPORT_COOLDOWN_TICK;
                return;
            }
        });
    }
}
