package jp.aquafactory.apprenticecodex.block.spellcasterworkbench;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.ProcessingRecipeDenylist;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class SpellcasterWorkbenchMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_COUNT = 3;
    public static final int RESULT_SLOT = INPUT_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_START = RESULT_SLOT + 1;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_SLOT_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_SLOT_END = HOTBAR_SLOT_START + HOTBAR_SLOT_COUNT;
    private static final int SHIFT_FILL_FLAG = 1 << 30;

    private static final int[] INPUT_SLOT_X = {20, 40, 20};
    private static final int[] INPUT_SLOT_Y = {23, 33, 43};
    private static final int RESULT_SLOT_X = 81;
    private static final int RESULT_SLOT_Y = 33;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final Inventory playerInventory;
    private final ContainerLevelAccess access;
    private final DataSlot selectedIconIndex = DataSlot.standalone();
    private final Container container = new SimpleContainer(INPUT_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            SpellcasterWorkbenchMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer resultContainer = new ResultContainer();

    private RecipeManager cachedRecipeManager;
    private List<RecipeSelection> selectableRecipeGroups = List.of();
    private long lastCraftSoundTime;

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SpellcasterWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MenuRegistry.SPELLCASTER_WORKBENCH.get(), containerId);
        this.playerInventory = inventory;
        this.access = access;

        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            addSlot(new Slot(container, slotIndex, INPUT_SLOT_X[slotIndex], INPUT_SLOT_Y[slotIndex]));
        }
        addSlot(new Slot(resultContainer, 0, RESULT_SLOT_X, RESULT_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(@NotNull Player player) {
                return !SpellcasterWorkbenchMenu.this.getActiveResult().isEmpty();
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                SpellcasterWorkbenchMenu.this.handleResultTake(player, stack.copy());
                super.onTake(player, stack);
            }
        });

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }

        addDataSlot(selectedIconIndex);
        selectedIconIndex.set(-1);
    }

    public static int encodeRecipeButtonId(int iconIndex, boolean shiftDown) {
        return shiftDown ? iconIndex | SHIFT_FILL_FLAG : iconIndex;
    }

    public @NotNull List<ItemStack> getSelectableIcons() {
        return getSelectableRecipeGroups().stream()
                .map(RecipeSelection::icon)
                .map(ItemStack::copy)
                .toList();
    }

    public int getSelectedIconIndex() {
        return selectedIconIndex.get();
    }

    public boolean isBlockedByArchivistsGrimoireMaxSlotReached() {
        return getBlockedGrimoireUpgradeReason() == GrimoireUpgradeBlockReason.MAX_SLOT_REACHED;
    }

    public boolean isBlockedBySpellThrowableCardCantImbue() {
        return getActiveDynamicCraft() == null && hasInvalidDynamicCardImbue();
    }

    public boolean isResultBlocked() {
        return isBlockedByArchivistsGrimoireMaxSlotReached()
                || isBlockedBySpellThrowableCardCantImbue();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        var iconIndex = buttonId & ~SHIFT_FILL_FLAG;
        var fillAll = (buttonId & SHIFT_FILL_FLAG) != 0;
        if (!isValidIconIndex(iconIndex)) {
            return false;
        }

        if (!player.level().isClientSide) {
            handleRecipeSelection(player, iconIndex, fillAll);
        } else {
            selectedIconIndex.set(iconIndex);
        }
        setupResultSlot();
        return true;
    }

    public boolean hasInputItem() {
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            if (slots.get(slotIndex).hasItem()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.SPELLCASTER_WORKBENCH.get());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        setupResultSlot();
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (slotIndex == RESULT_SLOT) {
            var activeResult = getActiveResult();
            if (activeResult.isEmpty()) {
                return ItemStack.EMPTY;
            }

            var craftedStack = activeResult.copy();
            var craftedCopy = craftedStack.copy();
            if (!moveItemStackTo(craftedStack, PLAYER_INVENTORY_START, HOTBAR_SLOT_END, true) || !craftedStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            handleResultTake(player, craftedCopy);
            return craftedCopy;
        }

        var stack = slot.getItem();
        var copy = stack.copy();
        if (slotIndex < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, INPUT_SLOT_COUNT, false)) {
            if (slotIndex < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        resultContainer.removeItemNoUpdate(0);
        access.execute((level, pos) -> clearContainer(player, container));
    }

    private void handleRecipeSelection(Player player, int iconIndex, boolean fillAll) {
        var previousActiveRecipe = getActiveRecipe();
        var previousActiveDynamicCraft = getActiveDynamicCraft();
        var selection = getSelectableRecipeGroups().get(iconIndex);
        var appendToExisting = previousActiveRecipe != null
                && ItemStack.isSameItemSameTags(previousActiveRecipe.getPrimaryResultTemplate(), selection.icon());
        var appendToExistingDynamic = previousActiveDynamicCraft != null
                && selection.dynamicRecipe() != null
                && ItemStack.isSameItemSameTags(previousActiveDynamicCraft.resultTemplate(), selection.icon());

        selectedIconIndex.set(iconIndex);
        if (appendToExisting) {
            var targetSlots = previousActiveRecipe.findMatchingSlots(container);
            if (targetSlots != null) {
                moveRecipeBatchesToInput(previousActiveRecipe, targetSlots, fillAll);
            }
            return;
        }
        if (appendToExistingDynamic) {
            moveDynamicRecipeBatchesToInput(
                    previousActiveDynamicCraft.group(),
                    previousActiveDynamicCraft.variant(),
                    previousActiveDynamicCraft.matchedSlots(),
                    fillAll
            );
            return;
        }

        returnAllInputs(player);
        if (selection.dynamicRecipe() != null) {
            for (var variant : selection.dynamicRecipe().variants()) {
                var targetSlots = new int[]{0, 1, 2};
                if (!tryMoveDynamicRecipeBatchToInput(selection.dynamicRecipe(), variant, targetSlots)) {
                    continue;
                }

                if (fillAll) {
                    while (tryMoveDynamicRecipeBatchToInput(selection.dynamicRecipe(), variant, targetSlots)) {
                        // シフト時はスクロールを残したまま、消費素材だけ積める範囲で追加入力する。
                    }
                }
                return;
            }
            return;
        }

        for (var recipe : selection.recipes()) {
            var targetSlots = new int[]{0, 1, 2};
            if (!tryMoveRecipeBatchToInput(recipe, targetSlots)) {
                continue;
            }

            if (fillAll) {
                while (tryMoveRecipeBatchToInput(recipe, targetSlots)) {
                    // シフト時は同一レシピで積めるだけ材料を集める。
                }
            }
            return;
        }
    }

    private void moveRecipeBatchesToInput(SpellcasterWorkbenchRecipe recipe, int[] targetSlots, boolean fillAll) {
        if (!tryMoveRecipeBatchToInput(recipe, targetSlots)) {
            return;
        }

        if (!fillAll) {
            return;
        }

        while (tryMoveRecipeBatchToInput(recipe, targetSlots)) {
            // シフト時は現在成立しているレシピを崩さずに追加入力する。
        }
    }

    private void moveDynamicRecipeBatchesToInput(
            DynamicRecipeGroup group,
            DynamicRecipeVariant variant,
            int[] targetSlots,
            boolean fillAll
    ) {
        if (!tryMoveDynamicRecipeBatchToInput(group, variant, targetSlots)) {
            return;
        }

        if (!fillAll) {
            return;
        }

        while (tryMoveDynamicRecipeBatchToInput(group, variant, targetSlots)) {
            // シフト時は現在成立している動的レシピを崩さずに追加入力する。
        }
    }

    private boolean tryMoveDynamicRecipeBatchToInput(DynamicRecipeGroup group, DynamicRecipeVariant variant, int[] targetSlots) {
        var transferPlan = planDynamicRecipeBatchTransfer(group, variant, targetSlots);
        if (transferPlan == null) {
            return false;
        }

        applyTransferPlan(transferPlan);
        return true;
    }

    private @Nullable List<PlannedTransfer> planDynamicRecipeBatchTransfer(
            DynamicRecipeGroup group,
            DynamicRecipeVariant variant,
            int[] targetSlots
    ) {
        var ingredients = variant.createIngredients(group.targetItem());
        if (targetSlots.length != ingredients.size()) {
            return null;
        }

        var sources = collectInventorySources();
        var transfers = new ArrayList<PlannedTransfer>();
        if (!planDynamicIngredientTransfers(ingredients, targetSlots, 0, sources, transfers)) {
            return null;
        }
        return List.copyOf(transfers);
    }

    private boolean planDynamicIngredientTransfers(
            List<DynamicIngredient> ingredients,
            int[] targetSlots,
            int ingredientIndex,
            List<InventorySourceState> sources,
            List<PlannedTransfer> transfers
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        var ingredient = ingredients.get(ingredientIndex);
        var targetSlotIndex = targetSlots[ingredientIndex];
        var targetStack = container.getItem(targetSlotIndex);
        if (!targetStack.isEmpty() && !ingredient.test(targetStack)) {
            return false;
        }

        if (!ingredient.consumed() && !targetStack.isEmpty()
                && planDynamicIngredientTransfers(ingredients, targetSlots, ingredientIndex + 1, sources, transfers)) {
            return true;
        }

        var prototypeCandidates = collectDynamicPrototypeCandidates(ingredient, targetStack, sources);
        for (var prototypeCandidate : prototypeCandidates) {
            var compatibleSources = new ArrayList<InventorySourceState>();
            var availableCount = 0;
            for (var source : sources) {
                if (source.remainingCount() <= 0
                        || !ingredient.test(source.stack())
                        || !canStacksMerge(prototypeCandidate.stack(), source.stack())) {
                    continue;
                }

                compatibleSources.add(source);
                availableCount += source.remainingCount();
            }

            var maxAcceptableCount = targetStack.isEmpty()
                    ? prototypeCandidate.stack().getMaxStackSize()
                    : targetStack.getMaxStackSize() - targetStack.getCount();
            if (availableCount < ingredient.count() || maxAcceptableCount < ingredient.count()) {
                continue;
            }

            var snapshot = new ArrayList<ReservedTransfer>();
            var remainingNeed = ingredient.count();
            for (var source : compatibleSources) {
                if (remainingNeed <= 0) {
                    break;
                }

                var movedCount = Math.min(source.remainingCount(), remainingNeed);
                if (movedCount <= 0) {
                    continue;
                }

                source.remove(movedCount);
                snapshot.add(new ReservedTransfer(source, movedCount));
                transfers.add(new PlannedTransfer(source.menuSlotIndex(), targetSlotIndex, movedCount));
                remainingNeed -= movedCount;
            }

            if (remainingNeed <= 0
                    && planDynamicIngredientTransfers(ingredients, targetSlots, ingredientIndex + 1, sources, transfers)) {
                return true;
            }

            rollbackReservedTransfers(snapshot, transfers);
        }

        return false;
    }

    private @NotNull List<InventorySourceState> collectDynamicPrototypeCandidates(
            DynamicIngredient ingredient,
            ItemStack targetStack,
            List<InventorySourceState> sources
    ) {
        var candidates = new ArrayList<InventorySourceState>();
        for (var source : sources) {
            if (source.remainingCount() <= 0 || !ingredient.test(source.stack())) {
                continue;
            }
            if (!targetStack.isEmpty() && !canStacksMerge(targetStack, source.stack())) {
                continue;
            }

            var alreadyAdded = false;
            for (var candidate : candidates) {
                if (canStacksMerge(candidate.stack(), source.stack())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                candidates.add(source);
            }
        }
        return candidates;
    }

    private boolean tryMoveRecipeBatchToInput(SpellcasterWorkbenchRecipe recipe, int[] targetSlots) {
        var transferPlan = planRecipeBatchTransfer(recipe, targetSlots);
        if (transferPlan == null) {
            return false;
        }

        applyTransferPlan(transferPlan);
        return true;
    }

    private @Nullable List<PlannedTransfer> planRecipeBatchTransfer(SpellcasterWorkbenchRecipe recipe, int[] targetSlots) {
        var ingredients = recipe.getSizedIngredients();
        if (targetSlots.length != ingredients.size()) {
            return null;
        }

        var sources = collectInventorySources();
        var transfers = new ArrayList<PlannedTransfer>();
        if (!planIngredientTransfers(ingredients, targetSlots, 0, sources, transfers)) {
            return null;
        }
        return List.copyOf(transfers);
    }

    private boolean planIngredientTransfers(
            List<SpellcasterWorkbenchRecipe.SizedIngredient> ingredients,
            int[] targetSlots,
            int ingredientIndex,
            List<InventorySourceState> sources,
            List<PlannedTransfer> transfers
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        var ingredient = ingredients.get(ingredientIndex);
        var targetSlotIndex = targetSlots[ingredientIndex];
        var targetStack = container.getItem(targetSlotIndex);
        if (!targetStack.isEmpty() && !ingredient.test(targetStack)) {
            return false;
        }

        var prototypeCandidates = collectPrototypeCandidates(ingredient, targetStack, sources);
        for (var prototypeCandidate : prototypeCandidates) {
            var compatibleSources = new ArrayList<InventorySourceState>();
            var availableCount = 0;
            for (var source : sources) {
                if (source.remainingCount() <= 0
                        || !ingredient.ingredient().test(source.stack())
                        || !canStacksMerge(prototypeCandidate.stack(), source.stack())) {
                    continue;
                }

                compatibleSources.add(source);
                availableCount += source.remainingCount();
            }

            var maxAcceptableCount = targetStack.isEmpty()
                    ? prototypeCandidate.stack().getMaxStackSize()
                    : targetStack.getMaxStackSize() - targetStack.getCount();
            if (availableCount < ingredient.count() || maxAcceptableCount < ingredient.count()) {
                continue;
            }

            var snapshot = new ArrayList<ReservedTransfer>();
            var remainingNeed = ingredient.count();
            for (var source : compatibleSources) {
                if (remainingNeed <= 0) {
                    break;
                }

                var movedCount = Math.min(source.remainingCount(), remainingNeed);
                if (movedCount <= 0) {
                    continue;
                }

                source.remove(movedCount);
                snapshot.add(new ReservedTransfer(source, movedCount));
                transfers.add(new PlannedTransfer(source.menuSlotIndex(), targetSlotIndex, movedCount));
                remainingNeed -= movedCount;
            }

            if (remainingNeed <= 0
                    && planIngredientTransfers(ingredients, targetSlots, ingredientIndex + 1, sources, transfers)) {
                return true;
            }

            rollbackReservedTransfers(snapshot, transfers);
        }

        return false;
    }

    private @NotNull List<InventorySourceState> collectPrototypeCandidates(
            SpellcasterWorkbenchRecipe.SizedIngredient ingredient,
            ItemStack targetStack,
            List<InventorySourceState> sources
    ) {
        var candidates = new ArrayList<InventorySourceState>();
        for (var source : sources) {
            if (source.remainingCount() <= 0 || !ingredient.ingredient().test(source.stack())) {
                continue;
            }
            if (!targetStack.isEmpty() && !canStacksMerge(targetStack, source.stack())) {
                continue;
            }

            var alreadyAdded = false;
            for (var candidate : candidates) {
                if (canStacksMerge(candidate.stack(), source.stack())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                candidates.add(source);
            }
        }
        return candidates;
    }

    private void rollbackReservedTransfers(List<ReservedTransfer> snapshot, List<PlannedTransfer> transfers) {
        for (var reservedTransfer : snapshot) {
            reservedTransfer.source().restore(reservedTransfer.count());
            transfers.remove(transfers.size() - 1);
        }
    }

    private @NotNull List<InventorySourceState> collectInventorySources() {
        var sources = new ArrayList<InventorySourceState>();
        for (var slotIndex = PLAYER_INVENTORY_START; slotIndex < HOTBAR_SLOT_END; ++slotIndex) {
            var stack = slots.get(slotIndex).getItem();
            if (!stack.isEmpty()) {
                sources.add(new InventorySourceState(slotIndex, stack.copy(), stack.getCount()));
            }
        }
        return sources;
    }

    private void applyTransferPlan(List<PlannedTransfer> transferPlan) {
        for (var transfer : transferPlan) {
            var sourceSlot = slots.get(transfer.sourceSlotIndex());
            var movedStack = sourceSlot.remove(transfer.count());
            if (movedStack.isEmpty()) {
                continue;
            }

            var targetStack = container.getItem(transfer.targetSlotIndex());
            if (targetStack.isEmpty()) {
                container.setItem(transfer.targetSlotIndex(), movedStack);
            } else {
                targetStack.grow(movedStack.getCount());
                container.setChanged();
            }
            sourceSlot.setChanged();
        }
    }

    private void returnAllInputs(Player player) {
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            var stack = container.removeItemNoUpdate(slotIndex);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        container.setChanged();
    }

    private void handleResultTake(Player player, ItemStack craftedStack) {
        var activeRecipe = getActiveRecipe();
        if (activeRecipe != null) {
            var matchedSlots = activeRecipe.findMatchingSlots(container);
            if (matchedSlots == null) {
                return;
            }

            craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
            consumeRecipeIngredients(player, activeRecipe, matchedSlots);

            var resultTemplates = activeRecipe.getResultTemplates();
            for (var index = 1; index < resultTemplates.size(); ++index) {
                player.getInventory().placeItemBackInInventory(resultTemplates.get(index).copy());
            }

            playCraftSound();
            setupResultSlot();
            return;
        }

        var dynamicCraft = getActiveDynamicCraft();
        if (dynamicCraft != null) {
            craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
            if (!consumeDynamicRecipeIngredients(dynamicCraft)) {
                return;
            }

            playCraftSound();
            setupResultSlot();
            return;
        }

        var grimoireUpgrade = getActiveGrimoireUpgrade();
        if (grimoireUpgrade != null) {
            craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
            if (!consumeGrimoireUpgradeInputs(grimoireUpgrade)) {
                return;
            }

            playCraftSound();
            setupResultSlot();
            return;
        }

        var flaskToggle = getActiveFlaskParticleToggle();
        if (flaskToggle == null) {
            return;
        }

        craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
        if (!consumeFlaskForParticleToggle(flaskToggle.sourceSlotIndex())) {
            return;
        }

        playCraftSound();
        setupResultSlot();
    }

    private boolean consumeDynamicRecipeIngredients(DynamicCraft craft) {
        var matchedSlots = craft.matchedSlots();
        if (matchedSlots.length < 2) {
            return false;
        }

        if (!hasInputCount(matchedSlots[0], craft.variant().baseCount())
                || !hasInputCount(matchedSlots[1], craft.variant().catalystCount())) {
            return false;
        }

        shrinkInput(matchedSlots[0], craft.variant().baseCount());
        shrinkInput(matchedSlots[1], craft.variant().catalystCount());
        container.setChanged();
        return true;
    }

    private void consumeRecipeIngredients(Player player, SpellcasterWorkbenchRecipe recipe, int[] matchedSlots) {
        var remainderStacks = new ArrayList<ItemStack>();
        var ingredients = recipe.getSizedIngredients();
        for (var ingredientIndex = 0; ingredientIndex < ingredients.size(); ++ingredientIndex) {
            var slotIndex = matchedSlots[ingredientIndex];
            var inputStack = container.getItem(slotIndex);
            if (inputStack.isEmpty()) {
                continue;
            }

            var consumeCount = ingredients.get(ingredientIndex).count();
            if (inputStack.hasCraftingRemainingItem()) {
                for (var count = 0; count < consumeCount; ++count) {
                    remainderStacks.add(inputStack.getCraftingRemainingItem().copy());
                }
            }

            inputStack.shrink(consumeCount);
            if (inputStack.isEmpty()) {
                container.setItem(slotIndex, ItemStack.EMPTY);
            }
        }

        container.setChanged();
        for (var remainderStack : remainderStacks) {
            if (!remainderStack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(remainderStack);
            }
        }
    }

    private boolean hasInputCount(int slotIndex, int count) {
        if (slotIndex < 0 || slotIndex >= INPUT_SLOT_COUNT) {
            return false;
        }

        var stack = container.getItem(slotIndex);
        return !stack.isEmpty() && stack.getCount() >= count;
    }

    private boolean shrinkInput(int slotIndex, int count) {
        if (slotIndex < 0 || slotIndex >= INPUT_SLOT_COUNT) {
            return false;
        }

        var stack = container.getItem(slotIndex);
        if (stack.isEmpty() || stack.getCount() < count) {
            return false;
        }

        stack.shrink(count);
        if (stack.isEmpty()) {
            container.setItem(slotIndex, ItemStack.EMPTY);
        }
        return true;
    }

    private void playCraftSound() {
        access.execute((level, pos) -> {
            var gameTime = level.getGameTime();
            if (lastCraftSoundTime == gameTime) {
                return;
            }

            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            lastCraftSoundTime = gameTime;
        });
    }

    private void setupResultSlot() {
        if (!isValidIconIndex(selectedIconIndex.get())) {
            selectedIconIndex.set(-1);
        }

        var result = getActiveResult();
        if (!ItemStack.matches(result, resultContainer.getItem(0))) {
            resultContainer.setItem(0, result);
        }
        broadcastChanges();
    }

    private @Nullable SpellcasterWorkbenchRecipe getActiveRecipe() {
        var selection = getSelectedSelection();
        if (selection == null) {
            return null;
        }

        var level = playerInventory.player.level();
        for (var recipe : selection.recipes()) {
            if (recipe.matches(container, level)) {
                return recipe;
            }
        }
        return null;
    }

    private @Nullable GrimoireUpgrade getActiveGrimoireUpgrade() {
        return buildGrimoireUpgrade();
    }

    private @Nullable FlaskParticleToggle getActiveFlaskParticleToggle() {
        return buildFlaskParticleToggle();
    }

    private @Nullable DynamicCraft getActiveDynamicCraft() {
        var selection = getSelectedSelection();
        if (selection != null && selection.dynamicRecipe() != null) {
            return buildDynamicCraft(selection.dynamicRecipe());
        }

        for (var group : buildDynamicRecipeGroups()) {
            var craft = buildDynamicCraft(group);
            if (craft != null) {
                return craft;
            }
        }
        return null;
    }

    private @NotNull ItemStack getActiveResult() {
        var activeRecipe = getActiveRecipe();
        if (activeRecipe != null) {
            return activeRecipe.getPrimaryResultTemplate();
        }

        var dynamicCraft = getActiveDynamicCraft();
        if (dynamicCraft != null) {
            return dynamicCraft.resultTemplate().copy();
        }

        var grimoireUpgrade = getActiveGrimoireUpgrade();
        if (grimoireUpgrade != null) {
            return grimoireUpgrade.resultTemplate().copy();
        }

        var flaskToggle = getActiveFlaskParticleToggle();
        return flaskToggle == null ? ItemStack.EMPTY : flaskToggle.resultTemplate().copy();
    }

    private @Nullable RecipeSelection getSelectedSelection() {
        if (!isValidIconIndex(selectedIconIndex.get())) {
            return null;
        }
        return getSelectableRecipeGroups().get(selectedIconIndex.get());
    }

    private boolean isValidIconIndex(int index) {
        return index >= 0 && index < getSelectableRecipeGroups().size();
    }

    private @NotNull List<RecipeSelection> getSelectableRecipeGroups() {
        var level = playerInventory.player.level();
        var recipeManager = level.getRecipeManager();
        if (cachedRecipeManager != recipeManager) {
            cachedRecipeManager = recipeManager;
            selectableRecipeGroups = buildSelectableRecipeGroups(recipeManager);
        }
        return selectableRecipeGroups;
    }

    private static @NotNull List<RecipeSelection> buildSelectableRecipeGroups(RecipeManager recipeManager) {
        var sortedRecipes = new ArrayList<>(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).stream()
                .filter(ProcessingRecipeDenylist::isAllowed)
                .toList());
        sortedRecipes.sort(Comparator
                .comparingInt(SpellcasterWorkbenchRecipe::getPriority)
                .thenComparing(recipe -> itemKey(recipe.getPrimaryResultTemplate()))
                .thenComparing(recipe -> recipe.getId().toString()));

        var groupedSelections = new ArrayList<MutableRecipeSelection>();
        for (var recipe : sortedRecipes) {
            var icon = recipe.getPrimaryResultTemplate();
            var existingSelection = findSelection(groupedSelections, icon);
            if (existingSelection == null) {
                existingSelection = new MutableRecipeSelection(icon.copy());
                groupedSelections.add(existingSelection);
            }
            existingSelection.recipes().add(recipe);
        }

        var selections = new ArrayList<RecipeSelection>();
        for (var selection : groupedSelections) {
            selections.add(new RecipeSelection(selection.icon(), List.copyOf(selection.recipes()), null));
        }
        for (var dynamicRecipe : buildDynamicRecipeGroups()) {
            selections.add(new RecipeSelection(dynamicRecipe.icon().copy(), List.of(), dynamicRecipe));
        }
        return List.copyOf(selections);
    }

    private static @NotNull List<DynamicRecipeGroup> buildDynamicRecipeGroups() {
        var invokeCraftCount = ApprenticeCodexServerConfig.spellInvokeCardCraftCount();
        var autonomyCraftCount = ApprenticeCodexServerConfig.spellAutonomyCardCraftCount();
        return List.of(
                new DynamicRecipeGroup(
                        ItemRegistry.SPELL_INVOKE_CARD.get().getDefaultInstance(),
                        (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_INVOKE_CARD.get(),
                        List.of(
                                new DynamicRecipeVariant(
                                        stack -> stack.is(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                                        invokeCraftCount,
                                        stack -> stack.is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                                        1,
                                        invokeCraftCount
                                ),
                                new DynamicRecipeVariant(
                                        stack -> stack.is(ItemRegistry.SPELL_INVOKE_CARD.get()),
                                        invokeCraftCount,
                                        stack -> stack.is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                                        1,
                                        invokeCraftCount
                                )
                        )
                ),
                new DynamicRecipeGroup(
                        ItemRegistry.SPELL_AUTONOMY_CARD.get().getDefaultInstance(),
                        (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        List.of(
                                new DynamicRecipeVariant(
                                        stack -> stack.is(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                                        autonomyCraftCount,
                                        stack -> stack.is(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                                        1,
                                        autonomyCraftCount
                                ),
                                new DynamicRecipeVariant(
                                        stack -> stack.is(ItemRegistry.SPELL_AUTONOMY_CARD.get()),
                                        autonomyCraftCount,
                                        stack -> stack.is(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                                        1,
                                        autonomyCraftCount
                                )
                        )
                )
        );
    }

    private @Nullable DynamicCraft buildDynamicCraft(DynamicRecipeGroup group) {
        for (var variant : group.variants()) {
            var ingredients = variant.createIngredients(group.targetItem());
            var matchedSlots = findMatchingDynamicSlots(ingredients);
            if (matchedSlots == null) {
                continue;
            }

            var spellData = getScrollSpellData(container.getItem(matchedSlots[2]));
            if (spellData == SpellData.EMPTY || !group.targetItem().canImbueSpell(spellData)) {
                continue;
            }

            var resultStack = group.targetItem().createArcaneAnvilImbueResult(
                    new ItemStack(group.targetItem(), variant.outputCount()),
                    spellData
            );
            resultStack.setCount(variant.outputCount());
            return new DynamicCraft(group, variant, matchedSlots, resultStack);
        }
        return null;
    }

    private boolean hasInvalidDynamicCardImbue() {
        var selection = getSelectedSelection();
        if (selection != null && selection.dynamicRecipe() != null) {
            return hasInvalidDynamicCardImbue(selection.dynamicRecipe());
        }

        for (var group : buildDynamicRecipeGroups()) {
            if (hasInvalidDynamicCardImbue(group)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInvalidDynamicCardImbue(DynamicRecipeGroup group) {
        for (var variant : group.variants()) {
            var matchedSlots = findMatchingDynamicSlots(variant.createLooseIngredients());
            if (matchedSlots == null) {
                continue;
            }

            var spellData = getScrollSpellData(container.getItem(matchedSlots[2]));
            return spellData == SpellData.EMPTY || !group.targetItem().canImbueSpell(spellData);
        }
        return false;
    }

    private @Nullable int[] findMatchingDynamicSlots(List<DynamicIngredient> ingredients) {
        if (ingredients.size() != INPUT_SLOT_COUNT) {
            return null;
        }

        var usedSlots = new boolean[INPUT_SLOT_COUNT];
        var matchedSlots = new int[INPUT_SLOT_COUNT];
        Arrays.fill(matchedSlots, -1);
        return matchesDynamicUnordered(ingredients, 0, usedSlots, matchedSlots) ? matchedSlots : null;
    }

    private boolean matchesDynamicUnordered(
            List<DynamicIngredient> ingredients,
            int ingredientIndex,
            boolean[] usedSlots,
            int[] matchedSlots
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        var ingredient = ingredients.get(ingredientIndex);
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            var stack = container.getItem(slotIndex);
            if (usedSlots[slotIndex] || stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            usedSlots[slotIndex] = true;
            matchedSlots[ingredientIndex] = slotIndex;
            if (matchesDynamicUnordered(ingredients, ingredientIndex + 1, usedSlots, matchedSlots)) {
                return true;
            }
            usedSlots[slotIndex] = false;
            matchedSlots[ingredientIndex] = -1;
        }

        return false;
    }

    private static @NotNull SpellData getScrollSpellData(@NotNull ItemStack scrollStack) {
        if (scrollStack.isEmpty() || !(scrollStack.getItem() instanceof Scroll)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        if (scrollContainer == null) {
            return SpellData.EMPTY;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        // @NotNullなのにnullで返るケースがあるため、Workbench 側でも防御する。
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private @Nullable FlaskParticleToggle buildFlaskParticleToggle() {
        var sourceSlotIndex = findSingleOccupiedInputSlot();
        if (sourceSlotIndex < 0) {
            return null;
        }

        var inputStack = container.getItem(sourceSlotIndex);
        if (!(inputStack.getItem() instanceof SpellcastersFlask)) {
            return null;
        }

        var toggledStack = SpellcastersFlask.copyWithToggledEffectParticles(inputStack);
        if (toggledStack.isEmpty()) {
            return null;
        }

        return new FlaskParticleToggle(sourceSlotIndex, toggledStack);
    }

    private @Nullable GrimoireUpgradeBlockReason getBlockedGrimoireUpgradeReason() {
        var context = getGrimoireUpgradeContext();
        return context == null ? null : context.blockReason();
    }

    private @Nullable GrimoireUpgrade buildGrimoireUpgrade() {
        var context = getGrimoireUpgradeContext();
        if (context == null || context.blockReason() != null) {
            return null;
        }

        var resultStack = ArchivistsGrimoire.createUpgradeResult(container.getItem(context.grimoireSlotIndex()));
        if (resultStack.isEmpty()) {
            return null;
        }
        return new GrimoireUpgrade(context.grimoireSlotIndex(), context.catalystSlotIndex(), context.materialSlotIndex(), resultStack);
    }

    private @Nullable GrimoireUpgradeContext getGrimoireUpgradeContext() {
        var grimoireSlotIndex = -1;
        var catalystSlotIndex = -1;
        var materialSlotIndex = -1;
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            var inputStack = container.getItem(slotIndex);
            if (inputStack.isEmpty()) {
                return null;
            }

            if (inputStack.getItem() instanceof ArchivistsGrimoire) {
                if (grimoireSlotIndex >= 0) {
                    return null;
                }
                grimoireSlotIndex = slotIndex;
            } else if (inputStack.is(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS)) {
                if (catalystSlotIndex >= 0) {
                    return null;
                }
                catalystSlotIndex = slotIndex;
            } else if (inputStack.is(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS)) {
                if (materialSlotIndex >= 0) {
                    return null;
                }
                materialSlotIndex = slotIndex;
            } else {
                return null;
            }
        }

        if (grimoireSlotIndex < 0 || catalystSlotIndex < 0 || materialSlotIndex < 0) {
            return null;
        }

        var grimoireStack = container.getItem(grimoireSlotIndex);
        var blockReason = ArchivistsGrimoire.canUpgrade(grimoireStack) ? null : GrimoireUpgradeBlockReason.MAX_SLOT_REACHED;
        return new GrimoireUpgradeContext(grimoireSlotIndex, catalystSlotIndex, materialSlotIndex, blockReason);
    }

    private int findSingleOccupiedInputSlot() {
        var occupiedSlotIndex = -1;
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            if (container.getItem(slotIndex).isEmpty()) {
                continue;
            }
            if (occupiedSlotIndex >= 0) {
                return -1;
            }
            occupiedSlotIndex = slotIndex;
        }
        return occupiedSlotIndex;
    }

    private boolean consumeGrimoireUpgradeInputs(GrimoireUpgrade upgrade) {
        if (upgrade.grimoireSlotIndex() < 0 || upgrade.grimoireSlotIndex() >= INPUT_SLOT_COUNT
                || upgrade.catalystSlotIndex() < 0 || upgrade.catalystSlotIndex() >= INPUT_SLOT_COUNT
                || upgrade.materialSlotIndex() < 0 || upgrade.materialSlotIndex() >= INPUT_SLOT_COUNT) {
            return false;
        }

        var grimoireStack = container.getItem(upgrade.grimoireSlotIndex());
        var catalystStack = container.getItem(upgrade.catalystSlotIndex());
        var materialStack = container.getItem(upgrade.materialSlotIndex());
        if (!(grimoireStack.getItem() instanceof ArchivistsGrimoire)
                || !catalystStack.is(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS)
                || !materialStack.is(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS)
                || !ArchivistsGrimoire.canUpgrade(grimoireStack)) {
            return false;
        }

        grimoireStack.shrink(1);
        catalystStack.shrink(1);
        materialStack.shrink(1);
        if (grimoireStack.isEmpty()) {
            container.setItem(upgrade.grimoireSlotIndex(), ItemStack.EMPTY);
        }
        if (catalystStack.isEmpty()) {
            container.setItem(upgrade.catalystSlotIndex(), ItemStack.EMPTY);
        }
        if (materialStack.isEmpty()) {
            container.setItem(upgrade.materialSlotIndex(), ItemStack.EMPTY);
        }
        container.setChanged();
        return true;
    }

    private boolean consumeFlaskForParticleToggle(int sourceSlotIndex) {
        var inputStack = container.getItem(sourceSlotIndex);
        if (!(inputStack.getItem() instanceof SpellcastersFlask)) {
            return false;
        }

        inputStack.shrink(1);
        if (inputStack.isEmpty()) {
            container.setItem(sourceSlotIndex, ItemStack.EMPTY);
        }
        container.setChanged();
        return true;
    }

    private static @Nullable MutableRecipeSelection findSelection(List<MutableRecipeSelection> groupedSelections, ItemStack icon) {
        for (var selection : groupedSelections) {
            if (ItemStack.isSameItemSameTags(selection.icon(), icon)) {
                return selection;
            }
        }
        return null;
    }

    private static boolean canStacksMerge(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
    }

    private static String itemKey(ItemStack stack) {
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String tagKey = null;
        if (stack.getTag() != null) {
            tagKey = stack.hasTag() ? stack.getTag().toString() : "";
        }
        return (itemId == null ? "unknown" : itemId.toString()) + tagKey;
    }

    private record RecipeSelection(
            ItemStack icon,
            List<SpellcasterWorkbenchRecipe> recipes,
            @Nullable DynamicRecipeGroup dynamicRecipe
    ) {
    }

    private record DynamicRecipeGroup(
            ItemStack icon,
            AbstractSpellThrowableCardItem targetItem,
            List<DynamicRecipeVariant> variants
    ) {
    }

    private record DynamicRecipeVariant(
            Predicate<ItemStack> basePredicate,
            int baseCount,
            Predicate<ItemStack> catalystPredicate,
            int catalystCount,
            int outputCount
    ) {
        private @NotNull List<DynamicIngredient> createIngredients(AbstractSpellThrowableCardItem targetItem) {
            return List.of(
                    new DynamicIngredient(basePredicate, baseCount, true),
                    new DynamicIngredient(catalystPredicate, catalystCount, true),
                    new DynamicIngredient(stack -> {
                        var spellData = getScrollSpellData(stack);
                        return spellData != SpellData.EMPTY && targetItem.canImbueSpell(spellData);
                    }, 1, false)
            );
        }

        private @NotNull List<DynamicIngredient> createLooseIngredients() {
            return List.of(
                    new DynamicIngredient(basePredicate, baseCount, true),
                    new DynamicIngredient(catalystPredicate, catalystCount, true),
                    new DynamicIngredient(stack -> stack.getItem() instanceof Scroll, 1, false)
            );
        }
    }

    private record DynamicIngredient(
            Predicate<ItemStack> predicate,
            int count,
            boolean consumed
    ) {
        private DynamicIngredient {
            count = Math.max(1, count);
        }

        private boolean test(ItemStack stack) {
            return predicate.test(stack) && stack.getCount() >= count;
        }
    }

    private record DynamicCraft(
            DynamicRecipeGroup group,
            DynamicRecipeVariant variant,
            int[] matchedSlots,
            ItemStack resultTemplate
    ) {
    }

    private record GrimoireUpgrade(
            int grimoireSlotIndex,
            int catalystSlotIndex,
            int materialSlotIndex,
            ItemStack resultTemplate
    ) {
    }

    private enum GrimoireUpgradeBlockReason {
        MAX_SLOT_REACHED
    }

    private record GrimoireUpgradeContext(
            int grimoireSlotIndex,
            int catalystSlotIndex,
            int materialSlotIndex,
            @Nullable GrimoireUpgradeBlockReason blockReason
    ) {
    }

    private record FlaskParticleToggle(
            int sourceSlotIndex,
            ItemStack resultTemplate
    ) {
    }

    private record MutableRecipeSelection(
            ItemStack icon,
            List<SpellcasterWorkbenchRecipe> recipes
    ) {
        private MutableRecipeSelection(ItemStack icon) {
            this(icon, new ArrayList<>());
        }
    }

    private static final class InventorySourceState {
        private final int menuSlotIndex;
        private final ItemStack stack;
        private int remainingCount;

        private InventorySourceState(int menuSlotIndex, ItemStack stack, int remainingCount) {
            this.menuSlotIndex = menuSlotIndex;
            this.stack = stack;
            this.remainingCount = remainingCount;
        }

        private int menuSlotIndex() {
            return menuSlotIndex;
        }

        private ItemStack stack() {
            return stack;
        }

        private int remainingCount() {
            return remainingCount;
        }

        private void remove(int count) {
            remainingCount -= count;
        }

        private void restore(int count) {
            remainingCount += count;
        }
    }

    private record PlannedTransfer(
            int sourceSlotIndex,
            int targetSlotIndex,
            int count
    ) {
    }

    private record ReservedTransfer(
            InventorySourceState source,
            int count
    ) {
    }
}
