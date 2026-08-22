package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.AlchemyBrewerWaterSupplyEffectPacket;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerModifierRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AlchemyBrewerBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY_MB = 1000;
    public static final int DOSE_AMOUNT_MB = 250;
    public static final int INPUT_SLOT = 0;
    public static final int FIRST_MATERIAL_SLOT = 1;
    public static final int MATERIAL_SLOT_COUNT = 10;
    public static final int OUTPUT_SLOT = 11;
    public static final int SLOT_COUNT = 12;
    public static final int START_STABILITY_TICKS = 20;
    private static final int FILL_INTERVAL_TICKS = 10;
    private static final double WATER_EFFECT_BROADCAST_RANGE = 48.0d;
    static final int MENU_DATA_COUNT = 7;
    static final int MENU_DATA_AUTO_BREWING = 0;
    static final int MENU_DATA_TANK_AMOUNT = 1;
    static final int MENU_DATA_DISPLAY_POTION = 2;
    static final int MENU_DATA_DISPLAY_AMOUNT = 3;
    static final int MENU_DATA_STATE_FLAGS = 4;
    static final int MENU_DATA_ELAPSED_TICKS = 5;
    static final int MENU_DATA_TOTAL_TICKS = 6;
    static final int MENU_FLAG_PROCESSING = 1;
    static final int MENU_FLAG_PREVIEW = 1 << 1;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) {
            if (slot >= FIRST_MATERIAL_SLOT && slot < OUTPUT_SLOT) resetStartWait();
            markUpdated();
        }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == INPUT_SLOT) return canUseAsContainerInput(stack);
            if (slot >= FIRST_MATERIAL_SLOT && slot < OUTPUT_SLOT) return isLoadedRecipeIngredient(stack);
            return false;
        }

        @Override public int getSlotLimit(int slot) {
            return slot == INPUT_SLOT ? 64 : super.getSlotLimit(slot);
        }
    };
    private LazyOptional<IItemHandler>[] sidedHandlers = createSidedHandlers();
    private boolean autoBrewing;
    private ResourceLocation tankPotion;
    private int tankAmountMb;
    private int fillTicker;
    private Candidate waitingCandidate;
    private int stableTicks;
    private Job activeJob;
    private ResourceLocation previewPotion;
    private int previewAmountMb;
    private int previewTotalTicks;
    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case MENU_DATA_AUTO_BREWING -> autoBrewing ? 1 : 0;
                case MENU_DATA_TANK_AMOUNT -> tankAmountMb;
                case MENU_DATA_DISPLAY_POTION -> registryId(getDisplayPotionId());
                case MENU_DATA_DISPLAY_AMOUNT -> getDisplayAmountMb();
                case MENU_DATA_STATE_FLAGS -> (isProcessing() ? MENU_FLAG_PROCESSING : 0)
                        | (isDisplayPreview() ? MENU_FLAG_PREVIEW : 0);
                case MENU_DATA_ELAPSED_TICKS -> getElapsedTicks();
                case MENU_DATA_TOTAL_TICKS -> getTotalTicks();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            // 表示専用。クライアントから受け取った値でBlockEntity状態を変更しない。
        }

        @Override public int getCount() { return MENU_DATA_COUNT; }
    };

    public AlchemyBrewerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ALCHEMY_BREWER.get(), pos, state);
    }

    public ItemStackHandler getInventory() { return inventory; }
    ContainerData getMenuData() { return menuData; }
    public boolean isAutoBrewing() { return autoBrewing; }
    public boolean isProcessing() { return activeJob != null; }
    public int getTankAmountMb() { return tankAmountMb; }
    public @Nullable ResourceLocation getTankPotionId() { return tankPotion; }
    public int extractTankPotion(ResourceLocation expectedPotionId, int requestedAmountMb) {
        if (tankPotion == null || !tankPotion.equals(expectedPotionId) || requestedAmountMb < DOSE_AMOUNT_MB) {
            return 0;
        }

        var requestedDoseAmount = requestedAmountMb / DOSE_AMOUNT_MB * DOSE_AMOUNT_MB;
        var extractedAmount = Math.min(tankAmountMb / DOSE_AMOUNT_MB * DOSE_AMOUNT_MB, requestedDoseAmount);
        if (extractedAmount <= 0) {
            return 0;
        }

        consumeTankAmount(extractedAmount);
        markUpdated();
        return extractedAmount;
    }
    public @Nullable ResourceLocation getDisplayPotionId() {
        return tankAmountMb > 0 ? tankPotion : activeJob != null ? activeJob.result : previewPotion;
    }
    public int getDisplayAmountMb() {
        return tankAmountMb > 0 ? tankAmountMb : activeJob != null ? activeJob.amountMb : previewAmountMb;
    }
    public boolean isDisplayPreview() { return tankAmountMb == 0 && getDisplayPotionId() != null; }
    public int getElapsedTicks() { return activeJob == null ? 0 : activeJob.elapsedTicks; }
    public int getTotalTicks() { return activeJob == null ? previewTotalTicks : activeJob.totalTicks; }

    public void toggleAutoBrewing() {
        autoBrewing = !autoBrewing;
        resetStartWait();
        markUpdated();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyBrewerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        blockEntity.trySupplyWater(serverLevel, pos, state);

        boolean changed = false;
        if (blockEntity.activeJob != null) {
            blockEntity.activeJob.elapsedTicks++;
            blockEntity.setChanged();
            if (blockEntity.activeJob.elapsedTicks >= blockEntity.activeJob.totalTicks) {
                blockEntity.finishJob();
                changed = true;
            }
        }

        blockEntity.fillTicker++;
        if (blockEntity.fillTicker >= FILL_INTERVAL_TICKS) {
            blockEntity.fillTicker = 0;
            changed |= blockEntity.fillContainer();
        }

        var candidate = blockEntity.findCandidate();
        blockEntity.updatePreview(candidate);
        if (!blockEntity.autoBrewing || blockEntity.activeJob != null || blockEntity.tankAmountMb != 0 || candidate == null) {
            blockEntity.resetStartWait();
        } else if (candidate.equals(blockEntity.waitingCandidate)) {
            blockEntity.stableTicks++;
            if (blockEntity.stableTicks >= START_STABILITY_TICKS) {
                changed |= blockEntity.reserveAndStart(candidate);
                blockEntity.resetStartWait();
            }
        } else {
            blockEntity.waitingCandidate = candidate;
            blockEntity.stableTicks = 1;
        }

        if (changed) blockEntity.markUpdated();
    }

    private void trySupplyWater(ServerLevel level, BlockPos pos, BlockState state) {
        var config = ApprenticeCodexServerConfig.alchemyBrewerConfig();
        if ((config.vanillaCauldronWaterLevelIncrease() <= 0
                && config.alchemistCauldronWaterAmountMb() <= 0)
                || level.hasNeighborSignal(pos)) {
            return;
        }

        var interval = Math.max(10, config.waterSupplyIntervalTicks());
        var phase = Math.floorMod(Long.hashCode(pos.asLong()), interval);
        if (Math.floorMod(level.getGameTime(), interval) != phase) {
            return;
        }

        var target = AlchemyBrewerWaterSupply.trySupply(level, pos, config);
        if (target == null || !state.hasProperty(AlchemyBrewer.FACING)) {
            return;
        }

        var facing = state.getValue(AlchemyBrewer.FACING);
        var source = AlchemyBrewerWaterEffects.localToWorld(pos, facing, AlchemyBrewerWaterEffects.JAR_MOUTH_LOCAL);
        level.playSound(null, source.x, source.y, source.z,
                SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0f, 1.0f);
        Networks.sendToPlayersNear(
                level,
                Vec3.atCenterOf(pos),
                WATER_EFFECT_BROADCAST_RANGE,
                new AlchemyBrewerWaterSupplyEffectPacket(pos, facing, target, level.getGameTime())
        );
    }

    private void updatePreview(@Nullable Candidate candidate) {
        var nextPotion = candidate == null ? null : candidate.result;
        var nextAmount = candidate == null ? 0 : candidate.amountMb;
        var nextTotalTicks = candidate == null ? 0 : candidate.totalTicks;
        if (java.util.Objects.equals(previewPotion, nextPotion)
                && previewAmountMb == nextAmount
                && previewTotalTicks == nextTotalTicks) return;
        previewPotion = nextPotion;
        previewAmountMb = nextAmount;
        previewTotalTicks = nextTotalTicks;
        markUpdated();
    }

    private @Nullable Candidate findCandidate() {
        if (level == null || tankAmountMb != 0 || activeJob != null) return null;
        var baseRecipes = level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get());
        var candidates = new ArrayList<Candidate>();
        for (var recipe : baseRecipes) {
            for (int baseSlot = FIRST_MATERIAL_SLOT; baseSlot < OUTPUT_SLOT; baseSlot++) {
                if (!recipe.base().test(inventory.getStackInSlot(baseSlot))) continue;
                for (int ingredientSlot = FIRST_MATERIAL_SLOT; ingredientSlot < OUTPUT_SLOT; ingredientSlot++) {
                    if (!recipe.ingredient().test(inventory.getStackInSlot(ingredientSlot))) continue;
                    if (baseSlot == ingredientSlot && inventory.getStackInSlot(baseSlot).getCount() < 2) continue;
                    var candidate = createCandidate(recipe, baseSlot, ingredientSlot);
                    // 無効な datapack ID を予約して素材だけ失うことがないよう、開始候補から除外する。
                    if (ForgeRegistries.POTIONS.containsKey(candidate.result)) candidates.add(candidate);
                }
            }
        }
        return candidates.stream().min(CANDIDATE_COMPARATOR).orElse(null);
    }

    private Candidate createCandidate(AlchemyBrewerRecipe recipe, int baseSlot, int ingredientSlot) {
        int modifierSlot = -1;
        ResourceLocation modifierId = null;
        ResourceLocation result = recipe.result();
        AlchemyBrewerModifierRecipe best = null;
        if (level != null){
            for (var modifier : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get())) {
                if (!modifier.input().equals(recipe.result())) continue;
                for (int slot = FIRST_MATERIAL_SLOT; slot < OUTPUT_SLOT; slot++) {
                    var remaining = inventory.getStackInSlot(slot).getCount()
                            - (slot == baseSlot ? 1 : 0) - (slot == ingredientSlot ? 1 : 0);
                    if (remaining <= 0 || !modifier.ingredient().test(inventory.getStackInSlot(slot))) continue;
                    if (best == null || slot < modifierSlot
                            || slot == modifierSlot && modifier.priority() > best.priority()
                            || slot == modifierSlot && modifier.priority() == best.priority()
                            && modifier.getId().toString().compareTo(best.getId().toString()) < 0) {
                        best = modifier;
                        modifierSlot = slot;
                    }
                }
            }
        }
        if (best != null) {
            modifierId = best.getId();
            result = best.result();
        }
        return new Candidate(recipe.getId(), modifierId, result, recipe.fluidAmountMb(), recipe.processingTimeTicks(),
                recipe.priority(), baseSlot, ingredientSlot, modifierSlot);
    }

    private boolean reserveAndStart(Candidate candidate) {
        int[] counts = new int[SLOT_COUNT];
        counts[candidate.baseSlot]++;
        counts[candidate.ingredientSlot]++;
        if (candidate.modifierSlot >= 0) counts[candidate.modifierSlot]++;
        for (int slot = FIRST_MATERIAL_SLOT; slot < OUTPUT_SLOT; slot++) {
            if (inventory.getStackInSlot(slot).getCount() < counts[slot]) return false;
        }
        var reserved = new ArrayList<ItemStack>();
        for (int slot = FIRST_MATERIAL_SLOT; slot < OUTPUT_SLOT; slot++) {
            if (counts[slot] > 0) reserved.add(inventory.extractItem(slot, counts[slot], false));
        }
        activeJob = new Job(candidate.baseRecipeId, candidate.modifierId, candidate.result,
                candidate.amountMb, candidate.totalTicks, 0, reserved);
        return true;
    }

    private void finishJob() {
        if (activeJob == null) return;
        tankPotion = activeJob.result;
        tankAmountMb = activeJob.amountMb;
        activeJob = null;
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private boolean fillContainer() {
        var input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;
        if (input.getItem() instanceof AbstractPotionFlaskItem
                && AbstractPotionFlaskItem.getStoredDoseCount(input) >= AbstractPotionFlaskItem.getMaxDoseCapacity(input)) {
            return moveFullInputToOutput();
        }
        if (tankPotion == null || tankAmountMb < DOSE_AMOUNT_MB) return false;
        var potion = resolveRegisteredPotion(tankPotion);
        if (potion == null) return false;
        var representative = PotionContentsHelper.createPotionStack(Items.POTION, potion);

        if (input.is(Items.GLASS_BOTTLE)) {
            if (!canAcceptOutput(representative)) return false;
            inventory.extractItem(INPUT_SLOT, 1, false);
            insertOutput(representative);
            consumeTankDose();
            return true;
        }
        if (!(input.getItem() instanceof AbstractPotionFlaskItem)) return false;
        int current = AbstractPotionFlaskItem.getStoredDoseCount(input);
        int maximum = AbstractPotionFlaskItem.getMaxDoseCapacity(input);
        if (!AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(input, representative)) return false;
        var filled = AbstractPotionFlaskItem.copyWithAddedDoses(input, representative, 1);
        if (filled.isEmpty()) return false;
        if (current + 1 >= maximum) {
            if (!canAcceptOutput(filled)) return false;
            inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
            insertOutput(filled);
        } else {
            inventory.setStackInSlot(INPUT_SLOT, filled);
        }
        consumeTankDose();
        return true;
    }

    private boolean moveFullInputToOutput() {
        var input = inventory.getStackInSlot(INPUT_SLOT);
        if (!canAcceptOutput(input)) return false;
        inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        insertOutput(input);
        return true;
    }

    private boolean canAcceptOutput(ItemStack stack) {
        var output = inventory.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || ItemStack.isSameItemSameTags(output, stack)
                && output.getCount() + stack.getCount() <= Math.min(output.getMaxStackSize(), inventory.getSlotLimit(OUTPUT_SLOT));
    }

    private void insertOutput(ItemStack stack) {
        var output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) inventory.setStackInSlot(OUTPUT_SLOT, stack.copy());
        else {
            var updated = output.copy();
            updated.grow(stack.getCount());
            inventory.setStackInSlot(OUTPUT_SLOT, updated);
        }
    }

    private void consumeTankDose() {
        consumeTankAmount(DOSE_AMOUNT_MB);
    }

    public static @Nullable Potion resolveRegisteredPotion(@Nullable ResourceLocation potionId) {
        return potionId == null || !ForgeRegistries.POTIONS.containsKey(potionId)
                ? null
                : ForgeRegistries.POTIONS.getValue(potionId);
    }

    private void consumeTankAmount(int amountMb) {
        tankAmountMb -= amountMb;
        if (tankAmountMb <= 0) {
            tankAmountMb = 0;
            tankPotion = null;
        }
    }

    private boolean canUseAsContainerInput(ItemStack stack) {
        if (stack.is(Items.GLASS_BOTTLE)) return true;
        return stack.getItem() instanceof AbstractPotionFlaskItem
                && AbstractPotionFlaskItem.getStoredDoseCount(stack) < AbstractPotionFlaskItem.getMaxDoseCapacity(stack);
    }

    private boolean isLoadedRecipeIngredient(ItemStack stack) {
        if (level == null || stack.isEmpty()) return !stack.isEmpty();
        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get())) {
            if (recipe.base().test(stack) || recipe.ingredient().test(stack)) return true;
        }
        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get())) {
            if (recipe.ingredient().test(stack)) return true;
        }
        return false;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
        }
        if (activeJob != null) {
            for (var stack : activeJob.reserved) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
        }
        inventory.setSize(SLOT_COUNT);
        activeJob = null;
    }

    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        return side == null ? null : sidedHandlers[side.get3DDataValue()].orElse(null);
    }

    @SuppressWarnings("unchecked")
    private LazyOptional<IItemHandler>[] createSidedHandlers() {
        var handlers = new LazyOptional[Direction.values().length];
        for (var side : Direction.values()) handlers[side.get3DDataValue()] = LazyOptional.of(() -> new AutomationHandler(side));
        return handlers;
    }

    private final class AutomationHandler implements IItemHandler {
        private final Direction side;
        private AutomationHandler(Direction side) { this.side = side; }
        @Override public int getSlots() { return SLOT_COUNT; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == OUTPUT_SLOT ? stack : inventory.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return side == Direction.DOWN && slot == OUTPUT_SLOT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot != OUTPUT_SLOT && inventory.isItemValid(slot, stack); }
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putBoolean("AutoBrewing", autoBrewing);
        if (tankPotion != null) tag.putString("TankPotion", tankPotion.toString());
        tag.putInt("TankAmountMb", tankAmountMb);
        if (activeJob != null) tag.put("ActiveJob", activeJob.save());
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) inventory.deserializeNBT(tag.getCompound("Inventory"));
        autoBrewing = tag.getBoolean("AutoBrewing");
        tankPotion = tag.contains("TankPotion", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("TankPotion")) : null;
        tankAmountMb = net.minecraft.util.Mth.clamp(tag.getInt("TankAmountMb"), 0, TANK_CAPACITY_MB);
        activeJob = tag.contains("ActiveJob", Tag.TAG_COMPOUND) ? Job.load(tag.getCompound("ActiveJob")) : null;
    }

    @Override public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        if (previewPotion != null) tag.putString("PreviewPotion", previewPotion.toString());
        tag.putInt("PreviewAmountMb", previewAmountMb);
        tag.putInt("PreviewTotalTicks", previewTotalTicks);
        return tag;
    }

    @Override public void handleUpdateTag(@NotNull CompoundTag tag) {
        load(tag);
        previewPotion = tag.contains("PreviewPotion", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("PreviewPotion")) : null;
        previewAmountMb = tag.getInt("PreviewAmountMb");
        previewTotalTicks = tag.getInt("PreviewTotalTicks");
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(@NotNull Connection connection, ClientboundBlockEntityDataPacket packet) {
        // 通常のBlockEntity更新もchunk初期同期と同じ経路で読み、将来のrendererからもプレビューを参照できるようにする。
        handleUpdateTag(packet.getTag());
    }

    private void resetStartWait() { waitingCandidate = null; stableTicks = 0; }
    private void markUpdated() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static int registryId(@Nullable ResourceLocation id) {
        var value = resolveRegisteredPotion(id);
        return value == null ? -1 : BuiltInRegistries.POTION.getId(value);
    }

    private static final Comparator<Candidate> CANDIDATE_COMPARATOR = Comparator
            .comparingInt((Candidate candidate) -> Math.min(candidate.baseSlot, candidate.ingredientSlot))
            .thenComparingInt(candidate -> Math.max(candidate.baseSlot, candidate.ingredientSlot))
            .thenComparing(Comparator.comparingInt((Candidate candidate) -> candidate.priority).reversed())
            .thenComparing(candidate -> candidate.baseRecipeId.toString());

    private record Candidate(ResourceLocation baseRecipeId, @Nullable ResourceLocation modifierId,
                             ResourceLocation result, int amountMb, int totalTicks, int priority,
                             int baseSlot, int ingredientSlot, int modifierSlot) { }

    private static final class Job {
        private final ResourceLocation baseRecipeId;
        private final @Nullable ResourceLocation modifierId;
        private final ResourceLocation result;
        private final int amountMb;
        private final int totalTicks;
        private int elapsedTicks;
        private final List<ItemStack> reserved;
        private Job(ResourceLocation baseRecipeId, @Nullable ResourceLocation modifierId, ResourceLocation result,
                    int amountMb, int totalTicks, int elapsedTicks, List<ItemStack> reserved) {
            this.baseRecipeId = baseRecipeId; this.modifierId = modifierId; this.result = result;
            this.amountMb = amountMb; this.totalTicks = totalTicks; this.elapsedTicks = elapsedTicks;
            this.reserved = List.copyOf(reserved);
        }

        private CompoundTag save() {
            var tag = new CompoundTag();
            tag.putString("BaseRecipe", baseRecipeId.toString());
            if (modifierId != null) tag.putString("ModifierRecipe", modifierId.toString());
            tag.putString("Result", result.toString()); tag.putInt("AmountMb", amountMb);
            tag.putInt("TotalTicks", totalTicks); tag.putInt("ElapsedTicks", elapsedTicks);
            var items = new ListTag();
            for (var stack : reserved) items.add(stack.save(new CompoundTag()));
            tag.put("Reserved", items);
            return tag;
        }

        private static @Nullable Job load(CompoundTag tag) {
            var base = ResourceLocation.tryParse(tag.getString("BaseRecipe"));
            var result = ResourceLocation.tryParse(tag.getString("Result"));
            if (base == null || result == null || tag.getInt("TotalTicks") <= 0) return null;
            var modifier = tag.contains("ModifierRecipe", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("ModifierRecipe")) : null;
            var reserved = new ArrayList<ItemStack>();
            var items = tag.getList("Reserved", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                var stack = ItemStack.of(items.getCompound(i));
                if (!stack.isEmpty()) reserved.add(stack);
            }
            int total = tag.getInt("TotalTicks");
            return new Job(base, modifier, result, net.minecraft.util.Mth.clamp(tag.getInt("AmountMb"), 250, 1000), total,
                    net.minecraft.util.Mth.clamp(tag.getInt("ElapsedTicks"), 0, total), reserved);
        }
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        for (var handler : sidedHandlers) handler.invalidate();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        sidedHandlers = createSidedHandlers();
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sidedHandlers[side.get3DDataValue()].cast();
        }
        return super.getCapability(capability, side);
    }
}
