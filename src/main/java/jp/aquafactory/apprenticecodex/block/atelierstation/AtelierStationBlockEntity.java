package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AtelierStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLASK_SLOT_COUNT = 5;
    public static final int FILTER_SLOT_COUNT = 6;

    private static final String FLASKS_TAG = "Flasks";
    private static final String FILTERS_TAG = "Filters";
    private static final String SLOT_TAG = "Slot";
    private static final String STORED_FLUID_AMOUNT_TAG = "StoredFluidAmount";

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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(FLASKS_TAG, flaskInventory.serializeNBT());
        tag.putInt(STORED_FLUID_AMOUNT_TAG, Math.max(0, storedFluidAmount));

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
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        flaskInventory.deserializeNBT(tag.getCompound(FLASKS_TAG));
        storedFluidAmount = Math.max(0, tag.getInt(STORED_FLUID_AMOUNT_TAG));

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

                filters.set(slot, ItemStack.of(filterTag.getCompound("Item")));
            }
        }
    }
}
