package jp.aquafactory.apprenticecodex.spell.personalshelf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PersonalShelfChestBlockEntity extends BlockEntity implements MenuProvider {
    private static final int EXPORT_COOLDOWN_TICK = 5;
    private static final int FALLBACK_LIFE_TIME_TICKS = 20 * 60;
    private static final double FALLBACK_KEEP_OWNER_RANGE = 10.0;
    private static final DustParticleOptions EXPORT_SUCCESS_DUST =
            new DustParticleOptions(new Vector3f(0.78f, 0.28f, 0.98f), 1.0f);
    private static final int EXPORT_SUCCESS_DUST_COUNT = 6;
    private static final double EXPORT_SUCCESS_FACE_OFFSET = 0.38D;
    private static final double EXPORT_SUCCESS_FACE_SPREAD = 0.22D;


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
        var capability = player.getCapability(Capabilities.PERSONAL_INVENTORY);
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
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
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        isExportMode = tag.getBoolean("IsExportMode");
        exportFacing = tag.contains("ExportFacing", Tag.TAG_INT) ? Direction.from3DDataValue(tag.getInt("ExportFacing")) : null;
        lifeTimeTicks = tag.getInt("LifeTimeTicks");
        keepOwnerRange = tag.getDouble("KeepOwnerRange");
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

        if (blockEntity.owner == null){
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        } else {
            var owner = level.getPlayerByUUID(blockEntity.owner);
            if (owner == null || owner.isRemoved()) {
                blockEntity.expireAndCloseOpenMenus(level, pos);
                return;
            }

            if( blockEntity.cachedOwner != owner){
                blockEntity.cachedOwner = owner;
                blockEntity.setChanged();
            }
        }

        --blockEntity.lifeTimeTicks;
        if (blockEntity.lifeTimeTicks <= 0) {
            blockEntity.expireAndCloseOpenMenus(level, pos);
            return;
        }

        // 単純な距離ではなく、XYZそれぞれnブロック以内で判定する.
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

        // 全員のUIを閉じる.
        for (var id : List.copyOf(openers)) {
            var serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (serverPlayer == null){
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
        // 1秒に1スタックずつ.
        if (exportCooldownTick > 0) {
            --exportCooldownTick;
            return;
        }

        var outPos = pos.relative(exportFacing);
        var outBlockEntity = level.getBlockEntity(outPos);
        if (outBlockEntity == null) {
            return;
        }

        // 搬出する際は受け取りは反対向きになる.
        var toTargetSide = exportFacing.getOpposite();
        outBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, toTargetSide)
                .ifPresent(targetHandler -> {
                    // 受け取れる場合の処理.
                    // 近場でしか動かさないのでチャンクロード周りは考慮しない.
                    if (!(level instanceof ServerLevel serverLevel)) {
                        return;
                    }

                    var ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(owner);
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
                                    playExportSuccessEffects(serverLevel, pos);
                                    setChanged();
                                    exportCooldownTick = EXPORT_COOLDOWN_TICK;
                                    return;
                                }
                            });
                });
    }

    private void playExportSuccessEffects(ServerLevel serverLevel, BlockPos pos) {
        if (exportFacing == null) {
            return;
        }

        var effectOrigin = getExportEffectOrigin(pos, exportFacing);
        AudioTools.playSoundFromPosition(serverLevel, effectOrigin, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.65F, 0.05F);

        // 粒子は搬出面から漏れる見た目を優先する。1.21.1 側で粒子 API 差分が出たらここを基準に見直す。
        for (var i = 0; i < EXPORT_SUCCESS_DUST_COUNT; ++i) {
            var particlePos = randomizeOnExportFace(serverLevel, effectOrigin, exportFacing);
            serverLevel.sendParticles(EXPORT_SUCCESS_DUST, particlePos.x, particlePos.y, particlePos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private Vec3 getExportEffectOrigin(BlockPos pos, Direction facing) {
        return pos.getCenter().add(
                facing.getStepX() * EXPORT_SUCCESS_FACE_OFFSET,
                facing.getStepY() * EXPORT_SUCCESS_FACE_OFFSET,
                facing.getStepZ() * EXPORT_SUCCESS_FACE_OFFSET
        );
    }

    private Vec3 randomizeOnExportFace(ServerLevel serverLevel, Vec3 basePosition, Direction facing) {
        var axisOffsetA = (serverLevel.random.nextDouble() - 0.5D) * EXPORT_SUCCESS_FACE_SPREAD * 2.0D;
        var axisOffsetB = (serverLevel.random.nextDouble() - 0.5D) * EXPORT_SUCCESS_FACE_SPREAD * 2.0D;

        return switch (facing.getAxis()) {
            case X -> basePosition.add(0.0D, axisOffsetA, axisOffsetB);
            case Y -> basePosition.add(axisOffsetA, 0.0D, axisOffsetB);
            case Z -> basePosition.add(axisOffsetA, axisOffsetB, 0.0D);
        };
    }
}
