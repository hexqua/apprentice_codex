package jp.aquafactory.apprenticecodex.block.apprenticestable;

import com.google.common.collect.Lists;
import io.redspace.ironsspellbooks.api.config.IronConfigParameters;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ApprenticesTableMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INVENTORY_SLOT_START = 2;
    private static final int INVENTORY_SLOT_END = 29;
    private static final int HOTBAR_SLOT_START = 29;
    private static final int HOTBAR_SLOT_END = 38;

    private final ContainerLevelAccess access;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final List<AbstractSpell> availableSpells = Lists.newArrayList();

    long lastSoundTime;
    final Slot inputSlot;
    final Slot resultSlot;
    Runnable slotUpdateListener = () -> {};

    public final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ApprenticesTableMenu.this.slotsChanged(this);
            ApprenticesTableMenu.this.slotUpdateListener.run();
        }
    };

    final ResultContainer resultContainer = new ResultContainer();

    public ApprenticesTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ApprenticesTableMenu(int containerId, Inventory inventory, ContainerLevelAccess containerAccess) {
        super(MenuRegistry.APPRENTICES_TABLE.get(), containerId);
        access = containerAccess;

        inputSlot = addSlot(new Slot(container, INPUT_SLOT, 18, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return true;
            }
        });
        resultSlot = addSlot(new Slot(resultContainer, RESULT_SLOT, 18, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ApprenticesTableMenu.this.inputSlot.remove(1);
                ApprenticesTableMenu.this.setupResultSlot();

                containerAccess.execute((targetLevel, targetPos) -> {
                    var gameTime = targetLevel.getGameTime();
                    if (ApprenticesTableMenu.this.lastSoundTime != gameTime) {
                        targetLevel.playSound(null, targetPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        ApprenticesTableMenu.this.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, stack);
            }
        });

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlot(selectedRecipeIndex);
        selectedRecipeIndex.set(-1);
        refreshAvailableSpells();
    }

    public int getSelectedRecipeIndex() {
        return selectedRecipeIndex.get();
    }

    public List<AbstractSpell> getAvailableSpells() {
        return availableSpells;
    }

    public boolean hasInputItem() {
        return inputSlot.hasItem() && !availableSpells.isEmpty();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.APPRENTICES_TABLE.get());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int recipeIndex) {
        if (isValidSpellIndex(recipeIndex)) {
            selectedRecipeIndex.set(recipeIndex);
            setupResultSlot();
            return true;
        }

        return false;
    }

    private boolean isValidSpellIndex(int recipeIndex) {
        return recipeIndex >= 0 && recipeIndex < availableSpells.size();
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        setupResultSlot();
    }

    void setupResultSlot() {
        if (hasInputItem() && isValidSpellIndex(selectedRecipeIndex.get())) {
            var spell = availableSpells.get(selectedRecipeIndex.get());
            var result = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, 1, result);
            resultSlot.set(result);
        } else {
            resultSlot.set(ItemStack.EMPTY);
        }

        broadcastChanges();
    }

    private void refreshAvailableSpells() {
        availableSpells.clear();
        availableSpells.addAll(
                SpellRegistry.getEnabledSpells()
                        .stream()
                        .filter(AbstractSpell::allowCrafting)
                        .filter(spell -> SpellConfigManager.getSpellConfigValue(spell, IronConfigParameters.MIN_RARITY).getValue() <= 0)
                        .toList()
        );
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var moved = ItemStack.EMPTY;
        var slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            var stackInSlot = slot.getItem();
            var item = stackInSlot.getItem();
            moved = stackInSlot.copy();
            if (slotIndex == RESULT_SLOT) {
                item.onCraftedBy(stackInSlot, player.level(), player);
                if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(stackInSlot, moved);
            } else if (slotIndex == INPUT_SLOT) {
                if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= INVENTORY_SLOT_START && slotIndex < HOTBAR_SLOT_END) {
                if (!moveItemStackTo(stackInSlot, INPUT_SLOT, RESULT_SLOT, false)) {
                    if (slotIndex < INVENTORY_SLOT_END) {
                        if (!moveItemStackTo(stackInSlot, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, INVENTORY_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (stackInSlot.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
            broadcastChanges();
        }

        return moved;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        resultContainer.removeItemNoUpdate(RESULT_SLOT);
        access.execute((targetLevel, targetPos) -> clearContainer(player, container));
    }
}
