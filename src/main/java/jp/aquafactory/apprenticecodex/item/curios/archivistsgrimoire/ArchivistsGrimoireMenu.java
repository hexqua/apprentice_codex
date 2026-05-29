package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class ArchivistsGrimoireMenu extends AbstractContainerMenu {
    private static final int CONTAINER_X = 8;
    private static final int CONTAINER_Y = 18;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 140;
    private static final int HOTBAR_Y = 198;

    private static final int GRIMOIRE_SLOT_START = 0;
    private static final int GRIMOIRE_SLOT_END = GRIMOIRE_SLOT_START + ArchivistsGrimoire.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = GRIMOIRE_SLOT_END;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final InteractionHand hand;
    private final ItemStack grimoireStack;

    public ArchivistsGrimoireMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, data.readEnum(InteractionHand.class));
    }

    public ArchivistsGrimoireMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(MenuRegistry.ARCHIVISTS_GRIMOIRE.get(), containerId);
        this.playerInventory = playerInventory;
        this.hand = hand;
        this.grimoireStack = playerInventory.player.getItemInHand(hand);

        var grimoireInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, playerInventory.player.registryAccess());
        for (var row = 0; row < ArchivistsGrimoire.ROW_COUNT; ++row) {
            for (var col = 0; col < ArchivistsGrimoire.COLUMN_COUNT; ++col) {
                var slot = col + row * ArchivistsGrimoire.COLUMN_COUNT;
                addSlot(new SlotItemHandler(grimoireInventory, slot, CONTAINER_X + col * 18, CONTAINER_Y + row * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return ArchivistsGrimoire.isSlotEnabled(grimoireStack, slot) && ArchivistsGrimoire.isScroll(stack);
                    }
                });
            }
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.isAlive() && player.getItemInHand(hand).getItem() instanceof ArchivistsGrimoire;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex >= GRIMOIRE_SLOT_START && slotIndex < GRIMOIRE_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_INVENTORY_START && slotIndex < HOTBAR_END) {
            if (!ArchivistsGrimoire.isScroll(stack)
                    || !moveItemStackTo(stack, GRIMOIRE_SLOT_START, getGrimoireEnabledSlotEnd(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (var col = 0; col < 9; ++col) {
            var x = PLAYER_INVENTORY_X + col * 18;
            if (hand == InteractionHand.MAIN_HAND && col == playerInventory.selected) {
                addSlot(new Slot(playerInventory, col, x, HOTBAR_Y) {
                    @Override
                    public boolean mayPickup(@NotNull Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            } else {
                addSlot(new Slot(playerInventory, col, x, HOTBAR_Y));
            }
        }
    }

    public ItemStack getGrimoireStack() {
        return grimoireStack;
    }

    public boolean isScrollSlotEnabled(int slot) {
        return ArchivistsGrimoire.isSlotEnabled(grimoireStack, slot);
    }

    public ItemStack getScrollItem(int slot) {
        if (slot < GRIMOIRE_SLOT_START || slot >= GRIMOIRE_SLOT_END) {
            return ItemStack.EMPTY;
        }
        return slots.get(GRIMOIRE_SLOT_START + slot).getItem();
    }

    private int getGrimoireEnabledSlotEnd() {
        return GRIMOIRE_SLOT_START + ArchivistsGrimoire.getUnlockedRowCount(grimoireStack) * ArchivistsGrimoire.COLUMN_COUNT;
    }
}
