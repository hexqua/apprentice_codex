package jp.aquafactory.apprenticecodex.block.spelldispenser;

import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
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
    private final IItemHandlerModifiable inventory;
    private final boolean mounted;
    private final boolean hasOwnerProfile;

    public SpellDispenserMenu(int containerId, Inventory playerInventory, SpellDispenserBlockEntity blockEntity) {
        this(containerId, playerInventory, MenuContext.forBlockEntity(blockEntity));
    }

    public SpellDispenserMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, MenuContext.fromNetwork(playerInventory, data));
    }

    public static SpellDispenserMenu createMounted(
            int containerId,
            Inventory playerInventory,
            BlockPos localPos,
            IItemHandlerModifiable inventory,
            boolean hasOwnerProfile
    ) {
        return new SpellDispenserMenu(containerId, playerInventory, MenuContext.forMounted(localPos, inventory, hasOwnerProfile));
    }

    private SpellDispenserMenu(int containerId, Inventory playerInventory, MenuContext context) {
        super(MenuRegistry.SPELL_DISPENSER.get(), containerId);
        this.playerInventory = playerInventory;
        this.blockPos = context.blockPos();
        this.blockEntity = context.blockEntity();
        this.inventory = context.inventory();
        this.mounted = context.mounted();
        this.hasOwnerProfile = context.hasOwnerProfile();
        this.access = mounted ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(playerInventory.player.level(), blockPos);

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
        if (mounted) {
            // からくり上ではワールド上の block entity が存在しないため、設置ブロック前提の距離判定を使わない。
            return true;
        }
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
        if (!hasOwnerProfile()) {
            return Component.translatable("container.apprenticecodex.spell_dispenser.status.owner_missing");
        }
        return getValidation(player).getStatus(player);
    }

    public boolean isReadyToCast(Player player) {
        return hasOwnerProfile()
                && getValidation(player).isSupported();
    }

    public @NotNull ItemStack getSpellSource() {
        return slots.get(SPELL_SLOT_INDEX).getItem();
    }

    public @Nullable SpellDispenserBlockEntity getBlockEntity() {
        if (mounted) {
            return null;
        }
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

    private static @NotNull IItemHandlerModifiable resolveInventory(Inventory playerInventory, BlockPos pos) {
        var blockEntity = resolveBlockEntity(playerInventory, pos);
        if (blockEntity != null) {
            return blockEntity.getInventory();
        }

        return new ItemStackHandler(1);
    }

    private boolean hasOwnerProfile() {
        var currentBlockEntity = getBlockEntity();
        if (currentBlockEntity != null) {
            return currentBlockEntity.hasOwnerProfile();
        }
        return hasOwnerProfile;
    }

    private record MenuContext(
            BlockPos blockPos,
            @Nullable SpellDispenserBlockEntity blockEntity,
            IItemHandlerModifiable inventory,
            boolean mounted,
            boolean hasOwnerProfile
    ) {
        private static MenuContext forBlockEntity(SpellDispenserBlockEntity blockEntity) {
            return new MenuContext(
                    blockEntity.getBlockPos(),
                    blockEntity,
                    blockEntity.getInventory(),
                    false,
                    blockEntity.hasOwnerProfile()
            );
        }

        private static MenuContext forMounted(BlockPos localPos, IItemHandlerModifiable inventory, boolean hasOwnerProfile) {
            return new MenuContext(localPos, null, inventory, true, hasOwnerProfile);
        }

        private static MenuContext fromNetwork(Inventory playerInventory, FriendlyByteBuf data) {
            var mounted = data.readBoolean();
            var blockPos = data.readBlockPos();
            if (!mounted) {
                var blockEntity = resolveBlockEntity(playerInventory, blockPos);
                return new MenuContext(
                        blockPos,
                        blockEntity,
                        resolveInventory(playerInventory, blockPos),
                        false,
                        blockEntity != null && blockEntity.hasOwnerProfile()
                );
            }

            var hasOwnerProfile = data.readBoolean();
            var inventory = new ItemStackHandler(1);
            inventory.setStackInSlot(0, data.readItem());
            return new MenuContext(blockPos, null, inventory, true, hasOwnerProfile);
        }
    }

    private static final class SpellSourceSlot extends SlotItemHandler {
        private SpellSourceSlot(IItemHandlerModifiable inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SpellDispenserSpellValidator.isSupported(stack);
        }
    }
}
