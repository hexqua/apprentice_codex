package jp.aquafactory.apprenticecodex.block.spellcasterworkbench;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SpellcasterWorkbenchMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_COUNT = 3;
    public static final int RESULT_SLOT = INPUT_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = RESULT_SLOT + 1;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_SLOT_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_SLOT_END = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;
    private static final int[] INPUT_SLOT_X = {20, 40, 20};
    private static final int[] INPUT_SLOT_Y = {23, 33, 43};
    private static final int RESULT_SLOT_X = 81;
    private static final int RESULT_SLOT_Y = 33;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SELECTABLE_ICON_COUNT = 3;

    private final ContainerLevelAccess access;
    private final DataSlot selectedIconIndex = DataSlot.standalone();
    private final Container container = new SimpleContainer(INPUT_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            SpellcasterWorkbenchMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer resultContainer = new ResultContainer();

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MenuRegistry.SPELLCASTER_WORKBENCH.get(), containerId);
        this.access = access;

        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            addSlot(new Slot(container, slotIndex, INPUT_SLOT_X[slotIndex], INPUT_SLOT_Y[slotIndex]));
        }
        addSlot(new Slot(resultContainer, 0, RESULT_SLOT_X, RESULT_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(@NotNull Player player) {
                return false;
            }
        });

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }

        addDataSlot(selectedIconIndex);
        selectedIconIndex.set(-1);
    }

    public int getSelectedIconIndex() {
        return selectedIconIndex.get();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (!isValidIconIndex(buttonId)) {
            return false;
        }

        selectedIconIndex.set(buttonId);
        setupResultSlot();
        return true;
    }

    public boolean hasInputItem() {
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            if (slots.get(slotIndex).hasItem()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.SPELLCASTER_WORKBENCH.get());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        setupResultSlot();
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex == RESULT_SLOT) {
            return ItemStack.EMPTY;
        }

        if (slotIndex < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, INPUT_SLOT_COUNT, false)) {
            if (slotIndex < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        resultContainer.removeItemNoUpdate(0);
        access.execute((level, pos) -> clearContainer(player, container));
    }

    private boolean isValidIconIndex(int index) {
        return index >= 0 && index < SELECTABLE_ICON_COUNT;
    }

    private void setupResultSlot() {
        var result = createPreviewStack(selectedIconIndex.get());
        if (!ItemStack.matches(result, resultContainer.getItem(0))) {
            resultContainer.setItem(0, result);
        }
        broadcastChanges();
    }

    private ItemStack createPreviewStack(int index) {
        return switch (index) {
            case 0 -> new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get());
            case 1 -> new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get());
            case 2 -> new ItemStack(ItemRegistry.ARCANE_SPELLCASTER_ROUND.get());
            default -> ItemStack.EMPTY;
        };
    }
}
