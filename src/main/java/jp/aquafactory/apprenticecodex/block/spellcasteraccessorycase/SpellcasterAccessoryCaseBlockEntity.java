package jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
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
    private boolean inventoryLoadPending;

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
        caseStack = stack.copyWithCount(1);
        collected = false;
        if (level == null) {
            // BlockEntity が Level へ接続される前は registry を取得できないため、setLevel まで復元を保留する。
            inventoryLoadPending = true;
        } else {
            loadInventoryFromCaseStack(level.registryAccess());
        }
        setChanged();
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        if (inventoryLoadPending) {
            loadInventoryFromCaseStack(level.registryAccess());
        }
    }

    public ItemStack takeCaseStack() {
        if (collected) {
            return ItemStack.EMPTY;
        }

        var result = getCaseStack().copyWithCount(1);
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
        return collected ? ItemStack.EMPTY : getCaseStack().copyWithCount(1);
    }

    @Override
    public @NotNull Component getDisplayName() {
        var stack = getCaseStack();
        return stack.has(DataComponents.CUSTOM_NAME)
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
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!collected) {
            ensureCaseStack();
            if (!inventoryLoadPending) {
                SpellcasterAccessoryCase.saveInventory(caseStack, inventory, registries);
            }
            tag.put(CASE_STACK_TAG, caseStack.saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        caseStack = tag.contains(CASE_STACK_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(CASE_STACK_TAG))
                : ItemStack.EMPTY;
        collected = false;
        loadInventoryFromCaseStack(registries);
    }

    private void ensureCaseStack() {
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof SpellcasterAccessoryCase)) {
            caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        }
    }

    private void loadInventoryFromCaseStack(HolderLookup.Provider registries) {
        loading = true;
        try {
            for (var slot = 0; slot < inventory.getSlots(); ++slot) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
            if (!caseStack.isEmpty()) {
                SpellcasterAccessoryCase.loadInventory(caseStack, inventory, registries);
            }
        } finally {
            loading = false;
            inventoryLoadPending = false;
        }
    }

    private void syncInventoryToCaseStack() {
        ensureCaseStack();
        if (level != null && !inventoryLoadPending) {
            SpellcasterAccessoryCase.saveInventory(caseStack, inventory, level.registryAccess());
        }
    }
}
