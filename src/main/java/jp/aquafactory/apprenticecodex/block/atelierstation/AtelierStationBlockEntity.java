package jp.aquafactory.apprenticecodex.block.atelierstation;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AtelierStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLASK_SLOT_COUNT = 5;
    public static final int FILTER_SLOT_COUNT = 6;
    public static final int MAX_STORED_FLUID_AMOUNT = 16000;
    public static final int MILLIBUCKETS_PER_USE = 250;

    private static final String FLASKS_TAG = "Flasks";
    private static final String FILTERS_TAG = "Filters";
    private static final String SLOT_TAG = "Slot";
    private static final String STORED_FLUIDS_TAG = "StoredFluids";
    private static final String STORED_ITEM_TAG = "Item";
    private static final String STORED_AMOUNT_TAG = "Amount";
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_RADIUS = 2;

    private final ItemStackHandler flaskInventory = new ItemStackHandler(FLASK_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ItemRegistry.SPELLCASTERS_FLASK.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final NonNullList<ItemStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
    private final List<StoredPotionEntry> storedFluids = new ArrayList<>();
    private int storedFluidAmount;

    public AtelierStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ATELIER_STATION.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.atelier_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory,
                                                      @NotNull Player player) {
        return new AtelierStationMenu(containerId, inventory, this);
    }

    public @NotNull ItemStackHandler getFlaskInventory() {
        return flaskInventory;
    }

    public @NotNull ItemStack getFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        return filters.get(slot);
    }

    public boolean hasFilter(int slot) {
        return !getFilter(slot).isEmpty();
    }

    public boolean hasAnyFilterConfigured() {
        for (var filter : filters) {
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int getStoredFluidAmount() {
        return storedFluidAmount;
    }

    public @NotNull List<StoredPotionEntry> getStoredFluidsForDisplay() {
        return storedFluids.stream().map(StoredPotionEntry::copy).toList();
    }

    public boolean setFilter(int slot, @NotNull ItemStack filterStack) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT) {
            return false;
        }

        var normalizedFilter = SpellcastersFlask.copyFilterItem(filterStack);
        if (normalizedFilter.isEmpty()) {
            return false;
        }

        if (ItemStack.matches(filters.get(slot), normalizedFilter)) {
            return false;
        }

        filters.set(slot, normalizedFilter);
        setChanged();
        return true;
    }

    public boolean clearFilter(int slot) {
        if (!hasFilter(slot)) {
            return false;
        }

        filters.set(slot, ItemStack.EMPTY);
        setChanged();
        return true;
    }

    public void dropStoredFlasks() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (var slot = 0; slot < flaskInventory.getSlots(); ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            flaskInventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtelierStationBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        if (!blockEntity.hasAnyFilterConfigured() || blockEntity.storedFluidAmount >= MAX_STORED_FLUID_AMOUNT) {
            return;
        }

        blockEntity.collectFromNearbyCauldrons(serverLevel);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(FLASKS_TAG, flaskInventory.serializeNBT());

        var filterList = new ListTag();
        for (var slot = 0; slot < filters.size(); ++slot) {
            var stack = filters.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var filterTag = new CompoundTag();
            filterTag.putInt(SLOT_TAG, slot);
            filterTag.put("Item", stack.save(new CompoundTag()));
            filterList.add(filterTag);
        }
        tag.put(FILTERS_TAG, filterList);

        var storedFluidList = new ListTag();
        for (var entry : storedFluids) {
            if (entry.amountMb() <= 0 || entry.representativeItem().isEmpty()) {
                continue;
            }

            var storedFluidTag = new CompoundTag();
            storedFluidTag.put(STORED_ITEM_TAG, entry.representativeItem().save(new CompoundTag()));
            storedFluidTag.putInt(STORED_AMOUNT_TAG, entry.amountMb());
            storedFluidList.add(storedFluidTag);
        }
        tag.put(STORED_FLUIDS_TAG, storedFluidList);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        flaskInventory.deserializeNBT(tag.getCompound(FLASKS_TAG));
        storedFluids.clear();
        storedFluidAmount = 0;

        for (var index = 0; index < FILTER_SLOT_COUNT; ++index) {
            filters.set(index, ItemStack.EMPTY);
        }

        if (tag.contains(FILTERS_TAG, Tag.TAG_LIST)) {
            var filterList = tag.getList(FILTERS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < filterList.size(); ++i) {
                var filterTag = filterList.getCompound(i);
                var slot = filterTag.getInt(SLOT_TAG);
                if (slot < 0 || slot >= FILTER_SLOT_COUNT || !filterTag.contains("Item", Tag.TAG_COMPOUND)) {
                    continue;
                }

                var normalizedFilter = SpellcastersFlask.copyFilterItem(ItemStack.of(filterTag.getCompound("Item")));
                if (!normalizedFilter.isEmpty()) {
                    filters.set(slot, normalizedFilter);
                }
            }
        }

        if (tag.contains(STORED_FLUIDS_TAG, Tag.TAG_LIST)) {
            var storedFluidList = tag.getList(STORED_FLUIDS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < storedFluidList.size(); ++i) {
                var storedFluidTag = storedFluidList.getCompound(i);
                if (!storedFluidTag.contains(STORED_ITEM_TAG, Tag.TAG_COMPOUND)) {
                    continue;
                }

                var representativeItem = SpellcastersFlask.copyFilterItem(ItemStack.of(storedFluidTag.getCompound(STORED_ITEM_TAG)));
                var amountMb = normalizeFluidAmount(storedFluidTag.getInt(STORED_AMOUNT_TAG));
                if (representativeItem.isEmpty() || amountMb <= 0) {
                    continue;
                }

                insertStoredFluid(representativeItem, amountMb);
            }
        }
    }

    private void collectFromNearbyCauldrons(ServerLevel level) {
        var minPos = worldPosition.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
        var maxPos = worldPosition.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);
        for (var pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (storedFluidAmount >= MAX_STORED_FLUID_AMOUNT) {
                return;
            }

            if (worldPosition.equals(pos) || !level.isLoaded(pos)) {
                continue;
            }

            var targetBlockEntity = level.getBlockEntity(pos);
            if (!(targetBlockEntity instanceof AlchemistCauldronTile cauldronTile) || cauldronTile.fluidInventory == null) {
                continue;
            }

            collectFromCauldron(level, cauldronTile);
        }
    }

    private void collectFromCauldron(ServerLevel level, AlchemistCauldronTile cauldronTile) {
        var fluidStack = cauldronTile.fluidInventory.getFluidInTank(0);
        if (fluidStack.isEmpty()) {
            return;
        }

        // Flask と同じ代表化ルールに揃えて、表示色とフィルタ一致条件を一致させる。
        var representativeItem = SpellcastersFlask.resolveRepresentativeItem(level, fluidStack);
        if (representativeItem.isEmpty() || !matchesAnyFilter(representativeItem)) {
            return;
        }

        var remainingCapacity = MAX_STORED_FLUID_AMOUNT - storedFluidAmount;
        var requestedAmount = normalizeFluidAmount(Math.min(remainingCapacity, fluidStack.getAmount()));
        if (requestedAmount <= 0) {
            return;
        }

        var simulatedDrain = cauldronTile.fluidInventory.drain(requestedAmount, IFluidHandler.FluidAction.SIMULATE);
        var drainAmount = normalizeFluidAmount(simulatedDrain.getAmount());
        if (drainAmount <= 0) {
            return;
        }

        var drained = cauldronTile.fluidInventory.drain(drainAmount, IFluidHandler.FluidAction.EXECUTE);
        var extractedAmount = normalizeFluidAmount(drained.getAmount());
        if (extractedAmount <= 0) {
            return;
        }

        insertStoredFluid(representativeItem, extractedAmount);
        cauldronTile.setChanged();
        markUpdated();
    }

    private boolean matchesAnyFilter(ItemStack representativeItem) {
        for (var filter : filters) {
            if (!filter.isEmpty() && ItemStack.isSameItemSameTags(filter, representativeItem)) {
                return true;
            }
        }
        return false;
    }

    private void insertStoredFluid(ItemStack representativeItem, int amountMb) {
        var normalizedAmount = normalizeFluidAmount(amountMb);
        if (representativeItem.isEmpty() || normalizedAmount <= 0) {
            return;
        }

        for (var index = 0; index < storedFluids.size(); ++index) {
            var current = storedFluids.get(index);
            if (!ItemStack.isSameItemSameTags(current.representativeItem(), representativeItem)) {
                continue;
            }

            storedFluids.set(index, new StoredPotionEntry(representativeItem, current.amountMb() + normalizedAmount));
            storedFluidAmount = Math.min(MAX_STORED_FLUID_AMOUNT, storedFluidAmount + normalizedAmount);
            return;
        }

        storedFluids.add(new StoredPotionEntry(representativeItem, normalizedAmount));
        storedFluidAmount = Math.min(MAX_STORED_FLUID_AMOUNT, storedFluidAmount + normalizedAmount);
    }

    private void markUpdated() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static int normalizeFluidAmount(int amountMb) {
        if (amountMb <= 0) {
            return 0;
        }

        return Math.min(MAX_STORED_FLUID_AMOUNT, amountMb - amountMb % MILLIBUCKETS_PER_USE);
    }

    public record StoredPotionEntry(ItemStack representativeItem, int amountMb) {
        public StoredPotionEntry {
            representativeItem = SpellcastersFlask.copyFilterItem(representativeItem);
            amountMb = normalizeFluidAmount(amountMb);
        }

        public StoredPotionEntry copy() {
            return new StoredPotionEntry(representativeItem, amountMb);
        }
    }
}
