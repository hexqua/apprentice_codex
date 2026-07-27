package jp.aquafactory.apprenticecodex.block.apprenticedesk;

import com.google.common.collect.Lists;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.apprenticedesk.PartiallyUsedInkState;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
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
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApprenticeDeskMenu extends AbstractContainerMenu {
    public static final int INK_SLOT = 0;
    public static final int WAND_BASE_SLOT = 1;
    public static final int FOCUS_SLOT = 2;
    public static final int RESULT_SLOT = 3;
    private static final int INVENTORY_SLOT_START = 4;
    private static final int INVENTORY_SLOT_END = 31;
    private static final int HOTBAR_SLOT_START = 31;
    private static final int HOTBAR_SLOT_END = 40;

    private final ContainerLevelAccess access;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final List<AbstractSpell> availableSpells = Lists.newArrayList();

    long lastSoundTime;
    final Slot inkSlot;
    final Slot wandBaseSlot;
    final Slot focusSlot;
    final Slot resultSlot;
    Runnable slotUpdateListener = () -> {};

    public final Container container = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ApprenticeDeskMenu.this.slotsChanged(this);
            ApprenticeDeskMenu.this.slotUpdateListener.run();
        }
    };

    final ResultContainer resultContainer = new ResultContainer();

    public ApprenticeDeskMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ApprenticeDeskMenu(int containerId, Inventory inventory, ContainerLevelAccess containerAccess) {
        super(MenuRegistry.APPRENTICE_DESK.get(), containerId);
        access = containerAccess;

        inkSlot = addSlot(new Slot(container, INK_SLOT, 12, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidInk(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        wandBaseSlot = addSlot(new Slot(container, WAND_BASE_SLOT, 35, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidWandBase(stack);
            }
        });
        focusSlot = addSlot(new Slot(container, FOCUS_SLOT, 58, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidFocus(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        resultSlot = addSlot(new Slot(resultContainer, 0, 35, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ApprenticeDeskMenu.this.consumeInk();
                ApprenticeDeskMenu.this.wandBaseSlot.remove(1);
                ApprenticeDeskMenu.this.setupResultSlot();

                access.execute((targetLevel, targetPos) -> {
                    var gameTime = targetLevel.getGameTime();
                    if (ApprenticeDeskMenu.this.lastSoundTime != gameTime) {
                        targetLevel.playSound(
                                null,
                                targetPos,
                                SoundRegistry.VANILLA_USE_DESK.get(),
                                SoundSource.BLOCKS,
                                1.0F,
                                1.0F
                        );
                        ApprenticeDeskMenu.this.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, stack);
            }
        });

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 29 + col * 18, 84 + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 29 + col * 18, 142));
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

    public Slot getInkSlot() {
        return inkSlot;
    }

    public boolean hasAllInputs() {
        return isValidInk(inkSlot.getItem())
                && isValidWandBase(wandBaseSlot.getItem())
                && isValidFocus(focusSlot.getItem());
    }

    public boolean canInkCraft(AbstractSpell spell) {
        var inkRarity = getInkRarity();
        return inkRarity != null
                && SpellConfigManager.getSpellConfigValue(spell, SpellConfigParameter.MIN_RARITY).getValue()
                <= inkRarity.getValue();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.APPRENTICE_DESK.get());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int recipeIndex) {
        if (!hasAllInputs() || !isValidSpellIndex(recipeIndex)) {
            return false;
        }

        var spell = availableSpells.get(recipeIndex);
        if (!canInkCraft(spell) || !spell.canBeCraftedBy(player)) {
            return false;
        }

        selectedRecipeIndex.set(recipeIndex);
        setupResultSlot();
        return true;
    }

    private boolean isValidSpellIndex(int recipeIndex) {
        return recipeIndex >= 0 && recipeIndex < availableSpells.size();
    }

    private static boolean isValidInk(ItemStack stack) {
        return !stack.isEmpty()
                && (PartiallyUsedInkState.OfficialInk.fromOriginal(stack) != null
                || PartiallyUsedInkState.readValid(stack).isPresent());
    }

    private static boolean isValidWandBase(ItemStack stack) {
        return stack.is(TagRegistry.Items.WAND_BASE);
    }

    private static boolean isValidFocus(ItemStack stack) {
        return stack.is(ModTags.SCHOOL_FOCUS) || getScrollSpell(stack) != null;
    }

    @Override
    public void slotsChanged(@NotNull Container changedContainer) {
        var previousSelection = isValidSpellIndex(selectedRecipeIndex.get())
                ? availableSpells.get(selectedRecipeIndex.get())
                : null;
        refreshAvailableSpells();
        selectedRecipeIndex.set(previousSelection == null ? -1 : availableSpells.indexOf(previousSelection));
        if (!hasAllInputs() || !isValidSpellIndex(selectedRecipeIndex.get())) {
            selectedRecipeIndex.set(-1);
        }
        setupResultSlot();
    }

    void setupResultSlot() {
        var result = ItemStack.EMPTY;
        if (hasAllInputs() && isValidSpellIndex(selectedRecipeIndex.get())) {
            var spell = availableSpells.get(selectedRecipeIndex.get());
            if (canInkCraft(spell)) {
                var inkRarity = getInkRarity();
                var spellLevel = spell.getMinLevelForRarity(inkRarity);
                result = new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.WOODEN_WAND.get());
                var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
                spellContainer.addSpellAtIndex(spell, spellLevel, 0, true);
                ISpellContainer.set(result, spellContainer.toImmutable());
            }
        }

        resultSlot.set(result);
        broadcastChanges();
    }

    private void refreshAvailableSpells() {
        var schools = getFocusSchools();
        var spellCraftBlacklist = getSpellCraftBlacklist();
        availableSpells.clear();
        if (schools.isEmpty()) {
            return;
        }

        availableSpells.addAll(SpellRegistry.getEnabledSpells().stream()
                .filter(AbstractSpell::allowCrafting)
                .filter(spell -> schools.contains(spell.getSchoolType()))
                .filter(spell -> !spellCraftBlacklist.contains(spell.getSpellId()))
                .toList());
        availableSpells.sort(Comparator
                .comparingInt((AbstractSpell spell) ->
                        SpellConfigManager.getSpellConfigValue(spell, SpellConfigParameter.MIN_RARITY).getValue())
                .thenComparing(spell -> spell.getSchoolType().getId().toString())
                .thenComparing(spell -> spell.getDisplayName(null).getString()));
    }

    private Set<io.redspace.ironsspellbooks.api.spells.SchoolType> getFocusSchools() {
        var focusStack = focusSlot.getItem();
        if (focusStack.is(ModTags.SCHOOL_FOCUS)) {
            return new HashSet<>(SchoolRegistry.getSchoolsFromFocus(focusStack));
        }

        var scrollSpell = getScrollSpell(focusStack);
        return scrollSpell == null ? Set.of() : Set.of(scrollSpell.getSpell().getSchoolType());
    }

    private @Nullable SpellRarity getInkRarity() {
        var stack = inkSlot.getItem();
        var original = PartiallyUsedInkState.OfficialInk.fromOriginal(stack);
        if (original != null) {
            return original.rarity();
        }
        return PartiallyUsedInkState.readValid(stack)
                .map(state -> state.source().rarity())
                .orElse(null);
    }

    private void consumeInk() {
        var stack = inkSlot.getItem();
        var returnGlassBottle =
                ApprenticeCodexServerConfig.apprenticeDeskReturnGlassBottleWhenInkDepleted();
        var original = PartiallyUsedInkState.OfficialInk.fromOriginal(stack);
        if (original != null) {
            inkSlot.set(PartiallyUsedInkState.consumeOriginal(
                    original,
                    ApprenticeCodexServerConfig.apprenticeDeskInkMaxUses(original.rarity()),
                    returnGlassBottle
            ));
            return;
        }
        if (PartiallyUsedInkState.readValid(stack).isPresent()) {
            inkSlot.set(PartiallyUsedInkState.consumePartiallyUsed(stack, returnGlassBottle));
        }
    }

    private static @Nullable SpellData getScrollSpell(ItemStack stack) {
        if (!stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())
                || !ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) {
            return null;
        }
        var spellData = container.getSpellAtIndex(0);
        return spellData == SpellData.EMPTY || spellData.getSpell() == null ? null : spellData;
    }

    private Set<String> getSpellCraftBlacklist() {
        if (!ApprenticeCodexServerConfig.apprenticeDeskEnableSpellCraftBlacklist()) {
            return Set.of();
        }
        return Set.copyOf(ApprenticeCodexServerConfig.apprenticeDeskSpellCraftBlacklist());
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var moved = ItemStack.EMPTY;
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return moved;
        }

        var stackInSlot = slot.getItem();
        moved = stackInSlot.copy();
        if (slotIndex == RESULT_SLOT) {
            if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, moved);
        } else if (slotIndex >= INK_SLOT && slotIndex < RESULT_SLOT) {
            if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= INVENTORY_SLOT_START && slotIndex < HOTBAR_SLOT_END) {
            var targetSlot = getInputTargetSlot(stackInSlot);
            if (targetSlot >= 0) {
                if (!moveItemStackTo(stackInSlot, targetSlot, targetSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < INVENTORY_SLOT_END) {
                if (!moveItemStackTo(stackInSlot, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, INVENTORY_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stackInSlot.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        broadcastChanges();
        return moved;
    }

    private static int getInputTargetSlot(ItemStack stack) {
        if (isValidInk(stack)) {
            return INK_SLOT;
        }
        if (isValidWandBase(stack)) {
            return WAND_BASE_SLOT;
        }
        if (isValidFocus(stack)) {
            return FOCUS_SLOT;
        }
        return -1;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        resultContainer.removeItemNoUpdate(0);
        access.execute((targetLevel, targetPos) -> clearContainer(player, container));
    }
}
