package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class AtelierStationMenu extends AbstractContainerMenu {
    private static final int FLASK_SLOT_X = 53;
    private static final int FLASK_SLOT_Y = 52;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int FLASK_SLOT_START = 0;
    private static final int FLASK_SLOT_END = FLASK_SLOT_START + AtelierStationBlockEntity.FLASK_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FLASK_SLOT_END;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + HOTBAR_SLOT_COUNT;

    private final ContainerLevelAccess access;

    public AtelierStationMenu(int containerId, Inventory playerInventory, AtelierStationBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getFlaskInventory(),
                ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.getBlockPos()));
    }

    public AtelierStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolveFlaskInventory(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    private AtelierStationMenu(int containerId, Inventory playerInventory, ItemStackHandler flaskInventory,
                               ContainerLevelAccess access) {
        super(MenuRegistry.ATELIER_STATION.get(), containerId);
        this.access = access;

        for (var slot = 0; slot < AtelierStationBlockEntity.FLASK_SLOT_COUNT; ++slot) {
            addSlot(new FlaskSlot(flaskInventory, slot, FLASK_SLOT_X + slot * 18, FLASK_SLOT_Y));
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
        } else {
            if (!stack.is(ItemRegistry.SPELLCASTERS_FLASK.get())
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

    private static @NotNull ItemStackHandler resolveFlaskInventory(Inventory playerInventory, BlockPos pos) {
        var level = playerInventory.player.level();
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AtelierStationBlockEntity atelierStation) {
            return atelierStation.getFlaskInventory();
        }

        return new ItemStackHandler(AtelierStationBlockEntity.FLASK_SLOT_COUNT);
    }

    private static final class FlaskSlot extends SlotItemHandler {
        private FlaskSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.is(ItemRegistry.SPELLCASTERS_FLASK.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
