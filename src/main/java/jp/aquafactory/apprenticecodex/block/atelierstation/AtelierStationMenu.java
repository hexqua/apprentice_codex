package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AtelierStationMenu extends AbstractContainerMenu {
    static final int FILTER_SLOT_X = 44;
    static final int FILTER_SLOT_Y = 20;
    static final int FILTER_SLOT_SPACING = 18;
    private static final int FLASK_SLOT_X = 53;
    private static final int FLASK_SLOT_Y = 52;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int FLASK_SLOT_START = 0;
    private static final int FLASK_SLOT_END = FLASK_SLOT_START + AtelierStationBlockEntity.FLASK_SLOT_COUNT;
    private static final int FILTER_SLOT_START = FLASK_SLOT_END;
    private static final int FILTER_SLOT_END = FILTER_SLOT_START + AtelierStationBlockEntity.FILTER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FILTER_SLOT_END;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + HOTBAR_SLOT_COUNT;
    private static final int FILTER_SET_BUTTON_BASE = 0;
    private static final int FILTER_CLEAR_BUTTON_BASE = FILTER_SET_BUTTON_BASE + AtelierStationBlockEntity.FILTER_SLOT_COUNT;

    private final Inventory playerInventory;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final Container filterContainer;
    @Nullable
    private final AtelierStationBlockEntity blockEntity;

    public AtelierStationMenu(int containerId, Inventory playerInventory, AtelierStationBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getBlockPos(), blockEntity, blockEntity.getFlaskInventory(),
                ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos()));
    }

    public AtelierStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, resolveBlockEntity(playerInventory, pos), resolveFlaskInventory(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    private AtelierStationMenu(int containerId, Inventory playerInventory, BlockPos blockPos,
                               @Nullable AtelierStationBlockEntity blockEntity,
                               ItemStackHandler flaskInventory,
                               ContainerLevelAccess access) {
        super(MenuRegistry.ATELIER_STATION.get(), containerId);
        this.playerInventory = playerInventory;
        this.blockPos = blockPos;
        this.access = access;
        this.blockEntity = blockEntity;
        this.filterContainer = createFilterContainer(blockEntity);

        for (var slot = 0; slot < AtelierStationBlockEntity.FLASK_SLOT_COUNT; ++slot) {
            addSlot(new FlaskSlot(flaskInventory, slot, FLASK_SLOT_X + slot * 18, FLASK_SLOT_Y));
        }
        for (var slot = 0; slot < AtelierStationBlockEntity.FILTER_SLOT_COUNT; ++slot) {
            addSlot(new FilterSlot(filterContainer, slot, FILTER_SLOT_X + slot * FILTER_SLOT_SPACING, FILTER_SLOT_Y));
        }

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.ATELIER_STATION.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();

        if (slotIndex < FLASK_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < FILTER_SLOT_END) {
            return ItemStack.EMPTY;
        } else {
            if (!(stack.getItem() instanceof AbstractPotionFlaskItem)
                    || !moveItemStackTo(stack, FLASK_SLOT_START, FLASK_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (!isFilterButton(buttonId)) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        if (blockEntity == null) {
            return false;
        }

        var filterSlot = decodeFilterSlot(buttonId);
        if (filterSlot < 0 || filterSlot >= AtelierStationBlockEntity.FILTER_SLOT_COUNT) {
            return false;
        }

        boolean changed;
        if (isFilterClearButton(buttonId)) {
            changed = blockEntity.clearFilter(filterSlot);
        } else {
            changed = blockEntity.setFilter(filterSlot, getCarried());
        }

        if (changed) {
            broadcastChanges();
        }
        return true;
    }

    public @NotNull ItemStack getFilterItem(int slot) {
        if (slot < 0 || slot >= AtelierStationBlockEntity.FILTER_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        return slots.get(FILTER_SLOT_START + slot).getItem();
    }

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }

    public @Nullable AtelierStationBlockEntity getBlockEntity() {
        if (blockEntity != null) {
            return blockEntity;
        }

        var currentBlockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        if (currentBlockEntity instanceof AtelierStationBlockEntity atelierStation) {
            return atelierStation;
        }

        return null;
    }

    public int getStoredFluidAmount() {
        var currentBlockEntity = getBlockEntity();
        return currentBlockEntity == null ? 0 : currentBlockEntity.getStoredFluidAmount();
    }

    public @NotNull List<AtelierStationBlockEntity.StoredPotionEntry> getStoredFluidsForDisplay() {
        var currentBlockEntity = getBlockEntity();
        return currentBlockEntity == null ? List.of() : currentBlockEntity.getStoredFluidsForDisplay();
    }

    public static int encodeFilterSetButtonId(int slot) {
        return FILTER_SET_BUTTON_BASE + slot;
    }

    public static int encodeFilterClearButtonId(int slot) {
        return FILTER_CLEAR_BUTTON_BASE + slot;
    }

    private static @NotNull ItemStackHandler resolveFlaskInventory(Inventory playerInventory, BlockPos pos) {
        var blockEntity = resolveBlockEntity(playerInventory, pos);
        if (blockEntity != null) {
            return blockEntity.getFlaskInventory();
        }

        return new ItemStackHandler(AtelierStationBlockEntity.FLASK_SLOT_COUNT);
    }

    @Nullable
    private static AtelierStationBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        var level = playerInventory.player.level();
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AtelierStationBlockEntity atelierStation) {
            return atelierStation;
        }

        return null;
    }

    private static @NotNull Container createFilterContainer(@Nullable AtelierStationBlockEntity blockEntity) {
        if (blockEntity == null) {
            return new SimpleContainer(AtelierStationBlockEntity.FILTER_SLOT_COUNT);
        }

        return new Container() {
            @Override
            public int getContainerSize() {
                return AtelierStationBlockEntity.FILTER_SLOT_COUNT;
            }

            @Override
            public boolean isEmpty() {
                for (var slot = 0; slot < AtelierStationBlockEntity.FILTER_SLOT_COUNT; ++slot) {
                    if (blockEntity.hasFilter(slot)) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public @NotNull ItemStack getItem(int slot) {
                return blockEntity.getFilter(slot);
            }

            @Override
            public @NotNull ItemStack removeItem(int slot, int amount) {
                var stack = getItem(slot);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                var removed = stack.copy();
                removed.setCount(Math.min(amount, removed.getCount()));
                blockEntity.clearFilter(slot);
                return removed;
            }

            @Override
            public @NotNull ItemStack removeItemNoUpdate(int slot) {
                var stack = getItem(slot);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                blockEntity.clearFilter(slot);
                return stack.copy();
            }

            @Override
            public void setItem(int slot, @NotNull ItemStack stack) {
                if (stack.isEmpty()) {
                    blockEntity.clearFilter(slot);
                    return;
                }

                blockEntity.setFilter(slot, stack);
            }

            @Override
            public void setChanged() {
                blockEntity.setChanged();
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return true;
            }

            @Override
            public void clearContent() {
                for (var slot = 0; slot < AtelierStationBlockEntity.FILTER_SLOT_COUNT; ++slot) {
                    blockEntity.clearFilter(slot);
                }
            }
        };
    }

    private static boolean isFilterButton(int buttonId) {
        return (buttonId >= FILTER_SET_BUTTON_BASE
                && buttonId < FILTER_SET_BUTTON_BASE + AtelierStationBlockEntity.FILTER_SLOT_COUNT)
                || isFilterClearButton(buttonId);
    }

    private static boolean isFilterClearButton(int buttonId) {
        return buttonId >= FILTER_CLEAR_BUTTON_BASE
                && buttonId < FILTER_CLEAR_BUTTON_BASE + AtelierStationBlockEntity.FILTER_SLOT_COUNT;
    }

    private static int decodeFilterSlot(int buttonId) {
        return buttonId >= FILTER_CLEAR_BUTTON_BASE
                ? buttonId - FILTER_CLEAR_BUTTON_BASE
                : buttonId - FILTER_SET_BUTTON_BASE;
    }

    private static final class FlaskSlot extends SlotItemHandler {
        private FlaskSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof AbstractPotionFlaskItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
