package jp.aquafactory.apprenticecodex.block.spellcalibrationbench;

import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class SpellCalibrationBenchMenu extends AbstractContainerMenu {
    static final int GAUNTLET_SLOT_X = 26;
    static final int GAUNTLET_SLOT_Y = 27;
    static final int ADJUSTMENT_SLOT_X = 8;
    static final int ADJUSTMENT_SLOT_Y = 51;
    static final int SCROLL_SLOT_X = 80;
    static final int SCROLL_SLOT_Y = 33;
    static final int PLAYER_INVENTORY_X = 8;
    static final int PLAYER_INVENTORY_Y = 84;
    static final int HOTBAR_Y = 142;

    private static final int SLOT_SPACING = 18;
    private static final int SCROLL_COLUMNS = 5;
    private static final int GAUNTLET_SLOT = 0;
    private static final int ADJUSTMENT_SLOT_START = GAUNTLET_SLOT + 1;
    private static final int ADJUSTMENT_SLOT_END = ADJUSTMENT_SLOT_START + ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    private static final int SCROLL_SLOT_START = ADJUSTMENT_SLOT_END;
    private static final int SCROLL_SLOT_END = SCROLL_SLOT_START + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = SCROLL_SLOT_END;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_SLOT_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_SLOT_END = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;

    public static final int TARGET_MENU_SLOT = GAUNTLET_SLOT;
    public static final int SCROLL_MENU_SLOT_START = SCROLL_SLOT_START;

    private final ContainerLevelAccess access;
    private final HolderLookup.Provider lookupProvider;
    private final ItemStackHandler gauntletInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            refreshTargetCalibration();
        }
    };

    public SpellCalibrationBenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public SpellCalibrationBenchMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(MenuRegistry.SPELL_CALIBRATION_BENCH.get(), containerId);
        this.access = access;
        this.lookupProvider = playerInventory.player.level().registryAccess();

        addSlot(new GauntletSlot(gauntletInventory, 0, GAUNTLET_SLOT_X, GAUNTLET_SLOT_Y));
        var adjustmentContainer = new AdjustmentContainer();
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            addSlot(new AdjustmentSlot(adjustmentContainer, slot, ADJUSTMENT_SLOT_X + slot * SLOT_SPACING, ADJUSTMENT_SLOT_Y));
        }

        var scrollContainer = new ScrollContainer();
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            addSlot(new ScrollSlot(
                    scrollContainer,
                    slot,
                    SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            ));
        }

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INVENTORY_X + col * SLOT_SPACING, PLAYER_INVENTORY_Y + row * SLOT_SPACING));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + col * SLOT_SPACING, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.SPELL_CALIBRATION_BENCH.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isCalibrationTarget(stack)) {
            if (!moveItemStackTo(stack, GAUNTLET_SLOT, GAUNTLET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isAdjustmentItem(stack)) {
            if (!moveItemStackTo(stack, ADJUSTMENT_SLOT_START, ADJUSTMENT_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isScroll(stack)) {
            if (!moveItemStackTo(stack, SCROLL_SLOT_START, SCROLL_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    public boolean hasGauntlet() {
        return isScrollcasterGauntlet(getGauntletStack());
    }

    public boolean hasCalibrationTarget() {
        return hasGauntlet() || SpellCalibrationImbueHelper.isSupportedTarget(getGauntletStack());
    }

    public int getEnabledScrollSlotCount() {
        if (hasGauntlet()) {
            return ScrollcasterGauntlet.getEnabledCalibrationScrollSlotCount(getGauntletStack());
        }
        return SpellCalibrationImbueHelper.getSpellSlotCount(getGauntletStack());
    }

    public boolean isScrollSlotEnabled(int slot) {
        return hasCalibrationTarget() && slot >= 0 && slot < getEnabledScrollSlotCount();
    }

    public @NotNull ItemStack getScrollItem(int slot) {
        return getScroll(slot);
    }

    public @NotNull ItemStack getAdjustmentItem(int slot) {
        return getAdjustment(slot);
    }

    public @NotNull ItemStack getLockedPreviewScrollItem(int slot) {
        if (hasGauntlet() || !hasCalibrationTarget() || !isScrollSlotEnabled(slot)) {
            return ItemStack.EMPTY;
        }
        return SpellCalibrationImbueHelper.createLockedPreviewScrollForSlot(getGauntletStack(), slot);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }

        var gauntletStack = gauntletInventory.getStackInSlot(0);
        if (!gauntletStack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(gauntletStack.copy());
            gauntletInventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @NotNull
    private ItemStack getGauntletStack() {
        return gauntletInventory.getStackInSlot(0);
    }

    private @NotNull ItemStack getAdjustment(int slot) {
        return hasGauntlet()
                ? ScrollcasterGauntlet.getCalibrationAdjustment(getGauntletStack(), slot, lookupProvider)
                : ItemStack.EMPTY;
    }

    private void setAdjustment(int slot, @NotNull ItemStack stack) {
        if (!hasGauntlet()) {
            return;
        }

        var storedStack = stack.copy();
        if (!storedStack.isEmpty()) {
            storedStack.setCount(1);
        }
        ScrollcasterGauntlet.setCalibrationAdjustment(getGauntletStack(), slot, storedStack, lookupProvider);
    }

    private @NotNull ItemStack getScroll(int slot) {
        if (hasGauntlet()) {
            return ScrollcasterGauntlet.getCalibrationScroll(getGauntletStack(), slot, lookupProvider);
        }
        return hasCalibrationTarget()
                ? SpellCalibrationImbueHelper.createScrollForSlot(getGauntletStack(), slot)
                : ItemStack.EMPTY;
    }

    private void refreshTargetCalibration() {
        var gauntletStack = getGauntletStack();
        if (hasGauntlet()) {
            ScrollcasterGauntlet.refreshCalibrationEnchantments(gauntletStack, lookupProvider);
            ScrollcasterGauntlet.refreshResolvedCalibrationSchool(gauntletStack, lookupProvider);
            ScrollcasterGauntlet.refreshSelectedSpellContainer(gauntletStack, lookupProvider);
        } else if (!gauntletStack.isEmpty()) {
            SpellCalibrationImbueHelper.prepareTarget(gauntletStack);
        }
    }

    private void setScroll(int slot, @NotNull ItemStack stack) {
        if (!hasCalibrationTarget()) {
            return;
        }

        if (hasGauntlet()) {
            var storedStack = stack.copy();
            if (!storedStack.isEmpty()) {
                storedStack.setCount(1);
            }
            ScrollcasterGauntlet.setCalibrationScroll(getGauntletStack(), slot, storedStack, lookupProvider);
            return;
        }

        if (stack.isEmpty()) {
            SpellCalibrationImbueHelper.removeScrollAt(getGauntletStack(), slot);
        } else {
            var storedStack = stack.copy();
            storedStack.setCount(1);
            SpellCalibrationImbueHelper.setScrollAt(getGauntletStack(), slot, storedStack);
        }
    }

    private boolean hasSchoolRuneAdjustmentExcept(int excludedSlot) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (slot == excludedSlot) {
                continue;
            }
            if (isSchoolRune(getAdjustment(slot))) {
                return true;
            }
        }
        return false;
    }

    static boolean isAdjustmentItem(@NotNull ItemStack stack) {
        return isSpellSlotUpgrade(stack) || isSchoolRune(stack) || isEnchantmentBook(stack);
    }

    static boolean isSchoolRune(@NotNull ItemStack stack) {
        return ScrollcasterSchoolRuneResolver.isSchoolRune(stack);
    }

    static boolean isSpellSlotUpgrade(@NotNull ItemStack stack) {
        return ScrollcasterGauntlet.isCalibrationSlotUpgrade(stack);
    }

    static boolean isEnchantmentBook(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS);
    }

    private static boolean isScroll(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof Scroll;
    }

    private static boolean isScrollcasterGauntlet(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
    }

    private static boolean isCalibrationTarget(@NotNull ItemStack stack) {
        return isScrollcasterGauntlet(stack) || SpellCalibrationImbueHelper.isSupportedTarget(stack);
    }

    private final class AdjustmentContainer implements Container {
        private final NonNullList<ItemStack> fallbackItems =
                NonNullList.withSize(ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT, ItemStack.EMPTY);

        @Override
        public int getContainerSize() {
            return ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (var slot = 0; slot < getContainerSize(); ++slot) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return hasGauntlet() ? getAdjustment(slot) : fallbackItems.get(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var current = getItem(slot);
            if (current.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            var removed = current.copy();
            removed.setCount(Math.min(amount, removed.getCount()));
            setItem(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var current = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return current;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            var storedStack = stack.copy();
            if (!storedStack.isEmpty()) {
                storedStack.setCount(1);
            }

            if (!hasGauntlet()) {
                fallbackItems.set(slot, storedStack);
            } else {
                setAdjustment(slot, storedStack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (var slot = 0; slot < getContainerSize(); ++slot) {
                setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private final class ScrollContainer implements Container {
        private final NonNullList<ItemStack> fallbackItems =
                NonNullList.withSize(ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT, ItemStack.EMPTY);

        @Override
        public int getContainerSize() {
            return ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (var slot = 0; slot < getContainerSize(); ++slot) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            if (hasGauntlet()) {
                return getScroll(slot);
            }
            if (hasCalibrationTarget()) {
                return SpellCalibrationImbueHelper.createScrollForSlot(getGauntletStack(), slot);
            }
            return fallbackItems.get(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var current = getItem(slot);
            if (current.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            var removed = current.copy();
            removed.setCount(Math.min(amount, removed.getCount()));
            setItem(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var current = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return current;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            var storedStack = stack.copy();
            if (!storedStack.isEmpty()) {
                storedStack.setCount(1);
            }

            if (!hasCalibrationTarget()) {
                fallbackItems.set(slot, storedStack);
            } else {
                setScroll(slot, storedStack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (var slot = 0; slot < getContainerSize(); ++slot) {
                setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static final class GauntletSlot extends SlotItemHandler {
        private GauntletSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return isCalibrationTarget(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return 1;
        }
    }

    private final class AdjustmentSlot extends Slot {
        private final int calibrationSlot;

        private AdjustmentSlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
            this.calibrationSlot = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (!hasGauntlet() || !isAdjustmentItem(stack)) {
                return false;
            }

            return !isSchoolRune(stack) || !hasSchoolRuneAdjustmentExcept(calibrationSlot);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return hasGauntlet();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return 1;
        }
    }

    private final class ScrollSlot extends Slot {
        private final int calibrationSlot;

        private ScrollSlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
            this.calibrationSlot = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (!hasCalibrationTarget() || !isScrollSlotEnabled(calibrationSlot) || !isScroll(stack)) {
                return false;
            }
            return hasGauntlet()
                    || SpellCalibrationImbueHelper.canPlaceScrollAt(getGauntletStack(), calibrationSlot, stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return hasCalibrationTarget() && (isScrollSlotEnabled(calibrationSlot) || hasGauntlet() && hasItem());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return 1;
        }
    }
}
