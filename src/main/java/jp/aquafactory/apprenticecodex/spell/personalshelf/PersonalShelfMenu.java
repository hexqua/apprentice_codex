package jp.aquafactory.apprenticecodex.spell.personalshelf;

import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class PersonalShelfMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public PersonalShelfMenu(int id, Inventory playerInventory, ItemStackHandler personalShelf, BlockPos pos) {
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
        boolean moved;

        if (slotIndex < inventorySize){
            // パーソナルシェルフ→プレイヤー.
            moved = moveItemStackTo(stack, inventorySize, playerEnd, true);
        } else {
            // 移動失敗時に空を返さないと、shift-click 側が同じスタックで再試行し続けて停止する。
            moved = moveItemStackTo(stack, 0, inventorySize, false);
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()){
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        var level = player.level();

        if (!level.isClientSide) {
            var be = level.getBlockEntity(this.pos);
            if (be instanceof PersonalShelfChestBlockEntity shelf) {
                shelf.onClosedBy(player, level);
            }
        }
    }
}
