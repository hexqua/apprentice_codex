package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;

public final class SpellcasterAccessoryCaseMenu extends AbstractContainerMenu implements ICuriosMenu {
    private static final int CASE_X = 8;
    private static final int CASE_Y = 18;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 85;
    private static final int HOTBAR_Y = 143;
    private static final int CURIOS_ROWS_PER_COLUMN = 8;

    private static final int CASE_SLOT_START = 0;
    private static final int CASE_SLOT_END = CASE_SLOT_START + SpellcasterAccessoryCase.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = CASE_SLOT_END;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final int CURIOS_START = HOTBAR_END;
    private final Inventory playerInventory;
    private final int sourceSlot;
    private final SpellcasterAccessoryCase.CaseInventory caseInventory;
    private final ICuriosItemHandler curiosHandler;
    private int curiosPanelWidth;

    public SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, data.readVarInt());
    }

    public SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, int sourceSlot) {
        super(MenuRegistry.SPELLCASTER_ACCESSORY_CASE.get(), containerId);
        this.playerInventory = playerInventory;
        this.sourceSlot = sourceSlot;
        var caseStack = isPlayerInventorySlot(sourceSlot) ? playerInventory.getItem(sourceSlot) : ItemStack.EMPTY;
        this.caseInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, playerInventory.player);
        this.curiosHandler = CuriosApi.getCuriosInventory(playerInventory.player).orElse(null);
        resetSlots();
    }

    @Override
    public void resetSlots() {
        slots.clear();
        lastSlots.clear();
        remoteSlots.clear();
        curiosPanelWidth = 0;

        addCaseSlots();
        addPlayerInventory();
        addPlayerHotbar();
        addCuriosSlots();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.isAlive()
                && isPlayerInventorySlot(sourceSlot)
                && playerInventory.getItem(sourceSlot).getItem() instanceof SpellcasterAccessoryCase;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        // Curiosのmodifier同期より先に再構築後のvanilla slot同期が届く場合があるため、旧構成のclientでは破棄する。
        if (slotId >= 0 && slotId < slots.size()) {
            super.setItem(slotId, stateId, stack);
        }
    }

    @Override
    public @NotNull Slot getSlot(int slotId) {
        // 全内容同期もmodifier同期と前後するため、Curios標準menuと同様に旧構成外のindexを末尾へ丸める。
        if (slotId < 0) {
            return super.getSlot(0);
        }
        if (slotId >= slots.size()) {
            return super.getSlot(slots.size() - 1);
        }
        return super.getSlot(slotId);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex < CASE_SLOT_END) {
            if (!moveItemStackTo(stack, CURIOS_START, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < HOTBAR_END) {
            if (!SpellcasterAccessoryCase.accepts(stack, player)
                    || !moveItemStackTo(stack, CASE_SLOT_START, CASE_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, CASE_SLOT_START, CASE_SLOT_END, false)
                    && !moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
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

    public int getCuriosPanelWidth() {
        return curiosPanelWidth;
    }

    private void addCaseSlots() {
        for (var row = 0; row < SpellcasterAccessoryCase.ROW_COUNT; ++row) {
            for (var col = 0; col < SpellcasterAccessoryCase.COLUMN_COUNT; ++col) {
                var slot = col + row * SpellcasterAccessoryCase.COLUMN_COUNT;
                addSlot(new SlotItemHandler(caseInventory, slot, CASE_X + col * 18, CASE_Y + row * 18));
            }
        }
    }

    private void addPlayerInventory() {
        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                var inventorySlot = col + row * 9 + 9;
                addPlayerSlot(inventorySlot, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18);
            }
        }
    }

    private void addPlayerHotbar() {
        for (var col = 0; col < 9; ++col) {
            addPlayerSlot(col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y);
        }
    }

    private void addPlayerSlot(int inventorySlot, int x, int y) {
        if (inventorySlot == sourceSlot) {
            addSlot(new Slot(playerInventory, inventorySlot, x, y) {
                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
            return;
        }
        addSlot(new Slot(playerInventory, inventorySlot, x, y));
    }

    private void addCuriosSlots() {
        if (curiosHandler == null) {
            return;
        }

        var visibleSlotCount = curiosHandler.getCurios().values().stream()
                .filter(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler::isVisible)
                .mapToInt(handler -> handler.getStacks().getSlots())
                .sum();
        if (visibleSlotCount == 0) {
            return;
        }

        var columnCount = (visibleSlotCount + CURIOS_ROWS_PER_COLUMN - 1) / CURIOS_ROWS_PER_COLUMN;
        curiosPanelWidth = 14 + columnCount * 18;
        var visibleIndex = 0;
        for (var entry : curiosHandler.getCurios().entrySet()) {
            var stacksHandler = entry.getValue();
            if (!stacksHandler.isVisible()) {
                continue;
            }

            var stackHandler = stacksHandler.getStacks();
            for (var slot = 0; slot < stackHandler.getSlots(); ++slot) {
                var column = visibleIndex / CURIOS_ROWS_PER_COLUMN;
                var row = visibleIndex % CURIOS_ROWS_PER_COLUMN;
                var x = 7 - curiosPanelWidth + column * 18;
                var y = 8 + row * 18;
                addSlot(new CurioSlot(
                        playerInventory.player,
                        stackHandler,
                        slot,
                        entry.getKey(),
                        x,
                        y,
                        stacksHandler.getRenders(),
                        stacksHandler.getActiveStates(),
                        stacksHandler.canToggleRendering(),
                        false,
                        false
                ));
                ++visibleIndex;
            }
        }
    }

    private static boolean isPlayerInventorySlot(int slot) {
        return slot >= 0 && slot < Inventory.INVENTORY_SIZE || slot == Inventory.SLOT_OFFHAND;
    }
}
