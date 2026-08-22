package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase.SpellcasterAccessoryCaseBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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
    private static final int HIDDEN_SLOT_POSITION = -10000;
    public static final int DEFAULT_MAX_VISIBLE_CURIOS_COLUMNS = 5;

    private static final int CASE_SLOT_START = 0;
    private static final int CASE_SLOT_END = CASE_SLOT_START + SpellcasterAccessoryCase.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = CASE_SLOT_END;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final int CURIOS_START = HOTBAR_END;
    private final Inventory playerInventory;
    private final int sourceSlot;
    private final BlockPos sourcePos;
    private final SpellcasterAccessoryCaseBlockEntity sourceBlockEntity;
    private final IItemHandler caseInventory;
    private final ICuriosItemHandler curiosHandler;
    private int maxVisibleCuriosColumns = DEFAULT_MAX_VISIBLE_CURIOS_COLUMNS;
    private int visibleCuriosColumnCount;
    private int visibleCuriosRowCount;
    private boolean curiosPanelVisible;
    private int curiosPanelWidth;

    public SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, readSource(data));
    }

    public SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, int sourceSlot) {
        this(containerId, playerInventory, Source.inventory(sourceSlot));
    }

    public SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, BlockPos sourcePos) {
        this(containerId, playerInventory, Source.block(sourcePos));
    }

    private SpellcasterAccessoryCaseMenu(int containerId, Inventory playerInventory, Source source) {
        super(MenuRegistry.SPELLCASTER_ACCESSORY_CASE.get(), containerId);
        this.playerInventory = playerInventory;
        this.sourceSlot = source.sourceSlot();
        this.sourcePos = source.sourcePos();
        this.sourceBlockEntity = resolveBlockEntity(playerInventory, sourcePos);
        this.caseInventory = resolveCaseInventory(playerInventory, sourceSlot, sourceBlockEntity);
        this.curiosHandler = CuriosApi.getCuriosInventory(playerInventory.player).orElse(null);
        resetSlots();
    }

    public static void writeInventorySource(RegistryFriendlyByteBuf buffer, int sourceSlot) {
        buffer.writeBoolean(false);
        buffer.writeVarInt(sourceSlot);
    }

    public static void writeBlockSource(RegistryFriendlyByteBuf buffer, BlockPos sourcePos) {
        buffer.writeBoolean(true);
        buffer.writeBlockPos(sourcePos);
    }

    @Override
    public void resetSlots() {
        slots.clear();
        lastSlots.clear();
        remoteSlots.clear();
        updateCuriosLayout();

        addCaseSlots();
        addPlayerInventory();
        addPlayerHotbar();
        addCuriosSlots();
    }

    public void configureMaxVisibleCuriosColumns(int maxVisibleCuriosColumns) {
        if (maxVisibleCuriosColumns < 0 || this.maxVisibleCuriosColumns == maxVisibleCuriosColumns) {
            return;
        }
        this.maxVisibleCuriosColumns = maxVisibleCuriosColumns;
        resetSlots();
    }

    private void updateCuriosLayout() {
        curiosPanelWidth = 0;
        visibleCuriosColumnCount = 0;
        visibleCuriosRowCount = 0;
        curiosPanelVisible = false;

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

        visibleCuriosColumnCount = (visibleSlotCount + CURIOS_ROWS_PER_COLUMN - 1) / CURIOS_ROWS_PER_COLUMN;
        visibleCuriosRowCount = Math.min(visibleSlotCount, CURIOS_ROWS_PER_COLUMN);
        curiosPanelVisible = maxVisibleCuriosColumns == 0
                || visibleCuriosColumnCount <= maxVisibleCuriosColumns;
        if (curiosPanelVisible) {
            curiosPanelWidth = 14 + visibleCuriosColumnCount * 18;
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (!player.isAlive()) {
            return false;
        }
        if (sourcePos == null) {
            return isPlayerInventorySlot(sourceSlot)
                    && playerInventory.getItem(sourceSlot).getItem() instanceof SpellcasterAccessoryCase;
        }

        return sourceBlockEntity != null
                && player.level().getBlockEntity(sourcePos) == sourceBlockEntity
                && player.level().getBlockState(sourcePos).is(BlockRegistry.SPELLCASTER_ACCESSORY_CASE.get())
                && player.distanceToSqr(
                        sourcePos.getX() + 0.5D,
                        sourcePos.getY() + 0.5D,
                        sourcePos.getZ() + 0.5D
                ) <= 64.0D;
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
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        var slot = getSlot(slotId);
        if (slot instanceof CurioSlot curioSlot
                && clickType == ClickType.CLONE
                && player.hasInfiniteMaterials()
                && getCarried().isEmpty()) {
            var stack = curioSlot.getSlotExtension()
                    .getCloneStack(curioSlot.getSlotContext(), curioSlot.getItem());
            if (!stack.isEmpty()) {
                setCarried(stack.copyWithCount(stack.getMaxStackSize()));
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
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
            var moved = curiosPanelVisible
                    ? moveItemStackTo(stack, CURIOS_START, slots.size(), false)
                    : moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < HOTBAR_END) {
            if (!SpellcasterAccessoryCase.accepts(stack, player)
                    || !moveItemStackTo(stack, CASE_SLOT_START, CASE_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (curiosPanelVisible) {
            if (!moveItemStackTo(stack, CASE_SLOT_START, CASE_SLOT_END, false)
                    && !moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
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

    public int getCuriosPanelWidth() {
        return curiosPanelWidth;
    }

    public int getVisibleCuriosColumnCount() {
        return visibleCuriosColumnCount;
    }

    public int getVisibleCuriosRowCount() {
        return visibleCuriosRowCount;
    }

    public boolean isCuriosPanelVisible() {
        return curiosPanelVisible;
    }

    private void addCaseSlots() {
        for (var row = 0; row < SpellcasterAccessoryCase.ROW_COUNT; ++row) {
            for (var col = 0; col < SpellcasterAccessoryCase.COLUMN_COUNT; ++col) {
                var slot = col + row * SpellcasterAccessoryCase.COLUMN_COUNT;
                addSlot(new SlotItemHandler(
                        caseInventory,
                        slot,
                        curiosPanelWidth + CASE_X + col * 18,
                        CASE_Y + row * 18
                ) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return SpellcasterAccessoryCase.accepts(stack, playerInventory.player)
                                && super.mayPlace(stack);
                    }
                });
            }
        }
    }

    private void addPlayerInventory() {
        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                var inventorySlot = col + row * 9 + 9;
                addPlayerSlot(
                        inventorySlot,
                        curiosPanelWidth + PLAYER_INVENTORY_X + col * 18,
                        PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
    }

    private void addPlayerHotbar() {
        for (var col = 0; col < 9; ++col) {
            addPlayerSlot(col, curiosPanelWidth + PLAYER_INVENTORY_X + col * 18, HOTBAR_Y);
        }
    }

    private void addPlayerSlot(int inventorySlot, int x, int y) {
        if (sourcePos == null && inventorySlot == sourceSlot) {
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
                var x = curiosPanelVisible ? 7 + column * 18 : HIDDEN_SLOT_POSITION;
                var y = curiosPanelVisible ? 8 + row * 18 : HIDDEN_SLOT_POSITION;
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

    private static Source readSource(RegistryFriendlyByteBuf data) {
        return data.readBoolean() ? Source.block(data.readBlockPos()) : Source.inventory(data.readVarInt());
    }

    private static SpellcasterAccessoryCaseBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (pos != null && inventory.player.level().getBlockEntity(pos)
                instanceof SpellcasterAccessoryCaseBlockEntity blockEntity) {
            return blockEntity;
        }
        return null;
    }

    private static IItemHandler resolveCaseInventory(
            Inventory inventory,
            int sourceSlot,
            SpellcasterAccessoryCaseBlockEntity blockEntity
    ) {
        if (blockEntity != null) {
            return blockEntity.getInventory();
        }
        if (isPlayerInventorySlot(sourceSlot)) {
            return new SpellcasterAccessoryCase.CaseInventory(inventory.getItem(sourceSlot), inventory.player);
        }
        return new ItemStackHandler(SpellcasterAccessoryCase.SLOT_COUNT);
    }

    private record Source(int sourceSlot, BlockPos sourcePos) {
        private static Source inventory(int sourceSlot) {
            return new Source(sourceSlot, null);
        }

        private static Source block(BlockPos sourcePos) {
            return new Source(-1, sourcePos.immutable());
        }
    }
}
