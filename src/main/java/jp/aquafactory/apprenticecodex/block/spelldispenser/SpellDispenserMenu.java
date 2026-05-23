package jp.aquafactory.apprenticecodex.block.spelldispenser;

import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

public final class SpellDispenserMenu extends AbstractContainerMenu {
    static final int SPELL_SLOT_X = 151;
    static final int SPELL_SLOT_Y = 18;
    static final int FLASK_SLOT_X = 26;
    static final int FLASK_SLOT_Y = 54;
    static final int FLASK_SLOT_SPACING = 18;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int SPELL_SLOT_INDEX = SpellDispenserBlockEntity.SPELL_SLOT_INDEX;
    private static final int FLASK_SLOT_START = SpellDispenserBlockEntity.FLASK_SLOT_START;
    private static final int FLASK_SLOT_END = FLASK_SLOT_START + SpellDispenserBlockEntity.FLASK_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FLASK_SLOT_END;
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
    private final SpellDispenserVariant variant;
    @Nullable
    private final String ownerName;
    private final IntSupplier currentManaSupplier;
    private int currentMana;

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
        return createMounted(containerId, playerInventory, localPos, inventory, hasOwnerProfile, null, SpellDispenserManaHelper.MAX_MANA);
    }

    public static SpellDispenserMenu createMounted(
            int containerId,
            Inventory playerInventory,
            BlockPos localPos,
            IItemHandlerModifiable inventory,
            boolean hasOwnerProfile,
            @Nullable String ownerName,
            IntSupplier currentManaSupplier
    ) {
        return createMounted(containerId, playerInventory, localPos, inventory, hasOwnerProfile, ownerName, currentManaSupplier, SpellDispenserVariant.NORMAL);
    }

    public static SpellDispenserMenu createMounted(
            int containerId,
            Inventory playerInventory,
            BlockPos localPos,
            IItemHandlerModifiable inventory,
            boolean hasOwnerProfile,
            @Nullable String ownerName,
            IntSupplier currentManaSupplier,
            SpellDispenserVariant variant
    ) {
        return new SpellDispenserMenu(
                containerId,
                playerInventory,
                MenuContext.forMounted(localPos, inventory, hasOwnerProfile, ownerName, currentManaSupplier, currentManaSupplier.getAsInt(), variant)
        );
    }

    public static SpellDispenserMenu createMounted(
            int containerId,
            Inventory playerInventory,
            BlockPos localPos,
            IItemHandlerModifiable inventory,
            boolean hasOwnerProfile,
            @Nullable String ownerName,
            int currentMana
    ) {
        return createMounted(containerId, playerInventory, localPos, inventory, hasOwnerProfile, ownerName, currentMana, SpellDispenserVariant.NORMAL);
    }

    public static SpellDispenserMenu createMounted(
            int containerId,
            Inventory playerInventory,
            BlockPos localPos,
            IItemHandlerModifiable inventory,
            boolean hasOwnerProfile,
            @Nullable String ownerName,
            int currentMana,
            SpellDispenserVariant variant
    ) {
        return new SpellDispenserMenu(
                containerId,
                playerInventory,
                MenuContext.forMounted(localPos, inventory, hasOwnerProfile, ownerName, null, currentMana, variant)
        );
    }

    private SpellDispenserMenu(int containerId, Inventory playerInventory, MenuContext context) {
        super(MenuRegistry.SPELL_DISPENSER.get(), containerId);
        this.playerInventory = playerInventory;
        this.blockPos = context.blockPos();
        this.blockEntity = context.blockEntity();
        this.inventory = context.inventory();
        this.mounted = context.mounted();
        this.hasOwnerProfile = context.hasOwnerProfile();
        this.variant = context.variant();
        this.ownerName = context.ownerName();
        this.currentMana = SpellDispenserManaHelper.clampMana(context.currentMana());
        this.currentManaSupplier = context.currentManaSupplier() != null
                ? context.currentManaSupplier()
                : () -> this.currentMana;
        this.access = mounted ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(playerInventory.player.level(), blockPos);

        addSlot(new SpellSourceSlot(inventory, SPELL_SLOT_INDEX, SPELL_SLOT_X, SPELL_SLOT_Y));
        for (var slot = 0; slot < SpellDispenserBlockEntity.FLASK_SLOT_COUNT; ++slot) {
            addSlot(new FlaskSlot(inventory, FLASK_SLOT_START + slot, FLASK_SLOT_X + slot * FLASK_SLOT_SPACING, FLASK_SLOT_Y));
        }
        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return SpellDispenserManaHelper.clampMana(currentManaSupplier.getAsInt());
            }

            @Override
            public void set(int value) {
                currentMana = SpellDispenserManaHelper.clampMana(value);
            }
        });
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (mounted) {
            // からくり上ではワールド上の block entity が存在しないため、設置ブロック前提の距離判定を使わない。
            return true;
        }
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).getBlock() instanceof SpellDispenser
                        && player.distanceToSqr(
                        (double) pos.getX() + 0.5D,
                        (double) pos.getY() + 0.5D,
                        (double) pos.getZ() + 0.5D
                ) <= 64.0D, true);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex == SPELL_SLOT_INDEX || (slotIndex >= FLASK_SLOT_START && slotIndex < FLASK_SLOT_END)) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (SpellDispenserSpellValidator.isPlaceableScroll(stack)) {
                if (!moveItemStackTo(stack, SPELL_SLOT_INDEX, FLASK_SLOT_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (SpellDispenserManaHelper.isSupportedFlaskSlotItem(stack)) {
                if (!moveItemStackTo(stack, FLASK_SLOT_START, FLASK_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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

    public boolean isReadyToCast(Player player) {
        var validation = getValidation(player);
        return (!SpellDispenserSpellProfileManager.requiresOwner(validation.spellData()) || hasOwnerProfile())
                && validation.isSupported()
                && (variant.isManaConsumptionExempt()
                || SpellDispenserManaHelper.canAffordSpell(getCurrentMana(), validation.spellData()));
    }

    public @NotNull ItemStack getSpellSource() {
        return slots.get(SPELL_SLOT_INDEX).getItem();
    }

    public boolean hasOwnerProfile() {
        var currentBlockEntity = getBlockEntity();
        if (currentBlockEntity != null) {
            return currentBlockEntity.hasOwnerProfile();
        }
        return hasOwnerProfile;
    }

    public @Nullable String getOwnerName() {
        var currentBlockEntity = getBlockEntity();
        if (currentBlockEntity != null) {
            return currentBlockEntity.getOwnerName();
        }
        return ownerName;
    }

    public int getCurrentMana() {
        var currentBlockEntity = getBlockEntity();
        if (currentBlockEntity != null) {
            return currentBlockEntity.getCurrentMana();
        }
        return SpellDispenserManaHelper.clampMana(currentManaSupplier.getAsInt());
    }

    public int getMaxMana() {
        return SpellDispenserManaHelper.MAX_MANA;
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

        return new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
    }

    private record MenuContext(
            BlockPos blockPos,
            @Nullable SpellDispenserBlockEntity blockEntity,
            IItemHandlerModifiable inventory,
            boolean mounted,
            boolean hasOwnerProfile,
            SpellDispenserVariant variant,
            @Nullable String ownerName,
            @Nullable IntSupplier currentManaSupplier,
            int currentMana
    ) {
        private static MenuContext forBlockEntity(SpellDispenserBlockEntity blockEntity) {
            return new MenuContext(
                    blockEntity.getBlockPos(),
                    blockEntity,
                    blockEntity.getInventory(),
                    false,
                    blockEntity.hasOwnerProfile(),
                    blockEntity.getVariant(),
                    blockEntity.getOwnerName(),
                    blockEntity::getCurrentMana,
                    blockEntity.getCurrentMana()
            );
        }

        private static MenuContext forMounted(
                BlockPos localPos,
                IItemHandlerModifiable inventory,
                boolean hasOwnerProfile,
                @Nullable String ownerName,
                @Nullable IntSupplier currentManaSupplier,
                int currentMana,
                SpellDispenserVariant variant
        ) {
            return new MenuContext(localPos, null, inventory, true, hasOwnerProfile, variant, ownerName, currentManaSupplier, currentMana);
        }

        private static MenuContext fromNetwork(Inventory playerInventory, FriendlyByteBuf data) {
            var mounted = data.readBoolean();
            var blockPos = data.readBlockPos();
            var hasOwnerName = data.readBoolean();
            var ownerName = hasOwnerName ? data.readUtf() : null;
            if (!mounted) {
                var blockEntity = resolveBlockEntity(playerInventory, blockPos);
                return new MenuContext(
                        blockPos,
                        blockEntity,
                        resolveInventory(playerInventory, blockPos),
                        false,
                        blockEntity != null && blockEntity.hasOwnerProfile(),
                        blockEntity != null ? blockEntity.getVariant() : SpellDispenserVariant.NORMAL,
                        blockEntity != null ? blockEntity.getOwnerName() : ownerName,
                        blockEntity != null ? blockEntity::getCurrentMana : null,
                        blockEntity != null ? blockEntity.getCurrentMana() : SpellDispenserManaHelper.MAX_MANA
                );
            }

            var hasOwnerProfile = data.readBoolean();
            var currentMana = SpellDispenserManaHelper.clampMana(data.readVarInt());
            var variant = data.readBoolean() ? SpellDispenserVariant.CREATIVE : SpellDispenserVariant.NORMAL;
            var inventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            for (var slot = 0; slot < SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT; ++slot) {
                inventory.setStackInSlot(slot, data.readItem());
            }
            return new MenuContext(blockPos, null, inventory, true, hasOwnerProfile, variant, ownerName, null, currentMana);
        }
    }

    private static final class SpellSourceSlot extends SlotItemHandler {
        private SpellSourceSlot(IItemHandlerModifiable inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SpellDispenserSpellValidator.isPlaceableScroll(stack);
        }
    }

    private static final class FlaskSlot extends SlotItemHandler {
        private FlaskSlot(IItemHandlerModifiable inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SpellDispenserManaHelper.isSupportedFlaskSlotItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
