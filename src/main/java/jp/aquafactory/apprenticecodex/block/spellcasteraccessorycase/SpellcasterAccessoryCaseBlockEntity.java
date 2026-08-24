package jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public final class SpellcasterAccessoryCaseBlockEntity extends BlockEntity implements MenuProvider {
    private static final String CASE_STACK_TAG = "CaseStack";
    private static final String CONTAINER_KEY = "container.apprenticecodex.spellcaster_accessory_case";

    private final ItemStackHandler inventory = new ItemStackHandler(SpellcasterAccessoryCase.SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // viewer に依存する Curios validator は menu slot 側で確認し、共有保存先では自己格納だけを拒否する。
            return !stack.isEmpty() && !(stack.getItem() instanceof SpellcasterAccessoryCase);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!loading) {
                syncInventoryToCaseStack();
                setChanged();
            }
        }
    };
    private ItemStack caseStack = ItemStack.EMPTY;
    private boolean loading;
    private boolean collected;

    public SpellcasterAccessoryCaseBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SPELLCASTER_ACCESSORY_CASE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack getCaseStack() {
        ensureCaseStack();
        syncInventoryToCaseStack();
        return caseStack;
    }

    public void setCaseStack(ItemStack stack) {
        caseStack = copySingle(stack);
        collected = false;
        loadInventoryFromCaseStack();
        setChanged();
    }

    public ItemStack takeCaseStack() {
        if (collected) {
            return ItemStack.EMPTY;
        }

        var result = copySingle(getCaseStack());
        collected = true;
        caseStack = ItemStack.EMPTY;
        loading = true;
        try {
            for (var slot = 0; slot < inventory.getSlots(); ++slot) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        } finally {
            loading = false;
        }
        setChanged();
        return result;
    }

    public ItemStack copyCaseStackForDrop() {
        return collected ? ItemStack.EMPTY : copySingle(getCaseStack());
    }

    @Override
    public @NotNull Component getDisplayName() {
        var stack = getCaseStack();
        return stack.hasCustomHoverName()
                ? stack.getHoverName()
                : Component.translatable(CONTAINER_KEY);
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(
            int containerId,
            @NotNull Inventory inventory,
            @NotNull Player player
    ) {
        return new SpellcasterAccessoryCaseMenu(containerId, inventory, getBlockPos());
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!collected) {
            ensureCaseStack();
            SpellcasterAccessoryCase.saveInventory(caseStack, inventory);
            tag.put(CASE_STACK_TAG, caseStack.save(new CompoundTag()));
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        caseStack = tag.contains(CASE_STACK_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(CASE_STACK_TAG))
                : ItemStack.EMPTY;
        collected = false;
        loadInventoryFromCaseStack();
    }

    private void ensureCaseStack() {
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof SpellcasterAccessoryCase)) {
            caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        }
    }

    private void loadInventoryFromCaseStack() {
        loading = true;
        try {
            for (var slot = 0; slot < inventory.getSlots(); ++slot) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            if (!caseStack.isEmpty()) {
                SpellcasterAccessoryCase.loadInventory(caseStack, inventory);
            }
        } finally {
            loading = false;
        }
    }

    private void syncInventoryToCaseStack() {
        ensureCaseStack();
        SpellcasterAccessoryCase.saveInventory(caseStack, inventory);
    }

    private static ItemStack copySingle(ItemStack stack) {
        var result = stack.copy();
        result.setCount(1);
        return result;
    }
}
