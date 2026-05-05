package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SpellDispenserBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider, SpellDispenserManaHelper.ManaAccess {
    public static final int SPELL_SLOT_INDEX = 0;
    public static final int FLASK_SLOT_START = 1;
    public static final int FLASK_SLOT_COUNT = 8;
    public static final int INVENTORY_SLOT_COUNT = FLASK_SLOT_START + FLASK_SLOT_COUNT;

    private static final String INVENTORY_TAG = "Inventory";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String CONTINUOUS_RESET_REQUIRED_TAG = "ContinuousResetRequired";
    private static final String COOLDOWN_REMAINING_TAG = "CooldownRemaining";
    private static final String CURRENT_MANA_TAG = "CurrentMana";
    private static final String REFILL_CHECK_TICKS_TAG = "RefillCheckTicks";
    private static final String MANA_POTION_FLUID_TAG = "ManaPotionFluid";

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SPELL_SLOT_INDEX) {
                return SpellDispenserSpellValidator.isPlaceableScroll(stack);
            }
            // 空瓶も残留先として許可し、補充候補の判定は Spell Dispenser 側ロジックへ集約する。
            return SpellDispenserManaHelper.isSupportedFlaskSlotItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private LazyOptional<IItemHandlerModifiable> inventoryCapability = createInventoryCapability();
    private LazyOptional<IItemHandler> automationInventoryCapability = createAutomationInventoryCapability();
    private LazyOptional<IFluidHandler> fluidCapability = createFluidCapability();
    @Nullable
    private GameProfile ownerProfile;
    @Nullable
    private SpellDispenserCastHelper.ContinuousCastSession activeContinuousCast;
    private final Map<String, Long> recentFailureNoticeTicks = new HashMap<>();
    private boolean continuousResetRequired;
    private int remainingCooldownTicks;
    private int currentMana = SpellDispenserManaHelper.MAX_MANA;
    private int refillCheckTicks;
    private FluidStack manaPotionFluid = FluidStack.EMPTY;

    public SpellDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SPELL_DISPENSER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.spell_dispenser");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new SpellDispenserMenu(containerId, inventory, this);
    }

    public @NotNull ItemStackHandler getInventory() {
        return inventory;
    }

    public @NotNull ItemStack getSpellSource() {
        return inventory.getStackInSlot(SPELL_SLOT_INDEX);
    }

    public void setOwnerProfile(@Nullable GameProfile ownerProfile) {
        this.ownerProfile = normalizeOwnerProfile(ownerProfile);
        markUpdated();
    }

    public @Nullable GameProfile getOwnerProfile() {
        return ownerProfile;
    }

    public @Nullable String getOwnerName() {
        var normalizedOwnerProfile = normalizeOwnerProfile(ownerProfile);
        return normalizedOwnerProfile != null ? normalizedOwnerProfile.getName() : null;
    }

    public boolean hasOwnerProfile() {
        return normalizeOwnerProfile(ownerProfile) != null;
    }

    public boolean hasActiveContinuousCast() {
        return activeContinuousCast != null && !activeContinuousCast.isFinished();
    }

    public boolean requiresContinuousReset() {
        return continuousResetRequired;
    }

    public boolean isCoolingDown() {
        return remainingCooldownTicks > 0;
    }

    public int getRemainingCooldownTicks() {
        return remainingCooldownTicks;
    }

    @Override
    public int getCurrentMana() {
        return currentMana;
    }

    public int getMaxMana() {
        return SpellDispenserManaHelper.MAX_MANA;
    }

    public boolean canAffordSpell(SpellData spellData) {
        return SpellDispenserManaHelper.canAffordSpell(currentMana, spellData);
    }

    public SpellDispenserCastHelper.CastResult tryActivate() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new SpellDispenserCastHelper.CastResult(
                    false,
                    SpellDispenserSpellValidator.validate(ItemStack.EMPTY),
                    null,
                    false,
                    0,
                    SpellDispenserCastHelper.FailureType.NONE,
                    0,
                    0,
                    0,
                    null
            );
        }

        var state = getBlockState();
        if (!(state.getBlock() instanceof SpellDispenser spellDispenser)) {
            return new SpellDispenserCastHelper.CastResult(
                    false,
                    SpellDispenserSpellValidator.validate(ItemStack.EMPTY),
                    null,
                    false,
                    0,
                    SpellDispenserCastHelper.FailureType.NONE,
                    0,
                    0,
                    0,
                    null
            );
        }

        var source = getSpellSource();
        var validation = SpellDispenserSpellValidator.validate(source);

        if (isCoolingDown()) {
            return notifyActivationFailure(serverLevel, SpellDispenserCastHelper.CastResult.cooldownBlocked(validation, remainingCooldownTicks));
        }

        if (source.isEmpty()) {
            return notifyActivationFailure(serverLevel, SpellDispenserCastHelper.CastResult.noScroll(validation));
        }

        if (requiresOwnerProfile(validation) && !hasOwnerProfile()) {
            return notifyActivationFailure(serverLevel, SpellDispenserCastHelper.CastResult.missingOwnerProfile(validation));
        }

        if (hasActiveContinuousCast()) {
            return notifyActivationFailure(serverLevel, SpellDispenserCastHelper.CastResult.validationFailure(validation));
        }

        var spellData = validation.spellData();
        if (spellData != SpellData.EMPTY && spellData.getSpell().getCastType() == CastType.CONTINUOUS) {
            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    serverLevel,
                    worldPosition,
                    spellDispenser.getFacing(state),
                    validation,
                    source.copy(),
                    ownerProfile,
                    this
            );
            if (startResult.result().succeeded()) {
                startContinuousCast(startResult.session());
            }
            return notifyActivationFailure(serverLevel, startResult.result());
        }

        var result = SpellDispenserCastHelper.tryCast(
                serverLevel,
                worldPosition,
                spellDispenser.getFacing(state),
                source.copy(),
                ownerProfile,
                this
        );
        startCooldown(result.cooldownTicks());
        return notifyActivationFailure(serverLevel, result);
    }

    private SpellDispenserCastHelper.CastResult notifyActivationFailure(
            ServerLevel serverLevel,
            SpellDispenserCastHelper.CastResult result
    ) {
        SpellDispenserCastHelper.notifyFailureToNearbyPlayers(serverLevel, worldPosition, result, recentFailureNoticeTicks);
        return result;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SpellDispenserBlockEntity blockEntity) {
        blockEntity.serverTick(level, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        tryConsumeStoredManaPotionFluid();
        tickCooldown();
        tickRefillCheck();

        if (activeContinuousCast == null) {
            return;
        }

        if (activeContinuousCast.isFinished()) {
            startCooldown(activeContinuousCast.consumeFinishedCooldownTicks());
            activeContinuousCast = null;
            continuousResetRequired = state.getValue(SpellDispenser.TRIGGERED);
            setChanged();
            return;
        }

        if (!(state.getBlock() instanceof SpellDispenser)) {
            stopContinuousCast(true);
            return;
        }

        if (!state.getValue(SpellDispenser.TRIGGERED)) {
            stopContinuousCast(true);
            return;
        }

        if (activeContinuousCast.profile().ownerRequired() && !hasOwnerProfile()) {
            stopContinuousCast(true);
            return;
        }

        var source = getSpellSource();
        if (source.isEmpty()
                || source.getCount() != activeContinuousCast.spellSource().getCount()
                || !ItemStack.isSameItemSameTags(source, activeContinuousCast.spellSource())) {
            stopContinuousCast(true);
            return;
        }

        if (!SpellDispenserCastHelper.tickContinuousCast(level, activeContinuousCast)) {
            startCooldown(activeContinuousCast.consumeFinishedCooldownTicks());
            activeContinuousCast = null;
            continuousResetRequired = state.getValue(SpellDispenser.TRIGGERED);
            setChanged();
        }
    }

    public void startContinuousCast(@Nullable SpellDispenserCastHelper.ContinuousCastSession session) {
        activeContinuousCast = session;
        continuousResetRequired = false;
        setChanged();
    }

    public void stopContinuousCast(boolean cancelled) {
        if (!(level instanceof ServerLevel serverLevel)) {
            activeContinuousCast = null;
            continuousResetRequired = false;
            return;
        }

        if (activeContinuousCast != null) {
            SpellDispenserCastHelper.finishContinuousCast(serverLevel, activeContinuousCast, cancelled);
            startCooldown(activeContinuousCast.consumeFinishedCooldownTicks());
            activeContinuousCast = null;
        }
        continuousResetRequired = false;
        setChanged();
    }

    public void clearContinuousResetRequired() {
        if (!continuousResetRequired) {
            return;
        }

        continuousResetRequired = false;
        setChanged();
    }

    public void dropStoredItems() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (var slot = 0; slot < inventory.getSlots(); ++slot) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        stopContinuousCast(true);
        super.setRemoved();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCapability.invalidate();
        automationInventoryCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inventoryCapability = createInventoryCapability();
        automationInventoryCapability = createAutomationInventoryCapability();
        fluidCapability = createFluidCapability();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        saveOwnerProfile(tag, ownerProfile);
        tag.putBoolean(CONTINUOUS_RESET_REQUIRED_TAG, continuousResetRequired);
        tag.putInt(COOLDOWN_REMAINING_TAG, remainingCooldownTicks);
        saveCurrentMana(tag, currentMana);
        saveRefillCheckTicks(tag, refillCheckTicks);
        if (!manaPotionFluid.isEmpty()) {
            tag.put(MANA_POTION_FLUID_TAG, manaPotionFluid.writeToNBT(new CompoundTag()));
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        ownerProfile = readOwnerProfile(tag);
        continuousResetRequired = tag.getBoolean(CONTINUOUS_RESET_REQUIRED_TAG);
        remainingCooldownTicks = Math.max(0, tag.getInt(COOLDOWN_REMAINING_TAG));
        currentMana = readCurrentMana(tag);
        refillCheckTicks = readRefillCheckTicks(tag);
        manaPotionFluid = readManaPotionFluid(tag);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            // Create の mounted storage は side == null の capability を読みに来るため、
            // そこでは scroll を含む内部インベントリを返し、面指定の automation だけを制限する。
            return (side == null ? inventoryCapability : automationInventoryCapability).cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void setCurrentMana(int mana) {
        var normalizedMana = SpellDispenserManaHelper.clampMana(mana);
        if (currentMana == normalizedMana) {
            return;
        }

        currentMana = normalizedMana;
        updateComparatorOutput();
        markUpdated();
    }

    @Override
    public int getInventorySlotCount() {
        return inventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getInventoryStack(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public void setInventoryStack(int slot, @NotNull ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    public @NotNull FluidStack getStoredManaPotionFluid() {
        return manaPotionFluid.copy();
    }

    private static boolean requiresOwnerProfile(SpellDispenserSpellValidator.ValidationResult validation) {
        return SpellDispenserSpellProfileManager.requiresOwner(validation.spellData());
    }

    private void markUpdated() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void startCooldown(int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }

        remainingCooldownTicks = cooldownTicks;
        setChanged();
    }

    public void clearCooldown() {
        if (remainingCooldownTicks == 0) {
            return;
        }

        remainingCooldownTicks = 0;
        setChanged();
    }

    private void tickCooldown() {
        if (remainingCooldownTicks > 0) {
            remainingCooldownTicks--;
            setChanged();
        }
    }

    private void tickRefillCheck() {
        refillCheckTicks++;
        if (refillCheckTicks < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS) {
            setChanged();
            return;
        }

        refillCheckTicks = 0;
        setChanged();
        SpellDispenserManaHelper.tryRefillMana(this);
    }

    public static void saveOwnerProfile(@NotNull CompoundTag tag, @Nullable GameProfile ownerProfile) {
        var normalizedOwnerProfile = normalizeOwnerProfile(ownerProfile);
        if (normalizedOwnerProfile == null) {
            return;
        }

        tag.putUUID(OWNER_UUID_TAG, normalizedOwnerProfile.getId());
        tag.putString(OWNER_NAME_TAG, normalizedOwnerProfile.getName());
    }

    public static @Nullable GameProfile readOwnerProfile(@Nullable CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        if (!tag.hasUUID(OWNER_UUID_TAG) || !tag.contains(OWNER_NAME_TAG, net.minecraft.nbt.Tag.TAG_STRING)) {
            return null;
        }
        return normalizeOwnerProfile(new GameProfile(tag.getUUID(OWNER_UUID_TAG), tag.getString(OWNER_NAME_TAG)));
    }

    public static @Nullable String readOwnerName(@Nullable CompoundTag tag) {
        var ownerProfile = readOwnerProfile(tag);
        return ownerProfile != null ? ownerProfile.getName() : null;
    }

    public static void saveCurrentMana(@NotNull CompoundTag tag, int currentMana) {
        tag.putInt(CURRENT_MANA_TAG, SpellDispenserManaHelper.clampMana(currentMana));
    }

    public static int readCurrentMana(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(CURRENT_MANA_TAG, net.minecraft.nbt.Tag.TAG_INT)) {
            return SpellDispenserManaHelper.MAX_MANA;
        }
        return SpellDispenserManaHelper.clampMana(tag.getInt(CURRENT_MANA_TAG));
    }

    public static void saveRefillCheckTicks(@NotNull CompoundTag tag, int refillCheckTicks) {
        tag.putInt(REFILL_CHECK_TICKS_TAG, Math.max(0, refillCheckTicks));
    }

    public static int readRefillCheckTicks(@Nullable CompoundTag tag) {
        if (tag == null) {
            return 0;
        }
        return Math.max(0, tag.getInt(REFILL_CHECK_TICKS_TAG));
    }

    public static @NotNull FluidStack readManaPotionFluid(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(MANA_POTION_FLUID_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return FluidStack.EMPTY;
        }

        var fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(MANA_POTION_FLUID_TAG));
        if (!SpellDispenserManaFluidHelper.isSupportedManaPotionFluid(fluid)) {
            return FluidStack.EMPTY;
        }

        fluid.setAmount(Math.min(fluid.getAmount(), SpellDispenserManaFluidHelper.CAPACITY_MB));
        return fluid;
    }

    private LazyOptional<IItemHandlerModifiable> createInventoryCapability() {
        return LazyOptional.of(() -> inventory);
    }

    private LazyOptional<IItemHandler> createAutomationInventoryCapability() {
        return LazyOptional.of(AutomationInventoryHandler::new);
    }

    private LazyOptional<IFluidHandler> createFluidCapability() {
        return LazyOptional.of(SpellDispenserFluidHandler::new);
    }

    private void setManaPotionFluid(@NotNull FluidStack fluidStack) {
        var normalized = fluidStack.copy();
        if (normalized.isEmpty() || normalized.getAmount() <= 0) {
            normalized = FluidStack.EMPTY;
        } else if (normalized.getAmount() > SpellDispenserManaFluidHelper.CAPACITY_MB) {
            normalized.setAmount(SpellDispenserManaFluidHelper.CAPACITY_MB);
        }

        manaPotionFluid = normalized;
        markUpdated();
    }

    private void tryConsumeStoredManaPotionFluid() {
        if (manaPotionFluid.isEmpty()) {
            return;
        }

        var changed = false;
        while (manaPotionFluid.getAmount() >= SpellDispenserManaFluidHelper.DOSE_MB) {
            var dose = manaPotionFluid.copy();
            dose.setAmount(SpellDispenserManaFluidHelper.DOSE_MB);
            var recoveredMana = SpellDispenserManaFluidHelper.getManaRecovery(dose);
            if (recoveredMana <= 0) {
                break;
            }

            var current = SpellDispenserManaHelper.clampMana(currentMana);
            if (recoveredMana > SpellDispenserManaHelper.MAX_MANA - current) {
                break;
            }

            currentMana = current + recoveredMana;
            manaPotionFluid.shrink(SpellDispenserManaFluidHelper.DOSE_MB);
            changed = true;
        }

        if (manaPotionFluid.isEmpty() || manaPotionFluid.getAmount() <= 0) {
            manaPotionFluid = FluidStack.EMPTY;
        }
        if (changed) {
            updateComparatorOutput();
            markUpdated();
        }
    }

    private void updateComparatorOutput() {
        if (level == null || level.isClientSide) {
            return;
        }

        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private final class SpellDispenserFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? manaPotionFluid.copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? SpellDispenserManaFluidHelper.CAPACITY_MB : 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && canAcceptFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canAcceptFluid(resource)) {
                return 0;
            }

            var acceptedAmount = Math.min(resource.getAmount(), SpellDispenserManaFluidHelper.CAPACITY_MB - manaPotionFluid.getAmount());
            if (acceptedAmount <= 0) {
                return 0;
            }

            if (action.execute()) {
                var updated = manaPotionFluid.isEmpty() ? resource.copy() : manaPotionFluid.copy();
                updated.setAmount(manaPotionFluid.getAmount() + acceptedAmount);
                setManaPotionFluid(updated);
                tryConsumeStoredManaPotionFluid();
            }

            return acceptedAmount;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || manaPotionFluid.isEmpty()
                    || !SpellDispenserManaFluidHelper.isSameFluidAndTags(manaPotionFluid, resource)) {
                return FluidStack.EMPTY;
            }

            return drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || manaPotionFluid.isEmpty()) {
                return FluidStack.EMPTY;
            }

            var drainedAmount = Math.min(maxDrain, manaPotionFluid.getAmount());
            var drained = manaPotionFluid.copy();
            drained.setAmount(drainedAmount);
            if (action.execute()) {
                var updated = manaPotionFluid.copy();
                updated.shrink(drainedAmount);
                setManaPotionFluid(updated);
            }
            return drained;
        }

        private boolean canAcceptFluid(@NotNull FluidStack stack) {
            if (stack.isEmpty() || !SpellDispenserManaFluidHelper.isSupportedManaPotionFluid(stack)) {
                return false;
            }

            return manaPotionFluid.isEmpty() || SpellDispenserManaFluidHelper.isSameFluidAndTags(manaPotionFluid, stack);
        }
    }

    private final class AutomationInventoryHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return isValidAutomationSlot(slot) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isValidAutomationInsert(slot, stack)) {
                return stack;
            }

            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return stack;
            }

            var inserted = stack.copyWithCount(1);
            if (!simulate) {
                inventory.setStackInSlot(slot, inserted);
            }

            var remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isValidAutomationSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }

            var current = inventory.getStackInSlot(slot);
            if (!SpellDispenserManaHelper.canAutomationExtract(current)) {
                return ItemStack.EMPTY;
            }

            var extracted = current.copyWithCount(Math.min(amount, current.getCount()));
            if (!simulate) {
                if (current.getCount() <= extracted.getCount()) {
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    var remainder = current.copy();
                    remainder.shrink(extracted.getCount());
                    inventory.setStackInSlot(slot, remainder);
                }
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (!isValidAutomationSlot(slot)) {
                return 0;
            }
            return slot == SPELL_SLOT_INDEX ? 0 : inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isValidAutomationInsert(slot, stack);
        }

        private boolean isValidAutomationSlot(int slot) {
            return slot >= 0 && slot < inventory.getSlots();
        }

        private boolean isValidAutomationInsert(int slot, @NotNull ItemStack stack) {
            return isValidAutomationSlot(slot)
                    && slot != SPELL_SLOT_INDEX
                    && SpellDispenserManaHelper.isAutomationInputItem(stack);
        }
    }

    private static @Nullable GameProfile normalizeOwnerProfile(@Nullable GameProfile ownerProfile) {
        if (ownerProfile == null || ownerProfile.getId() == null || ownerProfile.getName() == null || ownerProfile.getName().isBlank()) {
            return null;
        }
        return new GameProfile(ownerProfile.getId(), ownerProfile.getName());
    }
}
