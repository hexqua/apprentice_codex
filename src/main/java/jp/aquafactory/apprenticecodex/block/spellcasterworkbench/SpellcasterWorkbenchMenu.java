package jp.aquafactory.apprenticecodex.block.spellcasterworkbench;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SpellcasterWorkbenchMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int HOTBAR_SLOT_START = INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_SLOT_END = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_X = 19;
    private static final int PLAYER_INVENTORY_Y = 137;
    private static final int HOTBAR_Y = 195;

    private final ContainerLevelAccess access;

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MenuRegistry.SPELLCASTER_WORKBENCH.get(), containerId);
        this.access = access;

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.SPELLCASTER_WORKBENCH.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex < HOTBAR_SLOT_START) {
            if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < HOTBAR_SLOT_END) {
            if (!moveItemStackTo(stack, 0, HOTBAR_SLOT_START, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }
}
