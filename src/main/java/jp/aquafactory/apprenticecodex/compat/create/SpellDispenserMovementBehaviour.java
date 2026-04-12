package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.dispenser.MountedDispenseBehavior;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SpellDispenserMovementBehaviour implements MovementBehaviour {
    private static final String CONTINUOUS_STATE_TAG = "SpellDispenserContinuousState";
    private static final String CONTINUOUS_RESET_REQUIRED_TAG = "SpellDispenserContinuousResetRequired";
    private static final String LAST_POWERED_TAG = "SpellDispenserContinuousLastPowered";
    private static final String COOLDOWN_REMAINING_TAG = "CooldownRemaining";
    private static final String CURRENT_MANA_TAG = "CurrentMana";
    private static final String REFILL_CHECK_TICKS_TAG = "RefillCheckTicks";
    private static final int CONTINUOUS_STATE_IDLE = 0;
    private static final int CONTINUOUS_STATE_CASTING = 1;
    private static final int CONTINUOUS_STATE_WAITING_FOR_RESET = 2;

    @Override
    public void startMoving(MovementContext context) {
        context.temporaryData = new RuntimeState();
        setContinuousState(context, getContinuousState(context) == CONTINUOUS_STATE_WAITING_FOR_RESET
                ? CONTINUOUS_STATE_WAITING_FOR_RESET
                : CONTINUOUS_STATE_IDLE);
        setContinuousResetRequired(context, getContinuousState(context) == CONTINUOUS_STATE_WAITING_FOR_RESET);
        setRemainingCooldownTicks(context, Math.max(0, context.blockEntityData.getInt(COOLDOWN_REMAINING_TAG)));
        setCurrentMana(context, SpellDispenserBlockEntity.readCurrentMana(context.blockEntityData));
        setRefillCheckTicks(context, SpellDispenserBlockEntity.readRefillCheckTicks(context.blockEntityData));
        setLastPowered(context, false);
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        var spellSource = getSpellSource(context);
        if (spellSource.isEmpty()) {
            var result = SpellDispenserCastHelper.CastResult.noScroll(SpellDispenserSpellValidator.validate(spellSource));
            notifyFailure(serverLevel, pos, context, result);
            playActivationSound(serverLevel, pos, result);
            return;
        }

        var ownerProfile = SpellDispenserBlockEntity.readOwnerProfile(context.blockEntityData);
        if (ownerProfile == null) {
            var result = SpellDispenserCastHelper.CastResult.missingOwnerProfile(SpellDispenserSpellValidator.validate(spellSource));
            notifyFailure(serverLevel, pos, context, result);
            playActivationSound(serverLevel, pos, result);
            return;
        }

        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (!validation.isSupported()) {
            var result = SpellDispenserCastHelper.CastResult.validationFailure(validation);
            notifyFailure(serverLevel, pos, context, result);
            playActivationSound(serverLevel, pos, result);
            return;
        }

        var spellData = validation.spellData();
        var castType = spellData.getSpell().getCastType();
        if (castType == CastType.CONTINUOUS) {
            return;
        }
        if (castType != CastType.INSTANT && castType != CastType.LONG) {
            var result = SpellDispenserCastHelper.CastResult.validationFailure(validation);
            notifyFailure(serverLevel, pos, context, result);
            playActivationSound(serverLevel, pos, result);
            return;
        }

        if (isCoolingDown(context)) {
            var result = SpellDispenserCastHelper.CastResult.cooldownBlocked(validation, getRemainingCooldownTicks(context));
            notifyFailure(serverLevel, pos, context, result);
            playActivationSound(serverLevel, pos, result);
            return;
        }

        var forward = resolveForward(context);
        var castResult = SpellDispenserCastHelper.tryCast(serverLevel, pos, forward, spellSource.copy(), ownerProfile, new MountedManaAccess(context));
        notifyFailure(serverLevel, pos, context, castResult);
        playActivationSound(serverLevel, pos, castResult);
        startCooldown(context, castResult.cooldownTicks());
    }

    @Override
    public void tick(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        tickCooldown(context);
        tickRefillCheck(context);
        var runtime = getContinuousRuntime(context);
        if (!isContinuousSpell(context)) {
            finishContinuousCast(serverLevel, context, runtime, true);
            setContinuousState(context, CONTINUOUS_STATE_IDLE);
            setContinuousResetRequired(context, false);
            setLastPowered(context, !context.disabled);
            return;
        }

        var powered = !context.disabled;
        if (!powered) {
            finishContinuousCast(serverLevel, context, runtime, true);
            setContinuousState(context, CONTINUOUS_STATE_IDLE);
            setContinuousResetRequired(context, false);
            setLastPowered(context, false);
            return;
        }

        if (runtime != null) {
            if (!canKeepContinuousCast(context, runtime.session())) {
                finishContinuousCast(serverLevel, context, runtime, true);
                setContinuousState(context, CONTINUOUS_STATE_IDLE);
                setContinuousResetRequired(context, false);
                setLastPowered(context, true);
                return;
            }

            SpellDispenserCastHelper.syncContinuousCastTransform(runtime.session(), resolveCastBasePosition(context), resolveForward(context));
            if (!SpellDispenserCastHelper.tickContinuousCast(serverLevel, runtime.session())) {
                startCooldown(context, runtime.session().consumeFinishedCooldownTicks());
                getOrCreateRuntimeState(context).setContinuousRuntime(null);
                setContinuousState(context, CONTINUOUS_STATE_WAITING_FOR_RESET);
                setContinuousResetRequired(context, true);
                setLastPowered(context, true);
                return;
            }

            setContinuousState(context, CONTINUOUS_STATE_CASTING);
            setContinuousResetRequired(context, false);
            setLastPowered(context, true);
            return;
        }

        if (requiresContinuousReset(context)) {
            setLastPowered(context, true);
            return;
        }

        if (wasPoweredLastTick(context)) {
            setLastPowered(context, true);
            return;
        }

        var spellSource = getSpellSource(context);
        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (isCoolingDown(context)) {
            var result = SpellDispenserCastHelper.CastResult.cooldownBlocked(validation, getRemainingCooldownTicks(context));
            notifyFailure(serverLevel, resolveSoundPos(context), context, result);
            playActivationSound(serverLevel, resolveSoundPos(context), result);
            setLastPowered(context, true);
            return;
        }

        var ownerProfile = SpellDispenserBlockEntity.readOwnerProfile(context.blockEntityData);
        if (ownerProfile == null) {
            var result = SpellDispenserCastHelper.CastResult.missingOwnerProfile(validation);
            notifyFailure(serverLevel, resolveSoundPos(context), context, result);
            playActivationSound(serverLevel, resolveSoundPos(context), result);
            setLastPowered(context, true);
            return;
        }

        var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                serverLevel,
                resolveCastBasePosition(context),
                resolveForward(context),
                validation,
                spellSource.copy(),
                ownerProfile,
                new MountedManaAccess(context)
        );
        notifyFailure(serverLevel, resolveSoundPos(context), context, startResult.result());
        playActivationSound(serverLevel, resolveSoundPos(context), startResult.result());
        if (startResult.result().succeeded() && startResult.session() != null) {
            getOrCreateRuntimeState(context).setContinuousRuntime(new ContinuousRuntime(startResult.session()));
            setContinuousState(context, CONTINUOUS_STATE_CASTING);
            setContinuousResetRequired(context, false);
        } else {
            setContinuousState(context, CONTINUOUS_STATE_IDLE);
            setContinuousResetRequired(context, false);
        }
        setLastPowered(context, true);
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world instanceof ServerLevel serverLevel) {
            finishContinuousCast(serverLevel, context, getContinuousRuntime(context), true);
        } else {
            context.temporaryData = new RuntimeState();
        }

        setContinuousState(context, CONTINUOUS_STATE_IDLE);
        setContinuousResetRequired(context, false);
        setLastPowered(context, false);
    }

    @Override
    public @NotNull ItemStack canBeDisabledVia(MovementContext context) {
        // ContraptionControls のフィルタは「何のアクターか」で安定させる。
        // 中身の scroll を返すと Create 側 UI から SpellDispenser を選べなくなる。
        return new ItemStack(ItemRegistry.SPELL_DISPENSER.get());
    }

    public static boolean hasRunningContinuousCast(MovementContext context) {
        return getContinuousRuntime(context) != null;
    }

    public static boolean requiresContinuousReset(MovementContext context) {
        return context.data.getBoolean(CONTINUOUS_RESET_REQUIRED_TAG);
    }

    public static boolean isCoolingDown(MovementContext context) {
        return getRemainingCooldownTicks(context) > 0;
    }

    private static ItemStack getSpellSource(MovementContext context) {
        var itemStorage = context.getItemStorage();
        if (itemStorage == null || itemStorage.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }

        return itemStorage.getStackInSlot(0);
    }

    private static Vec3 resolveForward(MovementContext context) {
        return MountedDispenseBehavior.getDispenserNormal(context);
    }

    private static Vec3 resolveCastBasePosition(MovementContext context) {
        if (context.position != null) {
            return context.position;
        }
        return Vec3.atCenterOf(context.localPos);
    }

    private static BlockPos resolveSoundPos(MovementContext context) {
        if (context.position != null) {
            return BlockPos.containing(context.position);
        }
        return context.localPos;
    }

    private static boolean isContinuousSpell(MovementContext context) {
        var validation = SpellDispenserSpellValidator.validate(getSpellSource(context));
        return validation.isSupported()
                && validation.spellData().getSpell().getCastType() == CastType.CONTINUOUS;
    }

    private static boolean canKeepContinuousCast(
            MovementContext context,
            SpellDispenserCastHelper.ContinuousCastSession session
    ) {
        var ownerProfile = SpellDispenserBlockEntity.readOwnerProfile(context.blockEntityData);
        var source = getSpellSource(context);
        return ownerProfile != null
                && !source.isEmpty()
                && source.getCount() == session.spellSource().getCount()
                && ItemStack.isSameItemSameTags(source, session.spellSource());
    }

    private static void finishContinuousCast(
            ServerLevel level,
            MovementContext context,
            ContinuousRuntime runtime,
            boolean cancelled
    ) {
        if (runtime != null) {
            SpellDispenserCastHelper.finishContinuousCast(level, runtime.session(), cancelled);
            startCooldown(context, runtime.session().consumeFinishedCooldownTicks());
            getOrCreateRuntimeState(context).setContinuousRuntime(null);
        }
    }

    private static int getContinuousState(MovementContext context) {
        return context.data.getInt(CONTINUOUS_STATE_TAG);
    }

    private static void setContinuousState(MovementContext context, int state) {
        context.data.putInt(CONTINUOUS_STATE_TAG, state);
    }

    private static boolean wasPoweredLastTick(MovementContext context) {
        return context.data.getBoolean(LAST_POWERED_TAG);
    }

    private static void setLastPowered(MovementContext context, boolean powered) {
        context.data.putBoolean(LAST_POWERED_TAG, powered);
    }

    private static void setContinuousResetRequired(MovementContext context, boolean required) {
        context.data.putBoolean(CONTINUOUS_RESET_REQUIRED_TAG, required);
    }

    private static int getRemainingCooldownTicks(MovementContext context) {
        return Math.max(0, context.data.getInt(COOLDOWN_REMAINING_TAG));
    }

    private static void setRemainingCooldownTicks(MovementContext context, int cooldownTicks) {
        var normalized = Math.max(0, cooldownTicks);
        context.data.putInt(COOLDOWN_REMAINING_TAG, normalized);
        context.blockEntityData.putInt(COOLDOWN_REMAINING_TAG, normalized);
    }

    private static int getCurrentMana(MovementContext context) {
        if (context.data.contains(CURRENT_MANA_TAG)) {
            return SpellDispenserManaHelper.clampMana(context.data.getInt(CURRENT_MANA_TAG));
        }
        return SpellDispenserBlockEntity.readCurrentMana(context.blockEntityData);
    }

    private static void setCurrentMana(MovementContext context, int currentMana) {
        var normalized = SpellDispenserManaHelper.clampMana(currentMana);
        context.data.putInt(CURRENT_MANA_TAG, normalized);
        SpellDispenserBlockEntity.saveCurrentMana(context.blockEntityData, normalized);
    }

    private static int getRefillCheckTicks(MovementContext context) {
        if (context.data.contains(REFILL_CHECK_TICKS_TAG)) {
            return Math.max(0, context.data.getInt(REFILL_CHECK_TICKS_TAG));
        }
        return SpellDispenserBlockEntity.readRefillCheckTicks(context.blockEntityData);
    }

    private static void setRefillCheckTicks(MovementContext context, int refillCheckTicks) {
        var normalized = Math.max(0, refillCheckTicks);
        context.data.putInt(REFILL_CHECK_TICKS_TAG, normalized);
        SpellDispenserBlockEntity.saveRefillCheckTicks(context.blockEntityData, normalized);
    }

    private static void startCooldown(MovementContext context, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }

        setRemainingCooldownTicks(context, cooldownTicks);
    }

    private static void tickCooldown(MovementContext context) {
        var remaining = getRemainingCooldownTicks(context);
        if (remaining <= 0) {
            return;
        }

        setRemainingCooldownTicks(context, remaining - 1);
    }

    private static void tickRefillCheck(MovementContext context) {
        var refillCheckTicks = getRefillCheckTicks(context) + 1;
        if (refillCheckTicks < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS) {
            setRefillCheckTicks(context, refillCheckTicks);
            return;
        }

        setRefillCheckTicks(context, 0);
        tryRefillMana(context);
    }

    private static boolean tryRefillMana(MovementContext context) {
        if (SpellDispenserManaHelper.tryRefillMana(new MountedManaAccess(context))) {
            return true;
        }

        return tryRefillManaFromMountedFuelStorage(context);
    }

    private static boolean tryRefillManaFromMountedFuelStorage(MovementContext context) {
        var remainingCapacity = SpellDispenserManaHelper.MAX_MANA - getCurrentMana(context);
        if (remainingCapacity <= 0 || context.contraption == null) {
            return false;
        }

        var storageManager = context.contraption.getStorage();
        if (storageManager == null) {
            return false;
        }

        var fuelItems = storageManager.getFuelItems();
        if (fuelItems == null) {
            return false;
        }

        ExternalRefillCandidate bestCandidate = null;
        // fuelItems は Create 側の exposed / fuel-usable storage だけを残した集合。
        // Spell Dispenser 自身の storage は localPos で除外し、保護対象の Vault 等には触らない。
        for (var entry : fuelItems.storages.entrySet()) {
            var storagePos = entry.getKey();
            if (context.localPos.equals(storagePos)) {
                continue;
            }

            var storage = entry.getValue();
            for (var slot = 0; slot < storage.getSlots(); ++slot) {
                var stack = storage.getStackInSlot(slot);
                var recoveredMana = SpellDispenserManaHelper.getAutomationInputManaRecovery(stack);
                if (recoveredMana <= 0 || recoveredMana > remainingCapacity) {
                    continue;
                }

                var remainder = SpellDispenserManaHelper.consumeAutomationInput(stack);
                if (remainder.isEmpty() && !stack.isEmpty()) {
                    continue;
                }

                if (bestCandidate == null || recoveredMana > bestCandidate.recoveredMana()) {
                    bestCandidate = new ExternalRefillCandidate(storage, slot, recoveredMana, remainder);
                }
            }
        }

        if (bestCandidate == null) {
            return false;
        }

        bestCandidate.storage().setStackInSlot(bestCandidate.slot(), bestCandidate.remainder());
        setCurrentMana(context, getCurrentMana(context) + bestCandidate.recoveredMana());
        return true;
    }

    private static ContinuousRuntime getContinuousRuntime(MovementContext context) {
        var state = getOrCreateRuntimeState(context);
        return state.continuousRuntime();
    }

    private static RuntimeState getOrCreateRuntimeState(MovementContext context) {
        if (context.temporaryData instanceof RuntimeState state) {
            return state;
        }
        var state = new RuntimeState();
        context.temporaryData = state;
        return state;
    }

    private static void notifyFailure(
            ServerLevel level,
            BlockPos pos,
            MovementContext context,
            SpellDispenserCastHelper.CastResult result
    ) {
        SpellDispenserCastHelper.notifyFailureToNearbyPlayers(
                level,
                pos,
                result,
                getOrCreateRuntimeState(context).recentFailureNoticeTicks()
        );
    }

    private static void playActivationSound(ServerLevel level, BlockPos pos, SpellDispenserCastHelper.CastResult result) {
        if (result.succeeded()) {
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        if (!result.reachedOnCast()) {
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private record ContinuousRuntime(SpellDispenserCastHelper.ContinuousCastSession session) {
    }

    private record ExternalRefillCandidate(
            MountedItemStorage storage,
            int slot,
            int recoveredMana,
            ItemStack remainder
    ) {
    }

    private static final class RuntimeState {
        @Nullable
        private ContinuousRuntime continuousRuntime;
        private final Map<String, Long> recentFailureNoticeTicks = new HashMap<>();

        private @Nullable ContinuousRuntime continuousRuntime() {
            return continuousRuntime;
        }

        private Map<String, Long> recentFailureNoticeTicks() {
            return recentFailureNoticeTicks;
        }

        private void setContinuousRuntime(@Nullable ContinuousRuntime continuousRuntime) {
            this.continuousRuntime = continuousRuntime;
        }
    }

    private record MountedManaAccess(MovementContext context) implements SpellDispenserManaHelper.ManaAccess {

        @Override
            public int getCurrentMana() {
                return SpellDispenserMovementBehaviour.getCurrentMana(context);
            }

            @Override
            public void setCurrentMana(int mana) {
                SpellDispenserMovementBehaviour.setCurrentMana(context, mana);
            }

            @Override
            public int getInventorySlotCount() {
                var itemStorage = context.getItemStorage();
                return itemStorage == null ? 0 : itemStorage.getSlots();
            }

            @Override
            public @NotNull ItemStack getInventoryStack(int slot) {
                var itemStorage = context.getItemStorage();
                if (itemStorage == null || slot < 0 || slot >= itemStorage.getSlots()) {
                    return ItemStack.EMPTY;
                }
                return itemStorage.getStackInSlot(slot);
            }

            @Override
            public void setInventoryStack(int slot, @NotNull ItemStack stack) {
                var itemStorage = context.getItemStorage();
                if (itemStorage == null || slot < 0 || slot >= itemStorage.getSlots()) {
                    return;
                }
                itemStorage.setStackInSlot(slot, stack);
            }
        }
}
