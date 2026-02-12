package jp.aquafactory.apprenticecodex.common.capability.personalinventory;

import jp.aquafactory.apprenticecodex.common.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class PersonalInventoryMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public PersonalInventoryMenu(int id, Inventory playerInventory, ItemStackHandler personalShelf, BlockPos pos) {
        super(MenuRegistry.PERSONAL_SHELF.get(), id);
        this.pos = pos;

        // ラージサイズチェスt表示
        var slotIndex = 0;
        for (var row = 0; row < 6; ++row) {
            for (var col = 0; col < 9; ++col) {
                var x = 8 + col * 18;
                var y = 18 + row * 18;

                addSlot(new SlotItemHandler(personalShelf, slotIndex, x, y));
                ++slotIndex;
            }
        }

        // インベントリ.
        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                var x = 8 + col * 18;
                var y = 140 + row * 18;

                addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        // ホットバー.
        for (var col = 0; col < 9; ++col) {
            var x = 8 + col * 18;
            var y = 198;

            addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex){
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()){
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();

        var inventorySize = PersonalInventory.MAX_SIZE;
        var playerEnd = slots.size();

        if (slotIndex < inventorySize){
            // パーソナルシェルフ→プレイヤー.
            if (moveItemStackTo(stack, inventorySize, playerEnd, true)){
                return ItemStack.EMPTY;
            }
        } else {
            // プレイヤー→パーソナルシェルフ.
            if (moveItemStackTo(stack, 0, inventorySize, false)){
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()){
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }
}
