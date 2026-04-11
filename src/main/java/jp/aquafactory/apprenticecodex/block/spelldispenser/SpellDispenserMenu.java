package jp.aquafactory.apprenticecodex.block.spelldispenser;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserMenu extends AbstractContainerMenu {
    private static final int SPELL_SLOT_X = 80;
    private static final int SPELL_SLOT_Y = 35;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int SPELL_SLOT_INDEX = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    @Nullable
    private final SpellDispenserBlockEntity blockEntity;
    private final ItemStackHandler inventory;

    public SpellDispenserMenu(int containerId, Inventory playerInventory, SpellDispenserBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getBlockPos(), blockEntity, blockEntity.getInventory());
    }

    public SpellDispenserMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        this(containerId, playerInventory, blockPos, resolveBlockEntity(playerInventory, blockPos), resolveInventory(playerInventory, blockPos));
    }

    private SpellDispenserMenu(int containerId, Inventory playerInventory, BlockPos blockPos,
                               @Nullable SpellDispenserBlockEntity blockEntity, ItemStackHandler inventory) {
        super(MenuRegistry.SPELL_DISPENSER.get(), containerId);
        this.playerInventory = playerInventory;
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;
        this.inventory = inventory;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockPos);

        addSlot(new SpellSourceSlot(inventory, SPELL_SLOT_INDEX, SPELL_SLOT_X, SPELL_SLOT_Y));
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
        return stillValid(access, player, BlockRegistry.SPELL_DISPENSER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex == SPELL_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!SpellDispenserSpellValidator.isSupported(stack)
                    || !moveItemStackTo(stack, SPELL_SLOT_INDEX, PLAYER_INVENTORY_START, false)) {
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

    public SpellDispenserSpellValidator.ValidationResult getValidation(Player player) {
        return SpellDispenserSpellValidator.validate(getSpellSource());
    }

    public @NotNull Component getStatus(Player player) {
        var blockEntity = getBlockEntity();
        if (blockEntity != null && !blockEntity.hasOwnerProfile()) {
            return Component.translatable("container.apprenticecodex.spell_dispenser.status.owner_missing");
        }
        return getValidation(player).getStatus(player);
    }

    public boolean isReadyToCast(Player player) {
        var blockEntity = getBlockEntity();
        return blockEntity != null
                && blockEntity.hasOwnerProfile()
                && getValidation(player).isSupported();
    }

    public @NotNull ItemStack getSpellSource() {
        return slots.get(SPELL_SLOT_INDEX).getItem();
    }

    public @Nullable SpellDispenserBlockEntity getBlockEntity() {
        if (blockEntity != null) {
            return blockEntity;
        }

        var currentBlockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        if (currentBlockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
            return spellDispenser;
        }

        return null;
    }

    private static @Nullable SpellDispenserBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        var blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
            return spellDispenser;
        }
        return null;
    }

    private static @NotNull ItemStackHandler resolveInventory(Inventory playerInventory, BlockPos pos) {
        var blockEntity = resolveBlockEntity(playerInventory, pos);
        if (blockEntity != null) {
            return blockEntity.getInventory();
        }

        return new ItemStackHandler(1);
    }

    private static final class SpellSourceSlot extends SlotItemHandler {
        private SpellSourceSlot(ItemStackHandler inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SpellDispenserSpellValidator.isSupported(stack);
        }
    }
}
