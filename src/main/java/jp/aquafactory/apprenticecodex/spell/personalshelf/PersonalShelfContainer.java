package jp.aquafactory.apprenticecodex.spell.personalshelf;

import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

final class PersonalShelfContainer implements Container {
    private final PersonalShelfChestBlockEntity shelfBlockEntity;
    private final ItemStackHandler handler;

    PersonalShelfContainer(PersonalShelfChestBlockEntity shelfBlockEntity, ItemStackHandler handler) {
        this.shelfBlockEntity = shelfBlockEntity;
        this.handler = handler;
    }

    @Override
    public int getContainerSize() {
        return PersonalInventory.MAX_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (var slot = 0; slot < PersonalInventory.MAX_SIZE; ++slot) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.extractItem(slot, amount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        var stack = handler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        handler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= handler.getSlots()) {
            return;
        }
        handler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
        // 実体はプレイヤー capability 側にあり、menu 同期は ChestMenu が行う。
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        var level = shelfBlockEntity.getLevel();
        // Personal Shelf は距離ではなく spell 側の寿命/所有者条件で閉じるため、
        // vanilla chest の距離判定は使わず「まだ同じ棚が存在するか」だけを見る。
        return level != null
                && !shelfBlockEntity.isRemoved()
                && level.getBlockEntity(shelfBlockEntity.getBlockPos()) == shelfBlockEntity;
    }

    @Override
    public void clearContent() {
        for (var slot = 0; slot < handler.getSlots(); ++slot) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void startOpen(@NotNull Player player) {
        var level = shelfBlockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            shelfBlockEntity.onOpenedBy(player, level);
        }
    }

    @Override
    public void stopOpen(@NotNull Player player) {
        var level = shelfBlockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            shelfBlockEntity.onClosedBy(player, level);
        }
    }
}
