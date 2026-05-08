package jp.aquafactory.apprenticecodex.block.atelierstation;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AlchemistCauldronFluidTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AtelierStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLASK_SLOT_COUNT = 5;
    public static final int FILTER_SLOT_COUNT = 6;
    public static final int MAX_STORED_FLUID_AMOUNT = 16000;
    public static final int MILLIBUCKETS_PER_USE = 250;

    private static final String FLASKS_TAG = "Flasks";
    private static final String FILTERS_TAG = "Filters";
    private static final String SLOT_TAG = "Slot";
    private static final String STORED_FLUIDS_TAG = "StoredFluids";
    private static final String STORED_ITEM_TAG = "Item";
    private static final String STORED_AMOUNT_TAG = "Amount";
    private static final String FLASK_SUPPLY_COOLDOWN_UNTIL_TAG = "FlaskSupplyCooldownUntil";
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_RADIUS = 2;
    private static final double PLAYER_SUPPLY_RANGE = 8.0D;
    private static final int EXPORT_PHASE_OFFSET_TICKS = 10;
    private static final int FLASK_SUPPLY_COOLDOWN_TICKS = 20;
    private static final double EFFECT_BROADCAST_RANGE = 48.0D;

    private final ItemStackHandler flaskInventory = new ItemStackHandler(FLASK_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof AbstractPotionFlaskItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final NonNullList<ItemStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
    private final List<StoredPotionEntry> storedFluids = new ArrayList<>();
    private int storedFluidAmount;
    private long flaskSupplyCooldownUntilGameTime;

    public AtelierStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ATELIER_STATION.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.atelier_station");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory,
                                                     @NotNull Player player) {
        return new AtelierStationMenu(containerId, inventory, this);
    }

    public @NotNull ItemStackHandler getFlaskInventory() {
        return flaskInventory;
    }

    public @NotNull ItemStack getFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        return filters.get(slot);
    }

    public boolean hasFilter(int slot) {
        return !getFilter(slot).isEmpty();
    }

    public boolean hasAnyFilterConfigured() {
        for (var filter : filters) {
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int getStoredFluidAmount() {
        return storedFluidAmount;
    }

    public @NotNull List<StoredPotionEntry> getStoredFluidsForDisplay() {
        return storedFluids.stream().map(StoredPotionEntry::copy).toList();
    }

    public @NotNull List<StoredPotionEntry> getTopStoredFluidsForJade(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return storedFluids.stream()
                .map(StoredPotionEntry::copy)
                .sorted(Comparator.comparingInt(StoredPotionEntry::amountMb).reversed())
                .limit(limit)
                .toList();
    }

    public @NotNull List<ItemStack> getLoadedFlasksForDisplay() {
        var flasks = new ArrayList<ItemStack>(flaskInventory.getSlots());
        for (var slot = 0; slot < flaskInventory.getSlots(); ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                flasks.add(stack.copy());
            }
        }
        return flasks;
    }

    public boolean setFilter(int slot, @NotNull ItemStack filterStack) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT) {
            return false;
        }

        var normalizedFilter = SpellcastersFlask.copyFilterItem(filterStack);
        if (normalizedFilter.isEmpty()) {
            return false;
        }

        if (ItemStack.matches(filters.get(slot), normalizedFilter)) {
            return false;
        }

        filters.set(slot, normalizedFilter);
        setChanged();
        return true;
    }

    public boolean clearFilter(int slot) {
        if (!hasFilter(slot)) {
            return false;
        }

        filters.set(slot, ItemStack.EMPTY);
        setChanged();
        return true;
    }

    public void dropStoredFlasks() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (var slot = 0; slot < flaskInventory.getSlots(); ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            flaskInventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtelierStationBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var changed = false;
        if (blockEntity.shouldRunCollectionTick(serverLevel)
                && blockEntity.hasAnyFilterConfigured()
                && blockEntity.storedFluidAmount < MAX_STORED_FLUID_AMOUNT) {
            changed |= blockEntity.collectFromNearbyCauldrons(serverLevel);
        }
        if (blockEntity.shouldRunExportTick(serverLevel)) {
            if (!blockEntity.isFlaskSupplyCoolingDown(serverLevel.getGameTime())) {
                changed |= blockEntity.exportToFlaskStorage(serverLevel);
                changed |= blockEntity.supplyNearestPlayer(serverLevel, state);
            }
        }

        if (changed) {
            blockEntity.markUpdated();
        }
    }

    private boolean shouldRunCollectionTick(ServerLevel level) {
        return isScheduledPhase(level.getGameTime(), 0);
    }

    private boolean shouldRunExportTick(ServerLevel level) {
        return isScheduledPhase(level.getGameTime(), EXPORT_PHASE_OFFSET_TICKS);
    }

    private boolean isScheduledPhase(long gameTime, int phaseOffsetTicks) {
        var blockPhase = Math.floorMod(Long.hashCode(worldPosition.asLong()), SCAN_INTERVAL_TICKS);
        var scheduledPhase = Math.floorMod(blockPhase + phaseOffsetTicks, SCAN_INTERVAL_TICKS);
        return Math.floorMod(gameTime, SCAN_INTERVAL_TICKS) == scheduledPhase;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(FLASKS_TAG, flaskInventory.serializeNBT(registries));

        var filterList = new ListTag();
        for (var slot = 0; slot < filters.size(); ++slot) {
            var stack = filters.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var filterTag = new CompoundTag();
            filterTag.putInt(SLOT_TAG, slot);
            filterTag.put("Item", stack.saveOptional(registries));
            filterList.add(filterTag);
        }
        tag.put(FILTERS_TAG, filterList);

        var storedFluidList = new ListTag();
        for (var entry : storedFluids) {
            if (entry.amountMb() <= 0 || entry.representativeItem().isEmpty()) {
                continue;
            }

            var storedFluidTag = new CompoundTag();
            storedFluidTag.put(STORED_ITEM_TAG, entry.representativeItem().saveOptional(registries));
            storedFluidTag.putInt(STORED_AMOUNT_TAG, entry.amountMb());
            storedFluidList.add(storedFluidTag);
        }
        tag.put(STORED_FLUIDS_TAG, storedFluidList);
        tag.putLong(FLASK_SUPPLY_COOLDOWN_UNTIL_TAG, flaskSupplyCooldownUntilGameTime);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        flaskInventory.deserializeNBT(registries, tag.getCompound(FLASKS_TAG));
        storedFluids.clear();
        storedFluidAmount = 0;
        flaskSupplyCooldownUntilGameTime = tag.getLong(FLASK_SUPPLY_COOLDOWN_UNTIL_TAG);

        for (var index = 0; index < FILTER_SLOT_COUNT; ++index) {
            filters.set(index, ItemStack.EMPTY);
        }

        if (tag.contains(FILTERS_TAG, Tag.TAG_LIST)) {
            var filterList = tag.getList(FILTERS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < filterList.size(); ++i) {
                var filterTag = filterList.getCompound(i);
                var slot = filterTag.getInt(SLOT_TAG);
                if (slot < 0 || slot >= FILTER_SLOT_COUNT || !filterTag.contains("Item", Tag.TAG_COMPOUND)) {
                    continue;
                }

                var normalizedFilter = SpellcastersFlask.copyFilterItem(ItemStack.parseOptional(registries, filterTag.getCompound("Item")));
                if (!normalizedFilter.isEmpty()) {
                    filters.set(slot, normalizedFilter);
                }
            }
        }

        if (tag.contains(STORED_FLUIDS_TAG, Tag.TAG_LIST)) {
            var storedFluidList = tag.getList(STORED_FLUIDS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < storedFluidList.size(); ++i) {
                var storedFluidTag = storedFluidList.getCompound(i);
                if (!storedFluidTag.contains(STORED_ITEM_TAG, Tag.TAG_COMPOUND)) {
                    continue;
                }

                var representativeItem = SpellcastersFlask.copyFilterItem(ItemStack.parseOptional(registries, storedFluidTag.getCompound(STORED_ITEM_TAG)));
                var amountMb = normalizeFluidAmount(storedFluidTag.getInt(STORED_AMOUNT_TAG));
                if (representativeItem.isEmpty() || amountMb <= 0) {
                    continue;
                }

                insertStoredFluid(representativeItem, amountMb);
            }
        }
    }

    private boolean collectFromNearbyCauldrons(ServerLevel level) {
        var changed = false;
        var minPos = worldPosition.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
        var maxPos = worldPosition.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);
        for (var pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (storedFluidAmount >= MAX_STORED_FLUID_AMOUNT) {
                return changed;
            }

            if (worldPosition.equals(pos) || !level.isLoaded(pos)) {
                continue;
            }

            var targetBlockEntity = level.getBlockEntity(pos);
            if (!(targetBlockEntity instanceof AlchemistCauldronTile cauldronTile) || cauldronTile.fluidInventory == null) {
                continue;
            }

            changed |= collectFromCauldron(level, cauldronTile);
        }
        return changed;
    }

    private boolean collectFromCauldron(ServerLevel level, AlchemistCauldronTile cauldronTile) {
        var remainingCapacity = MAX_STORED_FLUID_AMOUNT - storedFluidAmount;
        if (remainingCapacity <= 0) {
            return false;
        }

        var fluidStack = AlchemistCauldronFluidTools.findFirstFluidFromTop(cauldronTile, candidate -> {
            var representativeItem = SpellcastersFlask.resolveRepresentativeItem(level, candidate);
            return !representativeItem.isEmpty() && matchesAnyFilter(representativeItem);
        });
        if (fluidStack == null || fluidStack.isEmpty()) {
            return false;
        }

        var representativeItem = SpellcastersFlask.resolveRepresentativeItem(level, fluidStack);
        if (representativeItem.isEmpty()) {
            return false;
        }

        var requestedAmount = normalizeFluidAmount(Math.min(remainingCapacity, fluidStack.getAmount()));
        if (requestedAmount <= 0) {
            return false;
        }

        var simulatedDrain = AlchemistCauldronFluidTools.drainMatchingFluid(
                cauldronTile,
                fluidStack,
                requestedAmount,
                IFluidHandler.FluidAction.SIMULATE
        );
        var drainAmount = normalizeFluidAmount(simulatedDrain.getAmount());
        if (drainAmount <= 0) {
            return false;
        }

        var drained = AlchemistCauldronFluidTools.drainMatchingFluid(
                cauldronTile,
                fluidStack,
                drainAmount,
                IFluidHandler.FluidAction.EXECUTE
        );
        var extractedAmount = normalizeFluidAmount(drained.getAmount());
        if (extractedAmount <= 0) {
            return false;
        }

        insertStoredFluid(representativeItem, extractedAmount);
        cauldronTile.setChanged();
        startFlaskSupplyCooldown(level.getGameTime());
        emitCauldronToStationEffect(level, cauldronTile.getBlockPos());
        return true;
    }

    private boolean exportToFlaskStorage(ServerLevel level) {
        var changed = false;

        for (var slot = 0; slot < flaskInventory.getSlots(); ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof AbstractPotionFlaskItem)) {
                continue;
            }

            var representativeItem = SpellcastersFlask.copyFilterItem(stack);
            if (representativeItem.isEmpty()) {
                continue;
            }

            changed |= fillStationFlaskFromTank(slot, representativeItem);
        }

        for (var slot = 0; slot < flaskInventory.getSlots(); ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof AbstractPotionFlaskItem)) {
                continue;
            }

            if (SpellcastersFlask.getStoredDoseCount(stack) > 0) {
                continue;
            }

            var representativeItem = getRandomStoredFluidRepresentative(level, stack);
            if (representativeItem.isEmpty()) {
                continue;
            }

            changed |= fillStationFlaskFromTank(slot, representativeItem);
        }

        return changed;
    }

    private boolean fillStationFlaskFromTank(int slot, ItemStack representativeItem) {
        var stack = flaskInventory.getStackInSlot(slot);
        if (stack.isEmpty() || representativeItem.isEmpty()) {
            return false;
        }

        var currentDoseCount = SpellcastersFlask.getStoredDoseCount(stack);
        if (currentDoseCount <= 0 && !SpellcastersFlask.canAcceptRepresentativeForAutomaticFill(stack, representativeItem)) {
            return false;
        }

        var remainingCapacity = SpellcastersFlask.getMaxDoseCapacity(stack) - currentDoseCount;
        if (remainingCapacity <= 0) {
            return false;
        }

        var movableDoseCount = Math.min(remainingCapacity, getStoredFluidDoseCount(representativeItem));
        if (movableDoseCount <= 0) {
            return false;
        }

        var updatedStack = SpellcastersFlask.copyWithAddedDoses(stack, representativeItem, movableDoseCount);
        if (updatedStack.isEmpty()) {
            return false;
        }

        var consumedAmount = consumeStoredFluid(representativeItem, movableDoseCount * MILLIBUCKETS_PER_USE);
        if (consumedAmount <= 0) {
            return false;
        }

        flaskInventory.setStackInSlot(slot, updatedStack);
        return true;
    }

    private boolean supplyNearestPlayer(ServerLevel level, BlockState state) {
        var player = findNearestPlayer(level, state);
        if (player == null) {
            return false;
        }

        var changed = false;
        var playerItems = player.getInventory().items;
        for (var slot = 0; slot < playerItems.size(); ++slot) {
            var stack = playerItems.get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof AbstractPotionFlaskItem)) {
                continue;
            }

            var storedDoseCount = SpellcastersFlask.getStoredDoseCount(stack);
            var representativeItem = SpellcastersFlask.copyFilterItem(stack);
            if (representativeItem.isEmpty()) {
                continue;
            }

            var targetStack = stack;
            var remainingCapacity = SpellcastersFlask.getMaxDoseCapacity(targetStack) - storedDoseCount;
            if (remainingCapacity <= 0) {
                continue;
            }

            var tankDoseCount = Math.min(remainingCapacity, getStoredFluidDoseCount(representativeItem));
            if (tankDoseCount > 0) {
                var updatedStack = SpellcastersFlask.copyWithAddedDoses(targetStack, representativeItem, tankDoseCount);
                if (!updatedStack.isEmpty()) {
                    var consumedAmount = consumeStoredFluid(representativeItem, tankDoseCount * MILLIBUCKETS_PER_USE);
                    if (consumedAmount > 0) {
                        targetStack = updatedStack;
                        remainingCapacity -= tankDoseCount;
                        changed = true;
                    }
                }
            }

            if (remainingCapacity > 0) {
                var extractedDoseCount = extractDoseCountFromStoredFlasks(representativeItem, remainingCapacity);
                if (extractedDoseCount > 0) {
                    var updatedStack = SpellcastersFlask.copyWithAddedDoses(targetStack, representativeItem, extractedDoseCount);
                    if (!updatedStack.isEmpty()) {
                        targetStack = updatedStack;
                        changed = true;
                    }
                }
            }

            if (!ItemStack.matches(stack, targetStack)) {
                playerItems.set(slot, targetStack);
            }
        }

        if (changed) {
            player.getInventory().setChanged();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            emitStationToPlayerEffect(level, state, player);
        }

        return changed;
    }

    private boolean isFlaskSupplyCoolingDown(long gameTime) {
        return gameTime < flaskSupplyCooldownUntilGameTime;
    }

    // 搬入直後は中身キューブの表示時間を優先し、全てのフラスコ供給を 20tick 停止する。
    private void startFlaskSupplyCooldown(long gameTime) {
        flaskSupplyCooldownUntilGameTime = Math.max(flaskSupplyCooldownUntilGameTime, gameTime + FLASK_SUPPLY_COOLDOWN_TICKS);
    }

    @Nullable
    private Player findNearestPlayer(ServerLevel level, BlockState state) {
        var searchBox = createPlayerSupplyAabb(state);
        var players = level.getEntitiesOfClass(Player.class, searchBox, player -> player.isAlive() && !player.isSpectator());
        if (players.isEmpty()) {
            return null;
        }

        var center = Vec3.atCenterOf(worldPosition);
        Player nearestPlayer = null;
        var nearestDistanceSqr = Double.MAX_VALUE;
        for (var player : players) {
            var distanceSqr = player.distanceToSqr(center);
            if (distanceSqr < nearestDistanceSqr) {
                nearestPlayer = player;
                nearestDistanceSqr = distanceSqr;
            }
        }

        return nearestPlayer;
    }

    private AABB createPlayerSupplyAabb(BlockState state) {
        var facing = state.getValue(AtelierStation.FACING);
        var center = Vec3.atCenterOf(worldPosition);
        var minX = center.x - PLAYER_SUPPLY_RANGE * Math.abs(facing.getStepZ()) + Math.min(0.0D, facing.getStepX() * PLAYER_SUPPLY_RANGE);
        var maxX = center.x + PLAYER_SUPPLY_RANGE * Math.abs(facing.getStepZ()) + Math.max(0.0D, facing.getStepX() * PLAYER_SUPPLY_RANGE);
        var minZ = center.z - PLAYER_SUPPLY_RANGE * Math.abs(facing.getStepX()) + Math.min(0.0D, facing.getStepZ() * PLAYER_SUPPLY_RANGE);
        var maxZ = center.z + PLAYER_SUPPLY_RANGE * Math.abs(facing.getStepX()) + Math.max(0.0D, facing.getStepZ() * PLAYER_SUPPLY_RANGE);
        return new AABB(
                minX,
                center.y - PLAYER_SUPPLY_RANGE,
                minZ,
                maxX,
                center.y + PLAYER_SUPPLY_RANGE,
                maxZ
        );
    }

    private int extractDoseCountFromStoredFlasks(ItemStack representativeItem, int requestedDoseCount) {
        var remainingDoseCount = requestedDoseCount;
        var extractedDoseCount = 0;
        for (var slot = 0; slot < flaskInventory.getSlots() && remainingDoseCount > 0; ++slot) {
            var stack = flaskInventory.getStackInSlot(slot);
            if (stack.isEmpty() || SpellcastersFlask.getStoredDoseCount(stack) <= 0) {
                continue;
            }

            if (!SpellcastersFlask.matchesStoredItem(stack, representativeItem)) {
                continue;
            }

            var movableDoseCount = Math.min(remainingDoseCount, SpellcastersFlask.getStoredDoseCount(stack));
            var updatedStack = SpellcastersFlask.copyAfterExtractingDoses(stack, movableDoseCount);
            if (updatedStack.isEmpty()) {
                continue;
            }

            flaskInventory.setStackInSlot(slot, updatedStack);
            remainingDoseCount -= movableDoseCount;
            extractedDoseCount += movableDoseCount;
        }

        return extractedDoseCount;
    }

    private boolean matchesAnyFilter(ItemStack representativeItem) {
        for (var filter : filters) {
            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, representativeItem)) {
                return true;
            }
        }
        return false;
    }

    private void insertStoredFluid(ItemStack representativeItem, int amountMb) {
        var normalizedAmount = normalizeFluidAmount(amountMb);
        if (representativeItem.isEmpty() || normalizedAmount <= 0) {
            return;
        }

        var previousStoredFluidAmount = storedFluidAmount;
        for (var index = 0; index < storedFluids.size(); ++index) {
            var current = storedFluids.get(index);
            if (!ItemStack.isSameItemSameComponents(current.representativeItem(), representativeItem)) {
                continue;
            }

            storedFluids.set(index, new StoredPotionEntry(representativeItem, current.amountMb() + normalizedAmount));
            storedFluidAmount = Math.min(MAX_STORED_FLUID_AMOUNT, storedFluidAmount + normalizedAmount);
            if (storedFluidAmount != previousStoredFluidAmount) {
                updateComparatorOutput();
            }
            return;
        }

        storedFluids.add(new StoredPotionEntry(representativeItem, normalizedAmount));
        storedFluidAmount = Math.min(MAX_STORED_FLUID_AMOUNT, storedFluidAmount + normalizedAmount);
        if (storedFluidAmount != previousStoredFluidAmount) {
            updateComparatorOutput();
        }
    }

    private int getStoredFluidDoseCount(ItemStack representativeItem) {
        return getStoredFluidAmount(representativeItem) / MILLIBUCKETS_PER_USE;
    }

    private int getStoredFluidAmount(ItemStack representativeItem) {
        if (representativeItem.isEmpty()) {
            return 0;
        }

        for (var entry : storedFluids) {
            if (ItemStack.isSameItemSameComponents(entry.representativeItem(), representativeItem)) {
                return entry.amountMb();
            }
        }

        return 0;
    }

    private int consumeStoredFluid(ItemStack representativeItem, int requestedAmountMb) {
        var normalizedAmount = normalizeFluidAmount(requestedAmountMb);
        if (representativeItem.isEmpty() || normalizedAmount <= 0) {
            return 0;
        }

        for (var index = 0; index < storedFluids.size(); ++index) {
            var current = storedFluids.get(index);
            if (!ItemStack.isSameItemSameComponents(current.representativeItem(), representativeItem)) {
                continue;
            }

            var consumedAmount = Math.min(current.amountMb(), normalizedAmount);
            var remainingAmount = current.amountMb() - consumedAmount;
            storedFluidAmount = Math.max(0, storedFluidAmount - consumedAmount);
            if (remainingAmount > 0) {
                storedFluids.set(index, new StoredPotionEntry(representativeItem, remainingAmount));
            } else {
                storedFluids.remove(index);
            }
            updateComparatorOutput();
            return consumedAmount;
        }

        return 0;
    }

    private ItemStack getRandomStoredFluidRepresentative(ServerLevel level, ItemStack flaskStack) {
        var candidates = new ArrayList<ItemStack>();
        for (var entry : storedFluids) {
            if (entry.amountMb() >= MILLIBUCKETS_PER_USE
                    && SpellcastersFlask.canAcceptRepresentativeForAutomaticFill(flaskStack, entry.representativeItem())) {
                candidates.add(entry.representativeItem());
            }
        }

        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return candidates.get(level.getRandom().nextInt(candidates.size())).copy();
    }

    private void emitCauldronToStationEffect(ServerLevel level, BlockPos cauldronPos) {
        var state = getBlockState();
        if (!state.hasProperty(AtelierStation.FACING)) {
            return;
        }

        Networks.sendToPlayersNear(
                level,
                Vec3.atCenterOf(worldPosition),
                EFFECT_BROADCAST_RANGE,
                AtelierStationFluidEffectPacket.createCauldronToStation(
                        worldPosition,
                        state.getValue(AtelierStation.FACING),
                        cauldronPos,
                        level.getGameTime()
                )
        );
    }

    private void emitStationToPlayerEffect(ServerLevel level, BlockState state, Player player) {
        if (!state.hasProperty(AtelierStation.FACING)) {
            return;
        }

        var orbData = createSupplyEffectOrbData(level);
        if (orbData.isEmpty()) {
            return;
        }

        Networks.sendToPlayersNear(
                level,
                Vec3.atCenterOf(worldPosition),
                EFFECT_BROADCAST_RANGE,
                AtelierStationFluidEffectPacket.createStationToPlayer(
                        worldPosition,
                        state.getValue(AtelierStation.FACING),
                        player.getId(),
                        level.getGameTime(),
                        orbData
                )
        );
    }

    private List<AtelierStationFluidEffectPacket.SupplyOrbData> createSupplyEffectOrbData(ServerLevel level) {
        var random = level.getRandom();
        var orbs = new ArrayList<AtelierStationFluidEffectPacket.SupplyOrbData>(AtelierStationFluidEffectTuning.SUPPLY_ORB_COUNT);
        for (var index = 0; index < AtelierStationFluidEffectTuning.SUPPLY_ORB_COUNT; ++index) {
            var cubeHalfExtent = AtelierStationFluidEffectTuning.SUPPLY_CONTROL_CUBE_HALF_EXTENT;
            var controlOffsetX = (float) ((random.nextDouble() * 2.0d - 1.0d) * cubeHalfExtent);
            var controlOffsetZ = (float) ((random.nextDouble() * 2.0d - 1.0d) * cubeHalfExtent);
            var controlOffsetY = (float) (AtelierStationFluidEffectTuning.SUPPLY_CONTROL_BASE_HEIGHT
                    + (random.nextDouble() * 2.0d - 1.0d) * cubeHalfExtent);
            var spinOffset = random.nextFloat() * 360.0f;
            var spinSpeed = 20.0f + random.nextFloat() * 10.0f;
            orbs.add(new AtelierStationFluidEffectPacket.SupplyOrbData(
                    controlOffsetX,
                    controlOffsetY,
                    controlOffsetZ,
                    index,
                    AtelierStationFluidEffectTuning.SUPPLY_TOTAL_TICKS,
                    spinOffset,
                    spinSpeed
            ));
        }
        return orbs;
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

    private void updateComparatorOutput() {
        if (level == null || level.isClientSide) {
            return;
        }

        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private static int normalizeFluidAmount(int amountMb) {
        if (amountMb <= 0) {
            return 0;
        }

        return Math.min(MAX_STORED_FLUID_AMOUNT, amountMb - amountMb % MILLIBUCKETS_PER_USE);
    }

    public record StoredPotionEntry(ItemStack representativeItem, int amountMb) {
        public StoredPotionEntry {
            representativeItem = SpellcastersFlask.copyFilterItem(representativeItem);
            amountMb = normalizeFluidAmount(amountMb);
        }

        public StoredPotionEntry copy() {
            return new StoredPotionEntry(representativeItem, amountMb);
        }
    }
}
