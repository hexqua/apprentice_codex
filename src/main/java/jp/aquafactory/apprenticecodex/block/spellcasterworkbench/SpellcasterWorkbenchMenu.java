package jp.aquafactory.apprenticecodex.block.spellcasterworkbench;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Comparator;
import java.util.List;

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

    public boolean isBlockedByDefaultSpellExtraction() {
        return getBlockedSpellExtractionReason() == SpellExtractionBlockReason.DEFAULT_SPELL;
    }

    public boolean isBlockedByUnsupportedSpellExtraction() {
        return getBlockedSpellExtractionReason() == SpellExtractionBlockReason.NOT_ALLOWED;
    }

    public boolean isBlockedByMissingSpellExtraction() {
        return getBlockedSpellExtractionReason() == SpellExtractionBlockReason.MISSING_SPELL;
    }

    public boolean isWarnedByUnsupportedEmptySpellExtraction() {
        return getBlockedSpellExtractionReason() == SpellExtractionBlockReason.EMPTY_NOT_ALLOWED;
    }

    public boolean isSpellExtractionBlocked() {
        return getBlockedSpellExtractionReason() != null;
    }

    public boolean isBlockedByUnsupportedWorkbenchImbue() {
        return getBlockedWorkbenchImbueReason() == WorkbenchImbueBlockReason.UNSUPPORTED_EQUIPMENT;
    }

    public boolean isResultBlocked() {
        return isSpellExtractionBlocked() || isBlockedByUnsupportedWorkbenchImbue();
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
        var selection = getSelectableRecipeGroups().get(iconIndex);
        var appendToExisting = previousActiveRecipe != null
                && ItemStack.isSameItemSameTags(previousActiveRecipe.getPrimaryResultTemplate(), selection.icon());

        selectedIconIndex.set(iconIndex);
        if (appendToExisting) {
            var targetSlots = previousActiveRecipe.findMatchingSlots(container);
            if (targetSlots != null) {
                moveRecipeBatchesToInput(previousActiveRecipe, targetSlots, fillAll);
            }
            return;
        }

        returnAllInputs(player);
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

        var workbenchImbue = getActiveWorkbenchImbue();
        if (workbenchImbue != null) {
            craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
            if (!consumeWorkbenchImbueInputs(workbenchImbue.sourceSlotIndex(), workbenchImbue.scrollSlotIndex())) {
                return;
            }

            playCraftSound();
            setupResultSlot();
            return;
        }

        var extraction = getActiveSpellExtraction();
        if (extraction != null) {
            craftedStack.onCraftedBy(player.level(), player, craftedStack.getCount());
            if (!removeSpellFromExtractableItem(extraction.sourceSlotIndex())) {
                return;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementTools.award(serverPlayer,
                        AdvancementTools.EXTRACT_SPELLCASTER_GUN_SCROLL,
                        AdvancementTools.EXTRACT_SPELLCASTER_GUN_SCROLL_CRITERION);
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

    private @Nullable SpellExtraction getActiveSpellExtraction() {
        return buildSpellExtraction();
    }

    private @Nullable WorkbenchImbue getActiveWorkbenchImbue() {
        return buildWorkbenchImbue();
    }

    private @Nullable FlaskParticleToggle getActiveFlaskParticleToggle() {
        return buildFlaskParticleToggle();
    }

    private @NotNull ItemStack getActiveResult() {
        var activeRecipe = getActiveRecipe();
        if (activeRecipe != null) {
            return activeRecipe.getPrimaryResultTemplate();
        }

        var workbenchImbue = getActiveWorkbenchImbue();
        if (workbenchImbue != null) {
            return workbenchImbue.resultTemplate().copy();
        }

        var extraction = getActiveSpellExtraction();
        if (extraction != null) {
            return extraction.resultTemplate().copy();
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
        var sortedRecipes = new ArrayList<>(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()));
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

        return groupedSelections.stream()
                .map(selection -> new RecipeSelection(selection.icon(), List.copyOf(selection.recipes())))
                .toList();
    }

    private @Nullable WorkbenchImbue buildWorkbenchImbue() {
        var context = getWorkbenchImbueContext();
        if (context == null || context.blockReason() != null || context.spellImbueItem() == null
                || !hasAvailableSpellSlot(context.normalizedBaseStack())) {
            return null;
        }

        var spellImbueItem = context.spellImbueItem();
        var resultStack = spellImbueItem.createArcaneAnvilImbueResult(
                context.normalizedBaseStack(),
                context.spellData()
        );
        if (resultStack.isEmpty()) {
            return null;
        }

        resultStack.setCount(1);
        if (!canExtractWorkbenchImbuedSpell(spellImbueItem, resultStack, context.spellData())) {
            return null;
        }

        return new WorkbenchImbue(context.sourceSlotIndex(), context.scrollSlotIndex(), resultStack);
    }

    private @Nullable SpellExtraction buildSpellExtraction() {
        var extractionContext = getSpellExtractionContext();
        if (extractionContext == null || extractionContext.blockReason() != null) {
            return null;
        }

        var scrollStack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(extractionContext.spellData().getSpell(), extractionContext.spellData().getLevel(), scrollStack);
        return new SpellExtraction(extractionContext.sourceSlotIndex(), scrollStack);
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

    private @Nullable SpellExtractionBlockReason getBlockedSpellExtractionReason() {
        var extractionContext = getSpellExtractionContext();
        if (extractionContext != null) {
            return extractionContext.blockReason();
        }
        return getEmptySpellExtractionBlockReason();
    }

    private @Nullable WorkbenchImbueBlockReason getBlockedWorkbenchImbueReason() {
        var context = getWorkbenchImbueContext();
        return context == null ? null : context.blockReason();
    }

    private @Nullable WorkbenchImbueContext getWorkbenchImbueContext() {
        var sourceSlotIndex = -1;
        var scrollSlotIndex = -1;
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            var inputStack = container.getItem(slotIndex);
            if (inputStack.isEmpty()) {
                continue;
            }

            if (inputStack.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll) {
                if (scrollSlotIndex >= 0) {
                    return null;
                }
                scrollSlotIndex = slotIndex;
                continue;
            }

            if (sourceSlotIndex >= 0) {
                return null;
            }
            sourceSlotIndex = slotIndex;
        }

        if (sourceSlotIndex < 0 || scrollSlotIndex < 0) {
            return null;
        }

        var inputStack = container.getItem(sourceSlotIndex);
        var scrollStack = container.getItem(scrollSlotIndex);
        var scrollContainer = ISpellContainer.get(scrollStack);
        if (scrollContainer == null) {
            return null;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        var normalizedBaseStack = inputStack.copy();
        normalizedBaseStack.setCount(1);
        repairExtractablePresetSpellContainerIfNeeded(normalizedBaseStack);
        initializePresetSpellContainerIfNeeded(normalizedBaseStack);
        var spellImbueItem = inputStack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem
                ? restrictedSpellImbuableItem
                : null;

        if (spellImbueItem == null) {
            if (!ISpellContainer.isSpellContainer(normalizedBaseStack)) {
                return null;
            }

            // 他 MOD の Imbue 対象は制約を判断できないため、Workbench ではなく Arcane Anvil へ誘導する。
            return new WorkbenchImbueContext(
                    sourceSlotIndex,
                    scrollSlotIndex,
                    null,
                    normalizedBaseStack,
                    spellData,
                    WorkbenchImbueBlockReason.UNSUPPORTED_EQUIPMENT
            );
        }

        if (!spellImbueItem.canImbueSpell(spellData)) {
            return null;
        }

        spellImbueItem.normalizeImbuedSpellContainer(normalizedBaseStack);

        if (!canCreateExtractableWorkbenchImbue(spellImbueItem, normalizedBaseStack, spellData)) {
            return new WorkbenchImbueContext(
                    sourceSlotIndex,
                    scrollSlotIndex,
                    spellImbueItem,
                    normalizedBaseStack,
                    spellData,
                    WorkbenchImbueBlockReason.UNSUPPORTED_EQUIPMENT
            );
        }

        return new WorkbenchImbueContext(sourceSlotIndex, scrollSlotIndex, spellImbueItem, normalizedBaseStack, spellData, null);
    }

    private static boolean canCreateExtractableWorkbenchImbue(
            RestrictedSpellImbuableItem spellImbueItem,
            ItemStack normalizedBaseStack,
            SpellData spellData
    ) {
        if (!hasAvailableSpellSlot(normalizedBaseStack)) {
            return true;
        }

        var resultStack = spellImbueItem.createArcaneAnvilImbueResult(normalizedBaseStack, spellData);
        if (resultStack.isEmpty()) {
            return false;
        }

        return canExtractWorkbenchImbuedSpell(spellImbueItem, resultStack, spellData);
    }

    private static boolean canCreateAnyExtractableWorkbenchImbue(
            ItemStack stack,
            RestrictedSpellImbuableItem spellImbueItem
    ) {
        var probeStack = createEmptyWorkbenchImbueProbe(stack, spellImbueItem);
        if (!hasAvailableSpellSlot(probeStack)) {
            return false;
        }

        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            for (var level = spell.getMinLevel(); level <= spell.getMaxLevel(); ++level) {
                var spellData = new SpellData(spell, level);
                if (spellImbueItem.canImbueSpell(spellData)
                        && canCreateExtractableWorkbenchImbue(spellImbueItem, probeStack, spellData)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack createEmptyWorkbenchImbueProbe(
            ItemStack stack,
            RestrictedSpellImbuableItem spellImbueItem
    ) {
        var probeStack = stack.copy();
        probeStack.setCount(1);
        repairExtractablePresetSpellContainerIfNeeded(probeStack);
        initializePresetSpellContainerIfNeeded(probeStack);

        var spellContainer = ISpellContainer.get(probeStack);
        var spellSlotCount = spellContainer == null ? 1 : Math.max(1, spellContainer.getMaxSpellCount());
        ISpellContainer.set(probeStack, ISpellContainer.create(spellSlotCount, false, false));
        // 実アイテムの正規化結果で、後から Workbench 抽出できる Imbue かを判定する。
        spellImbueItem.normalizeImbuedSpellContainer(probeStack);
        return probeStack;
    }

    private static boolean hasAvailableSpellSlot(ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return false;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            if (spellContainer.getSpellAtIndex(index) == SpellData.EMPTY) {
                return true;
            }
        }
        return false;
    }

    private static boolean canExtractWorkbenchImbuedSpell(
            RestrictedSpellImbuableItem spellImbueItem,
            ItemStack resultStack,
            SpellData expectedSpellData
    ) {
        var spellContainer = ISpellContainer.get(resultStack);
        if (spellContainer == null) {
            return false;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            var spellData = spellContainer.getSpellAtIndex(index);
            if (!isSameSpellData(spellData, expectedSpellData)) {
                continue;
            }
            if (spellImbueItem.canRemoveWorkbenchSpell(resultStack, spellContainer, index, spellData)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameSpellData(SpellData first, SpellData second) {
        return first != SpellData.EMPTY
                && second != SpellData.EMPTY
                && first.getSpell() == second.getSpell()
                && first.getLevel() == second.getLevel();
    }

    private @Nullable SpellExtractionContext getSpellExtractionContext() {
        var sourceSlotIndex = findSingleOccupiedInputSlot();
        if (sourceSlotIndex < 0) {
            return null;
        }

        var inputStack = container.getItem(sourceSlotIndex);
        repairExtractablePresetSpellContainerIfNeeded(inputStack);
        initializePresetSpellContainerIfNeeded(inputStack);
        if (!ISpellContainer.isSpellContainer(inputStack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(inputStack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var extractionIndex = getSpellExtractionIndex(inputStack, spellContainer);
        var spellData = spellContainer.getSpellAtIndex(extractionIndex);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        if (!isAllowedSpellExtractionItem(inputStack)) {
            return new SpellExtractionContext(sourceSlotIndex, extractionIndex, inputStack, spellContainer, spellData, SpellExtractionBlockReason.NOT_ALLOWED);
        }

        if (!canRemoveExtractedSpell(inputStack, spellContainer, extractionIndex, spellData)) {
            return new SpellExtractionContext(
                    sourceSlotIndex,
                    extractionIndex,
                    inputStack,
                    spellContainer,
                    spellData,
                    getUnsupportedSpellExtractionBlockReason(inputStack, spellData)
            );
        }

        return new SpellExtractionContext(sourceSlotIndex, extractionIndex, inputStack, spellContainer, spellData, null);
    }

    private @Nullable SpellExtractionBlockReason getEmptySpellExtractionBlockReason() {
        var sourceSlotIndex = findSingleOccupiedInputSlot();
        if (sourceSlotIndex < 0) {
            return null;
        }

        var inputStack = container.getItem(sourceSlotIndex);
        repairExtractablePresetSpellContainerIfNeeded(inputStack);
        initializePresetSpellContainerIfNeeded(inputStack);
        if (!ISpellContainer.isSpellContainer(inputStack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(inputStack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() > 0) {
            return null;
        }

        if (inputStack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem) {
            return canCreateAnyExtractableWorkbenchImbue(inputStack, restrictedSpellImbuableItem)
                    ? SpellExtractionBlockReason.MISSING_SPELL
                    : SpellExtractionBlockReason.EMPTY_NOT_ALLOWED;
        }

        return isAllowedSpellExtractionItem(inputStack)
                ? SpellExtractionBlockReason.MISSING_SPELL
                : SpellExtractionBlockReason.EMPTY_NOT_ALLOWED;
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

    private boolean removeSpellFromExtractableItem(int sourceSlotIndex) {
        var extractionContext = getSpellExtractionContext(sourceSlotIndex);
        if (extractionContext == null || extractionContext.blockReason() != null) {
            return false;
        }

        var mutable = extractionContext.spellContainer().mutableCopy();
        if (!mutable.removeSpellAtIndex(extractionContext.spellIndex())) {
            return false;
        }

        // 初期化済みアイテムから spell_container を消すと既定呪文が再生成され得るため、空コンテナを保持する。
        ISpellContainer.set(extractionContext.inputStack(), mutable.toImmutable());
        rememberClearedPresetSpellState(extractionContext.inputStack());
        container.setChanged();
        return true;
    }

    private boolean consumeWorkbenchImbueInputs(int sourceSlotIndex, int scrollSlotIndex) {
        if (sourceSlotIndex < 0 || sourceSlotIndex >= INPUT_SLOT_COUNT
                || scrollSlotIndex < 0 || scrollSlotIndex >= INPUT_SLOT_COUNT
                || sourceSlotIndex == scrollSlotIndex) {
            return false;
        }

        var sourceStack = container.getItem(sourceSlotIndex);
        var scrollStack = container.getItem(scrollSlotIndex);
        if (!(sourceStack.getItem() instanceof RestrictedSpellImbuableItem)
                || !(scrollStack.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll)) {
            return false;
        }

        sourceStack.shrink(1);
        scrollStack.shrink(1);
        if (sourceStack.isEmpty()) {
            container.setItem(sourceSlotIndex, ItemStack.EMPTY);
        }
        if (scrollStack.isEmpty()) {
            container.setItem(scrollSlotIndex, ItemStack.EMPTY);
        }
        container.setChanged();
        return true;
    }

    private static void rememberClearedPresetSpellState(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem
                || item instanceof AbstractSwingMagicItem
                || item instanceof AbstractImbueShieldItem) {
            PresetSpellContainerStateHelper.rememberCleared(stack);
        }
    }

    private static void repairExtractablePresetSpellContainerIfNeeded(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            spellGunItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractRightClickMagicWeaponItem magicWeaponItem) {
            magicWeaponItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbueShieldItem imbueShieldItem) {
            imbueShieldItem.repairPresetSpellContainerStateIfNeeded(stack);
        }
    }

    private static void initializePresetSpellContainerIfNeeded(ItemStack stack) {
        if (stack.isEmpty() || ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        if (stack.getItem() instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
    }

    private @Nullable SpellExtractionContext getSpellExtractionContext(int sourceSlotIndex) {
        if (sourceSlotIndex < 0 || sourceSlotIndex >= INPUT_SLOT_COUNT) {
            return null;
        }

        var inputStack = container.getItem(sourceSlotIndex);
        repairExtractablePresetSpellContainerIfNeeded(inputStack);
        initializePresetSpellContainerIfNeeded(inputStack);
        if (!ISpellContainer.isSpellContainer(inputStack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(inputStack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var extractionIndex = getSpellExtractionIndex(inputStack, spellContainer);
        var spellData = spellContainer.getSpellAtIndex(extractionIndex);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        if (!isAllowedSpellExtractionItem(inputStack)) {
            return new SpellExtractionContext(sourceSlotIndex, extractionIndex, inputStack, spellContainer, spellData, SpellExtractionBlockReason.NOT_ALLOWED);
        }

        if (!canRemoveExtractedSpell(inputStack, spellContainer, extractionIndex, spellData)) {
            return new SpellExtractionContext(
                    sourceSlotIndex,
                    extractionIndex,
                    inputStack,
                    spellContainer,
                    spellData,
                    getUnsupportedSpellExtractionBlockReason(inputStack, spellData)
            );
        }

        return new SpellExtractionContext(sourceSlotIndex, extractionIndex, inputStack, spellContainer, spellData, null);
    }

    private static SpellExtractionBlockReason getUnsupportedSpellExtractionBlockReason(ItemStack stack, SpellData spellData) {
        if (isAlchemistsFlaskDefaultExtract(stack, spellData)) {
            return SpellExtractionBlockReason.DEFAULT_SPELL;
        }

        if (stack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem
                && !canCreateAnyExtractableWorkbenchImbue(stack, restrictedSpellImbuableItem)) {
            return SpellExtractionBlockReason.NOT_ALLOWED;
        }
        return SpellExtractionBlockReason.DEFAULT_SPELL;
    }

    private static boolean isAlchemistsFlaskDefaultExtract(ItemStack stack, SpellData spellData) {
        return stack.getItem() instanceof AlchemistsFlask
                && spellData != SpellData.EMPTY
                && spellData.getSpell() == SpellRegistry.EXTRACT.get()
                && spellData.getLevel() == 1;
    }

    private static boolean isAllowedSpellExtractionItem(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof RestrictedSpellImbuableItem
                || item instanceof AbstractSpellGunItem
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AbstractImbueShieldItem
                || item instanceof AbstractOffhandMagicItem
                || item instanceof AlchemistsFlask
                || stack.is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE);
    }

    private static int getSpellExtractionIndex(ItemStack stack, ISpellContainer spellContainer) {
        if (stack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem) {
            return restrictedSpellImbuableItem.getWorkbenchSpellExtractionIndex(stack, spellContainer);
        }
        return 0;
    }

    private static boolean canRemoveExtractedSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        if (stack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem) {
            return restrictedSpellImbuableItem.canRemoveWorkbenchSpell(stack, spellContainer, spellIndex, spellData);
        }
        return spellData.canRemove();
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
        var tagKey = stack.hasTag() ? stack.getTag().toString() : "";
        return (itemId == null ? "unknown" : itemId.toString()) + tagKey;
    }

    private record RecipeSelection(
            ItemStack icon,
            List<SpellcasterWorkbenchRecipe> recipes
    ) {
    }

    private record SpellExtraction(
            int sourceSlotIndex,
            ItemStack resultTemplate
    ) {
    }

    private record WorkbenchImbue(
            int sourceSlotIndex,
            int scrollSlotIndex,
            ItemStack resultTemplate
    ) {
    }

    private record SpellExtractionContext(
            int sourceSlotIndex,
            int spellIndex,
            ItemStack inputStack,
            ISpellContainer spellContainer,
            SpellData spellData,
            @Nullable SpellExtractionBlockReason blockReason
    ) {
    }

    private enum SpellExtractionBlockReason {
        DEFAULT_SPELL,
        NOT_ALLOWED,
        MISSING_SPELL,
        EMPTY_NOT_ALLOWED
    }

    private enum WorkbenchImbueBlockReason {
        UNSUPPORTED_EQUIPMENT
    }

    private record WorkbenchImbueContext(
            int sourceSlotIndex,
            int scrollSlotIndex,
            @Nullable RestrictedSpellImbuableItem spellImbueItem,
            ItemStack normalizedBaseStack,
            SpellData spellData,
            @Nullable WorkbenchImbueBlockReason blockReason
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
