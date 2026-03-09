package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EssenceSmokerBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int MAX_MATERIAL_COUNT = 8;
    public static final int PROCESS_DURATION_TICKS = 20 * 60;
    private static final int CATALYST_SLOT = 0;
    private static final int FIRST_MATERIAL_SLOT = 1;
    private static final int TOTAL_SLOT_COUNT = FIRST_MATERIAL_SLOT + MAX_MATERIAL_COUNT;
    private static final String CATALYST_TAG = "Catalyst";
    private static final String MATERIALS_TAG = "Materials";
    private static final String MATERIAL_SLOT_TAG = "Slot";
    private static final String PROCESSING_TAG = "Processing";
    private static final String COMPLETED_TAG = "Completed";
    private static final String PROCESS_FINISH_GAME_TIME_TAG = "ProcessFinishGameTime";
    private static final String STORED_EXPERIENCE_TAG = "StoredExperience";
    private static final int EXPERIENCE_PER_ITEM = 2;
    private static final int[] NO_SLOTS = new int[0];
    private static final int[] CATALYST_INPUT_SLOTS = {CATALYST_SLOT};
    private static final int[] MATERIAL_SLOTS = {
            FIRST_MATERIAL_SLOT,
            FIRST_MATERIAL_SLOT + 1,
            FIRST_MATERIAL_SLOT + 2,
            FIRST_MATERIAL_SLOT + 3,
            FIRST_MATERIAL_SLOT + 4,
            FIRST_MATERIAL_SLOT + 5,
            FIRST_MATERIAL_SLOT + 6,
            FIRST_MATERIAL_SLOT + 7
    };

    private ItemStack catalyst = ItemStack.EMPTY;
    private final NonNullList<ItemStack> materials = NonNullList.withSize(MAX_MATERIAL_COUNT, ItemStack.EMPTY);
    private boolean processing;
    private boolean completed;
    private long processFinishGameTime = -1L;
    private int storedExperience;
    private long lastColoredParticleGameTime = Long.MIN_VALUE;
    @Nullable
    private RecipeManager cachedRecipeManager;
    private List<EssenceSmokerRecipe> cachedRecipes = List.of();
    private ItemStack materialLookupCatalyst = ItemStack.EMPTY;
    private List<EssenceSmokerRecipe> materialLookupRecipes = List.of();
    private final IItemHandler[] sidedHandlers = createSidedHandlers();

    public EssenceSmokerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ESSENCE_SMOKER.get(), pos, state);
    }

    public boolean hasCatalyst() {
        return !catalyst.isEmpty();
    }

    public @NotNull ItemStack getCatalyst() {
        return catalyst.copy();
    }

    public @NotNull List<ItemStack> getMaterials() {
        return copyFilledMaterials();
    }

    public boolean hasMaterials() {
        for (var material : materials) {
            if (!material.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isMaterialSlotsFull() {
        return findFirstEmptyMaterialIndex() < 0;
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean markColoredParticleGameTime(long gameTime) {
        if (lastColoredParticleGameTime == gameTime) {
            return false;
        }

        lastColoredParticleGameTime = gameTime;
        return true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean canAcceptCatalyst(ItemStack stack) {
        return !processing && !completed && !hasCatalyst() && !stack.isEmpty() && findRecipeByCatalyst(stack).isPresent();
    }

    public boolean canAcceptMaterial(ItemStack stack) {
        return !processing
                && !completed
                && hasCatalyst()
                && !stack.isEmpty()
                && !isMaterialSlotsFull()
                && matchesCurrentCatalystMaterial(stack);
    }

    public boolean matchesCurrentCatalystMaterial(ItemStack stack) {
        return hasCatalyst() && !stack.isEmpty() && findMatchingRecipe(catalyst, stack).isPresent();
    }

    public boolean canIgnite() {
        if (!hasCatalyst() || !hasMaterials() || processing || completed) {
            return false;
        }

        for (var material : materials) {
            if (material.isEmpty()) {
                continue;
            }

            if (findMatchingRecipe(catalyst, material).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean setCatalyst(ItemStack stack) {
        if (!canAcceptCatalyst(stack)) {
            return false;
        }

        setCatalystInternal(stack.copyWithCount(1));
        markUpdated();
        return true;
    }

    public boolean addMaterial(ItemStack stack) {
        if (!canAcceptMaterial(stack)) {
            return false;
        }

        var emptyIndex = findFirstEmptyMaterialIndex();
        if (emptyIndex < 0) {
            return false;
        }

        materials.set(emptyIndex, stack.copyWithCount(1));
        markUpdated();
        return true;
    }

    public @NotNull ItemStack popLastMaterial() {
        var lastIndex = findLastFilledMaterialIndex();
        if (lastIndex < 0) {
            return ItemStack.EMPTY;
        }

        var removed = materials.get(lastIndex);
        materials.set(lastIndex, ItemStack.EMPTY);
        markUpdated();
        return removed;
    }

    public @NotNull ItemStack popCatalyst() {
        if (catalyst.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var removed = catalyst;
        setCatalystInternal(ItemStack.EMPTY);
        markUpdated();
        return removed;
    }

    public boolean startProcessing(long gameTime) {
        if (!canIgnite()) {
            return false;
        }

        processing = true;
        completed = false;
        processFinishGameTime = gameTime + PROCESS_DURATION_TICKS;
        markUpdated();
        return true;
    }

    public boolean ignite(long gameTime) {
        if (!startProcessing(gameTime)) {
            return false;
        }

        // 着火手段に関わらず同じ開始フィードバックを返す。
        playIgniteSound();
        return true;
    }

    public List<ItemStack> collectCompletedItems() {
        if (!completed) {
            return List.of();
        }

        var drops = copyFilledMaterials();
        resetContents();
        markUpdated();
        return drops;
    }

    public List<ItemStack> getDropsForBlockBreak() {
        var drops = new ArrayList<ItemStack>();
        if (!catalyst.isEmpty()) {
            drops.add(catalyst.copy());
        }
        drops.addAll(copyFilledMaterials());
        return drops;
    }

    public void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public void awardStoredExperience(Player player) {
        if (storedExperience <= 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ExperienceOrb.award(serverLevel, player.position(), storedExperience);
        storedExperience = 0;
        markUpdated();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!catalyst.isEmpty()) {
            tag.put(CATALYST_TAG, catalyst.save(registries));
        }

        if (hasMaterials()) {
            var materialListTag = new ListTag();
            for (var i = 0; i < materials.size(); i++) {
                var material = materials.get(i);
                if (material.isEmpty()) {
                    continue;
                }

                var materialTag = (CompoundTag) material.save(registries);
                materialTag.putInt(MATERIAL_SLOT_TAG, i);
                materialListTag.add(materialTag);
            }
            tag.put(MATERIALS_TAG, materialListTag);
        }

        tag.putBoolean(PROCESSING_TAG, processing);
        tag.putBoolean(COMPLETED_TAG, completed);
        if (processFinishGameTime >= 0L) {
            tag.putLong(PROCESS_FINISH_GAME_TIME_TAG, processFinishGameTime);
        }
        if (storedExperience > 0) {
            tag.putInt(STORED_EXPERIENCE_TAG, storedExperience);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        catalyst = tag.contains(CATALYST_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(CATALYST_TAG))
                : ItemStack.EMPTY;

        clearMaterialSlots();
        if (tag.contains(MATERIALS_TAG, Tag.TAG_LIST)) {
            var materialListTag = tag.getList(MATERIALS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < materialListTag.size(); i++) {
                var materialTag = materialListTag.getCompound(i);
                var material = ItemStack.parseOptional(registries, materialTag);
                if (material.isEmpty()) {
                    continue;
                }

                // 旧セーブはコンパクト配列だったため、Slot 未保存時は順番に詰めて読む。
                var materialIndex = materialTag.contains(MATERIAL_SLOT_TAG, Tag.TAG_INT)
                        ? materialTag.getInt(MATERIAL_SLOT_TAG)
                        : findFirstEmptyMaterialIndex();
                if (materialIndex < 0 || materialIndex >= MAX_MATERIAL_COUNT) {
                    continue;
                }

                materials.set(materialIndex, material);
            }
        }

        processing = tag.getBoolean(PROCESSING_TAG);
        completed = tag.getBoolean(COMPLETED_TAG);
        processFinishGameTime = tag.contains(PROCESS_FINISH_GAME_TIME_TAG, Tag.TAG_LONG)
                ? tag.getLong(PROCESS_FINISH_GAME_TIME_TAG)
                : -1L;
        storedExperience = tag.getInt(STORED_EXPERIENCE_TAG);
        invalidateRecipeCaches();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return catalyst.isEmpty() && !hasMaterials();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot == CATALYST_SLOT) {
            return catalyst;
        }

        if (!isMaterialSlot(slot)) {
            return ItemStack.EMPTY;
        }

        return materials.get(toMaterialIndex(slot));
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        var current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (current.getCount() <= amount) {
            return removeItemNoUpdateInternal(slot, true);
        }

        var extracted = current.split(amount);
        if (current.isEmpty()) {
            setStackInternal(slot, ItemStack.EMPTY);
        }

        if (isMaterialSlot(slot)) {
            clearCompletedStateIfOutputDrained();
        }
        markUpdated();
        return extracted;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeItemNoUpdateInternal(slot, false);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }

        ItemStack normalized = ItemStack.EMPTY;
        if (!stack.isEmpty()) {
            if (!canPlaceItem(slot, stack)) {
                return;
            }

            normalized = stack.copyWithCount(1);
        }

        if (stacksEqual(getItem(slot), normalized)) {
            return;
        }

        setStackInternal(slot, normalized);
        markUpdated();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null) {
            return false;
        }

        return level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty() || processing || completed || !isValidSlot(slot) || !getItem(slot).isEmpty()) {
            return false;
        }

        if (slot == CATALYST_SLOT) {
            return !hasCatalyst() && canAcceptCatalyst(stack);
        }

        return hasCatalyst() && isMaterialSlot(slot) && matchesCurrentCatalystMaterial(stack);
    }

    @Override
    public void clearContent() {
        if (isEmpty()) {
            return;
        }

        resetContents();
        markUpdated();
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        if (side == Direction.DOWN) {
            return completed ? MATERIAL_SLOTS : NO_SLOTS;
        }

        if (side == Direction.UP) {
            return hasCatalyst() && !processing && !completed ? MATERIAL_SLOTS : NO_SLOTS;
        }

        return !hasCatalyst() && !processing && !completed ? CATALYST_INPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        if (side == null) {
            return false;
        }

        if (slot == CATALYST_SLOT) {
            return side.getAxis().isHorizontal() && !hasCatalyst() && canPlaceItem(slot, stack);
        }

        return side == Direction.UP && isMaterialSlot(slot) && hasCatalyst() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
        return side == Direction.DOWN && completed && isMaterialSlot(slot) && !stack.isEmpty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceSmokerBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !blockEntity.processing) {
            return;
        }

        if (serverLevel.getGameTime() < blockEntity.processFinishGameTime) {
            return;
        }

        blockEntity.finishProcessing();
    }

    private void finishProcessing() {
        if (!processing) {
            return;
        }

        accumulateProcessingExperience();
        processing = false;
        completed = true;
        processFinishGameTime = -1L;
        transformMaterialsToResults();
        setCatalystInternal(ItemStack.EMPTY);
        playCompletionSound();
        markUpdated();
    }

    private void transformMaterialsToResults() {
        for (var i = 0; i < materials.size(); i++) {
            var material = materials.get(i);
            if (material.isEmpty()) {
                continue;
            }

            var transformed = resolveProcessedResult(material);
            if (!transformed.isEmpty()) {
                materials.set(i, transformed);
            }
        }
    }

    private @NotNull ItemStack resolveProcessedResult(ItemStack material) {
        var recipe = findMatchingRecipe(catalyst, material);
        if (recipe.isEmpty()) {
            // 加工開始後に datapack が差し替わっても、素材消失は起こさない。
            return material.copy();
        }

        var result = recipe.get().getResultTemplate();
        if (result.isEmpty()) {
            return material.copy();
        }

        var transformed = result.copy();
        var outputCount = Math.max(1L, material.getCount()) * result.getCount();
        transformed.setCount((int) Math.min(Integer.MAX_VALUE, outputCount));
        return transformed;
    }

    private void accumulateProcessingExperience() {
        var processedItemCount = 0;
        for (var material : materials) {
            if (!material.isEmpty()) {
                processedItemCount += Math.max(1, material.getCount());
            }
        }

        if (processedItemCount > 0) {
            // かまど同様、ホッパー搬出では消費せず手動回収時まで内部に保持する。
            var gainedExperience = (long) processedItemCount * EXPERIENCE_PER_ITEM;
            storedExperience = (int) Math.min(Integer.MAX_VALUE, storedExperience + gainedExperience);
        }
    }

    private List<ItemStack> copyFilledMaterials() {
        var copies = new ArrayList<ItemStack>(MAX_MATERIAL_COUNT);
        for (var material : materials) {
            if (!material.isEmpty()) {
                copies.add(material.copy());
            }
        }
        return copies;
    }

    private Optional<EssenceSmokerRecipe> findRecipeByCatalyst(ItemStack catalystStack) {
        return getAllRecipes().stream()
                .filter(recipe -> recipe.getCatalyst().test(catalystStack))
                .findFirst();
    }

    private Optional<EssenceSmokerRecipe> findMatchingRecipe(ItemStack catalystStack, ItemStack materialStack) {
        if (catalystStack.isEmpty() || materialStack.isEmpty()) {
            return Optional.empty();
        }

        return getRecipesForCatalyst(catalystStack).stream()
                .filter(recipe -> recipe.getMaterial().test(materialStack))
                .findFirst();
    }

    private List<EssenceSmokerRecipe> getRecipesForCatalyst(ItemStack catalystStack) {
        if (catalystStack.isEmpty()) {
            return List.of();
        }

        if (stacksEqual(materialLookupCatalyst, catalystStack)) {
            return materialLookupRecipes;
        }

        var recipes = getAllRecipes().stream()
                .filter(recipe -> recipe.getCatalyst().test(catalystStack))
                .toList();
        materialLookupCatalyst = catalystStack.copy();
        materialLookupRecipes = recipes;
        return recipes;
    }

    private List<EssenceSmokerRecipe> getAllRecipes() {
        if (level == null) {
            return List.of();
        }

        var recipeManager = level.getRecipeManager();
        if (cachedRecipeManager != recipeManager) {
            cachedRecipeManager = recipeManager;
            cachedRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).stream()
                    .map(recipe -> recipe.value())
                    .toList();
            invalidateMaterialRecipeCache();
        }

        return cachedRecipes;
    }

    private void resetContents() {
        setCatalystInternal(ItemStack.EMPTY);
        clearMaterialSlots();
        processing = false;
        completed = false;
        processFinishGameTime = -1L;
    }

    private void markUpdated() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void playCompletionSound() {
        if (level == null) {
            return;
        }

        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.9F, 0.08F);
    }

    private void playIgniteSound() {
        if (level == null) {
            return;
        }

        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void setCatalystInternal(ItemStack stack) {
        catalyst = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        invalidateMaterialRecipeCache();
    }

    private void clearMaterialSlots() {
        Collections.fill(materials, ItemStack.EMPTY);
    }

    private int findFirstEmptyMaterialIndex() {
        for (var i = 0; i < materials.size(); i++) {
            if (materials.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findLastFilledMaterialIndex() {
        for (var i = materials.size() - 1; i >= 0; i--) {
            if (!materials.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void clearCompletedStateIfOutputDrained() {
        if (!completed || hasMaterials()) {
            return;
        }

        completed = false;
        processFinishGameTime = -1L;
    }

    private @NotNull ItemStack removeItemNoUpdateInternal(int slot, boolean notify) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }

        var current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var removed = current.copy();
        setStackInternal(slot, ItemStack.EMPTY);
        if (isMaterialSlot(slot)) {
            clearCompletedStateIfOutputDrained();
        }
        if (notify) {
            markUpdated();
        }
        return removed;
    }

    private void setStackInternal(int slot, ItemStack stack) {
        if (slot == CATALYST_SLOT) {
            setCatalystInternal(stack);
            return;
        }

        if (isMaterialSlot(slot)) {
            materials.set(toMaterialIndex(slot), stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    private void invalidateRecipeCaches() {
        cachedRecipeManager = null;
        cachedRecipes = List.of();
        invalidateMaterialRecipeCache();
    }

    private void invalidateMaterialRecipeCache() {
        materialLookupCatalyst = ItemStack.EMPTY;
        materialLookupRecipes = List.of();
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < TOTAL_SLOT_COUNT;
    }

    private static boolean isMaterialSlot(int slot) {
        return slot >= FIRST_MATERIAL_SLOT && slot < TOTAL_SLOT_COUNT;
    }

    private static int toMaterialIndex(int slot) {
        return slot - FIRST_MATERIAL_SLOT;
    }

    private static boolean stacksEqual(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() && right.isEmpty();
        }

        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }

    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return null;
        }

        return sidedHandlers[side.get3DDataValue()];
    }

    private IItemHandler[] createSidedHandlers() {
        var handlers = new IItemHandler[Direction.values().length];
        for (var direction : Direction.values()) {
            handlers[direction.get3DDataValue()] = new SidedAutomationItemHandler(direction);
        }
        return handlers;
    }

    private final class SidedAutomationItemHandler implements IItemHandler {
        private final Direction side;

        private SidedAutomationItemHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return TOTAL_SLOT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return isSlotVisible(slot) ? EssenceSmokerBlockEntity.this.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isSlotVisible(slot) || !canInsert(slot, stack)) {
                return stack;
            }

            var remainder = stack.copy();
            remainder.shrink(1);
            if (!simulate) {
                setStackInternal(slot, stack.copyWithCount(1));
                markUpdated();
            }
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !isSlotVisible(slot) || !canExtract(slot)) {
                return ItemStack.EMPTY;
            }

            var current = EssenceSmokerBlockEntity.this.getItem(slot);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }

            var extracted = current.copyWithCount(Math.min(amount, current.getCount()));
            if (simulate) {
                return extracted;
            }

            if (current.getCount() <= extracted.getCount()) {
                setStackInternal(slot, ItemStack.EMPTY);
            } else {
                current.shrink(extracted.getCount());
                if (current.isEmpty()) {
                    setStackInternal(slot, ItemStack.EMPTY);
                }
            }

            if (isMaterialSlot(slot)) {
                clearCompletedStateIfOutputDrained();
            }
            markUpdated();
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (!isSlotVisible(slot)) {
                return 0;
            }

            return completed && isMaterialSlot(slot) ? 64 : 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isSlotVisible(slot) && canInsert(slot, stack);
        }

        private boolean isSlotVisible(int slot) {
            if (!isValidSlot(slot)) {
                return false;
            }

            for (var visibleSlot : getSlotsForFace(side)) {
                if (visibleSlot == slot) {
                    return true;
                }
            }
            return false;
        }

        private boolean canInsert(int slot, ItemStack stack) {
            return canPlaceItemThroughFace(slot, stack, side);
        }

        private boolean canExtract(int slot) {
            return canTakeItemThroughFace(slot, EssenceSmokerBlockEntity.this.getItem(slot), side);
        }
    }
}
