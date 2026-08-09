package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AlchemyBrewerMenu extends AbstractContainerMenu {
    public static final int TOGGLE_AUTO_BUTTON = 0;
    private static final int MACHINE_SLOT_COUNT = AlchemyBrewerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;
    private final Inventory playerInventory;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final @Nullable AlchemyBrewerBlockEntity blockEntity;
    private final ContainerData data;

    public AlchemyBrewerMenu(int id, Inventory inventory, AlchemyBrewerBlockEntity blockEntity) {
        this(id, inventory, blockEntity.getBlockPos(), blockEntity, blockEntity.getMenuData());
    }
    public AlchemyBrewerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, resolve(inventory, pos), new SimpleContainerData(AlchemyBrewerBlockEntity.MENU_DATA_COUNT));
    }
    private AlchemyBrewerMenu(int id, Inventory playerInventory, BlockPos pos,
                              @Nullable AlchemyBrewerBlockEntity blockEntity, ContainerData data) {
        super(MenuRegistry.ALCHEMY_BREWER.get(), id);
        this.playerInventory = playerInventory; this.blockPos = pos; this.blockEntity = blockEntity;
        this.data = data;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        checkContainerDataCount(data, AlchemyBrewerBlockEntity.MENU_DATA_COUNT);
        addDataSlots(data);
        var handler = blockEntity == null ? new ItemStackHandler(MACHINE_SLOT_COUNT) : blockEntity.getInventory();
        addSlot(new InputSlot(handler, AlchemyBrewerBlockEntity.INPUT_SLOT, 26, 20));
        for (int index = 0; index < AlchemyBrewerBlockEntity.MATERIAL_SLOT_COUNT; index++) {
            addSlot(new SlotItemHandler(handler, AlchemyBrewerBlockEntity.FIRST_MATERIAL_SLOT + index,
                    80 + index % 5 * 18, 34 + index / 5 * 18));
        }
        addSlot(new OutputSlot(handler, AlchemyBrewerBlockEntity.OUTPUT_SLOT, 26, 52));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public @Nullable AlchemyBrewerBlockEntity getBlockEntity() {
        if (blockEntity != null) return blockEntity;
        return resolve(playerInventory, blockPos);
    }
    public boolean isAutoBrewing() { return data.get(AlchemyBrewerBlockEntity.MENU_DATA_AUTO_BREWING) != 0; }
    public int getTankAmountMb() { return data.get(AlchemyBrewerBlockEntity.MENU_DATA_TANK_AMOUNT); }
    public @Nullable ResourceLocation getDisplayPotionId() {
        int registryId = data.get(AlchemyBrewerBlockEntity.MENU_DATA_DISPLAY_POTION);
        if (registryId < 0) return null;
        var potion = BuiltInRegistries.POTION.byId(registryId);
        return potion == null ? null : BuiltInRegistries.POTION.getKey(potion);
    }
    public int getDisplayAmountMb() { return data.get(AlchemyBrewerBlockEntity.MENU_DATA_DISPLAY_AMOUNT); }
    public boolean isProcessing() {
        return (data.get(AlchemyBrewerBlockEntity.MENU_DATA_STATE_FLAGS) & AlchemyBrewerBlockEntity.MENU_FLAG_PROCESSING) != 0;
    }
    public boolean isDisplayPreview() {
        return (data.get(AlchemyBrewerBlockEntity.MENU_DATA_STATE_FLAGS) & AlchemyBrewerBlockEntity.MENU_FLAG_PREVIEW) != 0;
    }
    public int getElapsedTicks() { return data.get(AlchemyBrewerBlockEntity.MENU_DATA_ELAPSED_TICKS); }
    public int getTotalTicks() { return data.get(AlchemyBrewerBlockEntity.MENU_DATA_TOTAL_TICKS); }
    @Override public boolean stillValid(@NotNull Player player) { return stillValid(access, player, BlockRegistry.ALCHEMY_BREWER.get()); }
    @Override public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId != TOGGLE_AUTO_BUTTON || !stillValid(player)) return false;
        if (!player.level().isClientSide && getBlockEntity() != null) getBlockEntity().toggleAutoBrewing();
        return true;
    }
    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (stack.is(Items.GLASS_BOTTLE) || stack.getItem() instanceof AbstractPotionFlaskItem)
                moved = moveItemStackTo(stack, 0, 1, false);
            if (!moved) moved = moveItemStackTo(stack, 1, 11, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }
    private static @Nullable AlchemyBrewerBlockEntity resolve(Inventory inventory, BlockPos pos) {
        var found = inventory.player.level().getBlockEntity(pos);
        return found instanceof AlchemyBrewerBlockEntity brewer ? brewer : null;
    }
    private static final class InputSlot extends SlotItemHandler {
        private InputSlot(ItemStackHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public int getMaxStackSize(@NotNull ItemStack stack) { return stack.getItem() instanceof AbstractPotionFlaskItem ? 1 : super.getMaxStackSize(stack); }
    }
    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(ItemStackHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
    }
}
