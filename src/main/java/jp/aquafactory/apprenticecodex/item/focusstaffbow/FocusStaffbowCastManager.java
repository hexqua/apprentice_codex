                                                           package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowCastStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowPresentationPacket;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FocusStaffbowCastManager {
    private static final UUID OVERCHARGE_SPELL_POWER_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");
    private static final String OVERCHARGE_SPELL_POWER_MODIFIER_NAME = "apprenticecodex.focus_staffbow.overcharge";

    private FocusStaffbowCastManager() {
    }

    public static boolean handleSelectedSpellInput(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return handleResolvedInput(serverPlayer, resolveSelection(serverPlayer), stack);
    }

    public static boolean hasActiveContinuousCast(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(serverPlayer);
        if (codexSpellData == null) {
            return false;
        }

        return codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous();
    }

    public static boolean hasOutstandingLoan(net.minecraft.world.entity.player.Player player) {
        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        return codexSpellData != null
                && codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).hasOutstandingLoan();
    }

    public static float getOutstandingLoanMana(net.minecraft.world.entity.player.Player player) {
        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return 0.0F;
        }

        return codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE).remainingLoanMana;
    }

    public static void tickLoanRepayment(ServerPlayer player) {
        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return;
        }

        var loanState = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
        if (!loanState.hasOutstandingLoan()) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() <= 0.0F) {
            return;
        }

        var repaidMana = Math.min(magicData.getMana(), loanState.remainingLoanMana);
        if (repaidMana <= 0.0F) {
            return;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - repaidMana));
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, state -> state.repay(repaidMana));
        syncManaToClient(player, magicData);
    }

    public static void releasePendingCast(net.minecraft.world.entity.player.Player player, ItemStack stack, int drawDuration) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(stack.getItem() instanceof FocusStaffbow)) {
            return;
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(serverPlayer);
        if (codexSpellData == null) {
            return;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isPending()) {
            return;
        }

        var spell = SpellRegistry.getSpell(state.spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            clearFocusStaffbowState(serverPlayer, true, true);
            return;
        }

        var currentGameTime = serverPlayer.level().getGameTime();
        var totalCastTicks = Math.max(state.getElapsedTicks(currentGameTime), Math.max(0, drawDuration));
        if (totalCastTicks < state.requiredCastTicks) {
            clearPendingCastState(serverPlayer, state, true);
            return;
        }

        confirmPendingCast(serverPlayer, stack, spell, state, codexSpellData, totalCastTicks, currentGameTime);
    }

    public static void releaseContinuousCast(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(stack.getItem() instanceof FocusStaffbow)) {
            return;
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(serverPlayer);
        if (codexSpellData == null) {
            return;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (state.isContinuous()) {
            stopContinuousCast(serverPlayer, state, true, true, true);
        }
    }

    public static void tick(ServerPlayer player) {
        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isActive()) {
            return;
        }

        var spell = SpellRegistry.getSpell(state.spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            clearFocusStaffbowState(player, true, true);
            return;
        }

        if (state.isContinuous()) {
            tickContinuousCast(player, state, spell);
            return;
        }

        if (shouldCancelFocusStaffbowState(player, state)) {
            clearPendingCastState(player, state, true);
            return;
        }

        if (!state.preCastStarted) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        syncPendingMagicDataSimulation(
                magicData,
                spell,
                state.spellLevel,
                resolveCastSource(state.castSource),
                state.requiredCastTicks,
                state.getElapsedTicks(player.level().getGameTime()),
                player.getMainHandItem()
        );
        spell.onServerCastTick(player.level(), state.spellLevel, player, magicData);
    }

    public static void cancelPendingCast(ServerPlayer player) {
        clearFocusStaffbowState(player, true, true);
    }

    public static void resetPendingState(ServerPlayer player, boolean syncClientCancel) {
        clearFocusStaffbowState(player, false, syncClientCancel);
    }

    public static boolean shouldBypassMagicManager(MagicData magicData) {
        if (!(magicData.getPlayerCastingItem().getItem() instanceof FocusStaffbow)) {
            return false;
        }

        var serverPlayer = ((MagicDataAccessor) magicData).apprenticecodex$getServerPlayer();
        if (serverPlayer == null) {
            return false;
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(serverPlayer);
        if (codexSpellData == null) {
            return false;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        return state.isContinuous()
                && magicData.getSyncedData().isCasting()
                && state.spellId.equals(magicData.getCastingSpellId());
    }

    private static boolean handleResolvedInput(ServerPlayer player, @Nullable SpellSelectionManager.SelectionOption selection,
                                               ItemStack focusStaffbowStack) {
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return false;
        }
        if (!(focusStaffbowStack.getItem() instanceof FocusStaffbow)) {
            return false;
        }

        var spell = selection.spellData.getSpell();
        var spellLevel = spell.getLevelFor(selection.spellData.getLevel(), player);
        var castSource = selection.getCastSource();
        var castingSlot = selection.slot;
        var magicData = MagicData.getPlayerMagicData(player);

        if (magicData.isCasting() && !magicData.getCastingSpellId().equals(spell.getSpellId())) {
            stopFocusStaffbowOrVanillaCast(player, magicData);
        }
        if (magicData.isCasting()) {
            return false;
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return false;
        }
        if (denyIfLoanOutstanding(player, codexSpellData)) {
            return false;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (state.isActive()) {
            if (state.matches(spell, spellLevel, castSource, castingSlot)) {
                return true;
            }

            clearFocusStaffbowState(player, true, true);
        }

        if (spell.getCastType() == CastType.CONTINUOUS) {
            return beginContinuousCast(player, focusStaffbowStack, spell, spellLevel, castSource, castingSlot, codexSpellData);
        }

        return beginPendingCast(player, focusStaffbowStack, spell, spellLevel, castSource, castingSlot, codexSpellData);
    }

    private static boolean beginPendingCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell, int spellLevel,
                                            CastSource castSource, String castingSlot,
                                            jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData) {
        var originalEffectiveCastTicks = Math.max(spell.getEffectiveCastTime(spellLevel, player), 0);
        var requiredCastTicks = FocusStaffbowChargeLogic.normalizePendingRequiredCastTicks(originalEffectiveCastTicks);
        var chargeBaselineTicks = FocusStaffbowChargeLogic.normalizePendingChargeBaselineTicks(originalEffectiveCastTicks);

        var magicData = MagicData.getPlayerMagicData(player);
        if (!validateCastStart(player, spell, spellLevel, castSource, magicData)) {
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            return false;
        }

        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        // 通常の cast state を使うと auto-cast へ入るため、LONG/INSTANT 用は専用 state と HUD 同期だけで管理する。
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, state -> state.startPending(
                spell,
                spellLevel,
                castSource,
                castingSlot,
                player.level().getGameTime(),
                requiredCastTicks,
                chargeBaselineTicks,
                player.level().dimension().location().toString(),
                player.getInventory().selected
        ));
        syncPendingMagicDataSimulation(magicData, spell, spellLevel, castSource, requiredCastTicks, 0L, focusStaffbowStack);
        FocusStaffbowStartSoundContext.runSuppressed(player.getUUID(), () ->
                spell.onServerPreCast(player.level(), spellLevel, player, magicData)
        );
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::markPreCastStarted);
        Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(createCastStateData(
                spell,
                player.level().getGameTime(),
                requiredCastTicks,
                chargeBaselineTicks,
                spell.getManaCost(spellLevel),
                FocusStaffbowCastState.Mode.PENDING,
                1
        )));
        Networks.sendToTrackingEntityAndSelf(player, new SyncFocusStaffbowPresentationPacket(
                player.getUUID(),
                spell.getSpellId(),
                SyncFocusStaffbowPresentationPacket.PresentationAction.START_PENDING
        ));
        if (player.getMainHandItem().getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerChargeAnimation(player, player.getMainHandItem());
        }
        return true;
    }

    private static boolean beginContinuousCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell, int spellLevel,
                                               CastSource castSource, String castingSlot,
                                               jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData) {
        // CONTINUOUS は詠唱時間短縮 Attribute が逆効果になるため、
        // FocusStaffbow 側では spell 本来の castTime だけを標準詠唱可能時間として扱う。
        var standardCastTicks = Math.max(spell.getCastTime(spellLevel), 0);
        var requiredCastTicks = FocusStaffbowChargeLogic.normalizeContinuousRequiredCastTicks(standardCastTicks);

        var magicData = MagicData.getPlayerMagicData(player);
        if (!validateCastStart(player, spell, spellLevel, castSource, magicData)) {
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            return false;
        }

        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        magicData.initiateCast(spell, spellLevel, requiredCastTicks, castSource, castingSlot);
        magicData.setPlayerCastingItem(focusStaffbowStack);
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, state -> state.startContinuous(
                spell,
                spellLevel,
                castSource,
                castingSlot,
                player.level().getGameTime(),
                requiredCastTicks,
                player.level().dimension().location().toString(),
                player.getInventory().selected,
                FocusStaffbowChargeLogic.CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS
        ));
        syncContinuousMagicDataSimulation(magicData, spell, spellLevel, castSource, requiredCastTicks, 0L, focusStaffbowStack);
        updateContinuousChargeModifier(player, spell, spellLevel, codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE), 0L);
        FocusStaffbowStartSoundContext.runSuppressed(player.getUUID(), () ->
                spell.onServerPreCast(player.level(), spellLevel, player, magicData)
        );
        Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(createCastStateData(
                spell,
                player.level().getGameTime(),
                requiredCastTicks,
                requiredCastTicks,
                spell.getManaCost(spellLevel),
                FocusStaffbowCastState.Mode.CONTINUOUS,
                FocusStaffbowChargeLogic.CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS
        )));
        Networks.sendToTrackingEntityAndSelf(player, new SyncFocusStaffbowPresentationPacket(
                player.getUUID(),
                spell.getSpellId(),
                SyncFocusStaffbowPresentationPacket.PresentationAction.START_PENDING
        ));
        PacketDistributor.sendToPlayer(player, new UpdateCastingStatePacket(
                spell.getSpellId(),
                spellLevel,
                requiredCastTicks,
                castSource,
                castingSlot
        ));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new OnCastStartedPacket(player.getUUID(), spell.getSpellId(), spellLevel));
        if (player.getMainHandItem().getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerChargeAnimation(player, player.getMainHandItem());
        }
        return true;
    }

    private static boolean confirmPendingCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell,
                                              FocusStaffbowCastState state,
                                              jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData,
                                              long totalCastTicks, long currentGameTime) {
        var spellLevel = state.spellLevel;
        var castSource = resolveCastSource(state.castSource);
        var castingSlot = state.castingSlot;
        var finalMultiplier = FocusStaffbowChargeLogic.computePendingChargeMultiplier(totalCastTicks, state.chargeBaselineTicks);
        var shouldConsumeScaledMana = castSource.consumesMana() && !player.getAbilities().instabuild;
        var plannedManaCost = shouldConsumeScaledMana
                ? FocusStaffbowChargeLogic.computeScaledManaCost(spell.getManaCost(spellLevel), finalMultiplier)
                : 0;

        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
        Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(null));

        var magicData = MagicData.getPlayerMagicData(player);
        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute == null) {
            cleanupPendingSpellArtifacts(player, magicData.getAdditionalCastData());
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            cancelPendingPresentation(player, focusStaffbowStack, spell.getSpellId());
            return false;
        }

        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        var modifier = new AttributeModifier(
                OVERCHARGE_SPELL_POWER_MODIFIER_ID,
                OVERCHARGE_SPELL_POWER_MODIFIER_NAME,
                finalMultiplier - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        spellPowerAttribute.removeModifier(OVERCHARGE_SPELL_POWER_MODIFIER_ID);
        spellPowerAttribute.addTransientModifier(modifier);
        var borrowedMana = shouldConsumeScaledMana
                ? Math.max(0.0F, plannedManaCost - magicData.getMana())
                : 0.0F;
        var castCompleted = false;
        try {
            if (borrowedMana > 0.0F) {
                magicData.addMana(borrowedMana);
            }
            if (plannedManaCost > 0) {
                FocusStaffbowManaCostOverrideEvent.reserveManaCostOverride(player, plannedManaCost);
            }
            magicData.initiateCast(spell, spellLevel, 0, castSource, castingSlot);
            magicData.setPlayerCastingItem(focusStaffbowStack);
            syncPendingMagicDataSimulation(
                    magicData,
                    spell,
                    spellLevel,
                    castSource,
                    state.requiredCastTicks,
                    totalCastTicks,
                    focusStaffbowStack
            );
            PacketDistributor.sendToPlayer(player, new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, 0, castSource, castingSlot));
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new OnCastStartedPacket(player.getUUID(), spell.getSpellId(), spellLevel));
            spell.castSpell(player.level(), spellLevel, player, castSource, true);
            spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
            castCompleted = true;
            return true;
        } finally {
            FocusStaffbowManaCostOverrideEvent.clearManaCostOverride(player);
            if (borrowedMana > 0.0F) {
                if (castCompleted) {
                    codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, loanState -> loanState.addLoan(borrowedMana));
                } else {
                    magicData.setMana(Math.max(0.0F, magicData.getMana() - borrowedMana));
                    syncManaToClient(player, magicData);
                }
            }
            if (castCompleted && plannedManaCost > 0) {
                syncManaToClient(player, magicData);
            }
            removeOverchargeModifier(spellPowerAttribute);
        }
    }

    private static void tickContinuousCast(ServerPlayer player, FocusStaffbowCastState state, AbstractSpell spell) {
        if (shouldCancelFocusStaffbowState(player, state)) {
            stopContinuousCast(player, state, true, true, true);
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (!magicData.getSyncedData().isCasting() || !state.spellId.equals(magicData.getCastingSpellId())) {
            clearContinuousCastState(player, state, true);
            return;
        }

        var currentGameTime = player.level().getGameTime();
        var totalHeldTicks = state.getElapsedTicks(currentGameTime);
        var castSource = resolveCastSource(state.castSource);
        var currentChargeMultiplier = resolveContinuousChargeMultiplier(totalHeldTicks);
        var currentManaCost = resolveContinuousManaCost(player, spell, state, castSource, currentChargeMultiplier);

        if (castSource.consumesMana() && magicData.getMana() < currentManaCost) {
            stopContinuousCast(player, state, true, true, true);
            return;
        }

        syncContinuousMagicDataSimulation(
                magicData,
                spell,
                state.spellLevel,
                castSource,
                state.requiredCastTicks,
                totalHeldTicks,
                player.getMainHandItem()
        );
        updateContinuousChargeModifier(player, spell, state.spellLevel, state, totalHeldTicks);

        if (FocusStaffbowChargeLogic.shouldTriggerContinuousCast(totalHeldTicks, state.requiredCastTicks)) {
            if (castSource.consumesMana() && magicData.getMana() < currentManaCost) {
                stopContinuousCast(player, state, true, true, true);
                return;
            }

            try {
                if (currentManaCost > 0) {
                    FocusStaffbowManaCostOverrideEvent.reserveManaCostOverride(player, currentManaCost);
                }
                spell.castSpell(player.level(), state.spellLevel, player, castSource, false);
            } finally {
                FocusStaffbowManaCostOverrideEvent.clearManaCostOverride(player);
            }
            syncContinuousMagicDataSimulation(
                    magicData,
                    spell,
                    state.spellLevel,
                    castSource,
                    state.requiredCastTicks,
                    totalHeldTicks,
                    player.getMainHandItem()
            );
        }

        spell.onServerCastTick(player.level(), state.spellLevel, player, magicData);
        if (!magicData.getSyncedData().isCasting()) {
            clearContinuousCastState(player, state, true);
        }
    }

    private static void stopContinuousCast(ServerPlayer player, FocusStaffbowCastState state, boolean triggerCooldown,
                                           boolean callServerCastComplete, boolean syncClientCancel) {
        var magicData = MagicData.getPlayerMagicData(player);
        removeOverchargeModifier(player.getAttribute(AttributeRegistry.SPELL_POWER.get()));

        if (callServerCastComplete && magicData.getSyncedData().isCasting() && state.spellId.equals(magicData.getCastingSpellId())) {
            var spell = SpellRegistry.getSpell(state.spellId);
            if (spell != null && spell != SpellRegistry.none()) {
                if (triggerCooldown) {
                    MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, resolveCastSource(state.castSource));
                }
                spell.onServerCastComplete(player.level(), state.spellLevel, player, magicData, true);
            } else {
                magicData.resetCastingState();
            }
        } else {
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            magicData.resetCastingState();
        }

        clearContinuousCastState(player, state, syncClientCancel);
    }

    private static void clearFocusStaffbowState(ServerPlayer player, boolean triggerCooldown, boolean syncClientCancel) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isActive()) {
            return;
        }

        if (state.isContinuous()) {
            stopContinuousCast(player, state, triggerCooldown, true, syncClientCancel);
            return;
        }

        clearPendingCastState(player, state, syncClientCancel);
    }

    private static void clearPendingCastState(ServerPlayer player, FocusStaffbowCastState state, boolean syncClientCancel) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var mainHandStack = player.getMainHandItem();
        var magicData = MagicData.getPlayerMagicData(player);
        cleanupPendingSpellArtifacts(player, magicData.getAdditionalCastData());
        magicData.resetAdditionalCastData();
        clearPendingMagicDataSimulation(magicData);
        spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
        if (syncClientCancel) {
            Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(null));
            Networks.sendToTrackingEntityAndSelf(player, new SyncFocusStaffbowPresentationPacket(
                    player.getUUID(),
                    state.spellId,
                    SyncFocusStaffbowPresentationPacket.PresentationAction.CANCEL_PENDING
            ));
        }

        if (mainHandStack.getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerIdleAnimation(player, mainHandStack);
        }
    }

    private static void clearContinuousCastState(ServerPlayer player, FocusStaffbowCastState state, boolean syncClientCancel) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
        if (syncClientCancel) {
            Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(null));
            Networks.sendToTrackingEntityAndSelf(player, new SyncFocusStaffbowPresentationPacket(
                    player.getUUID(),
                    state.spellId,
                    SyncFocusStaffbowPresentationPacket.PresentationAction.CANCEL_PENDING
            ));
        }

        if (player.getMainHandItem().getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerIdleAnimation(player, player.getMainHandItem());
        }
    }

    private static boolean shouldCancelFocusStaffbowState(ServerPlayer player, FocusStaffbowCastState state) {
        if (!(player.getMainHandItem().getItem() instanceof FocusStaffbow)) {
            return true;
        }
        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return true;
        }
        if (player.getInventory().selected != state.selectedHotbarSlot) {
            return true;
        }
        if (!player.level().dimension().location().toString().equals(state.dimensionId)) {
            return true;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            return true;
        }
        if (player.containerMenu != player.inventoryMenu) {
            return true;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        return state.isPending()
                ? magicData.isCasting()
                : !state.spellId.equals(magicData.getCastingSpellId()) && magicData.getSyncedData().isCasting();
    }

    private static boolean denyIfLoanOutstanding(ServerPlayer player,
                                                 jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData) {
        var loanState = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
        if (!loanState.hasOutstandingLoan()) {
            return false;
        }

        player.connection.send(new ClientboundSetActionBarTextPacket(
                FocusStaffbow.createLoanBlockedMessage(loanState.remainingLoanMana)
        ));
        return true;
    }

    private static boolean validateCastStart(ServerPlayer player, AbstractSpell spell, int spellLevel,
                                             CastSource castSource, MagicData magicData) {
        var castResult = spell.canBeCastedBy(spellLevel, castSource, magicData, player);
        if (castResult.message != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        return castResult.isSuccess()
                && spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)
                && !MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(player, spell.getSpellId(), spellLevel, spell.getSchoolType(), castSource));
    }

    private static void stopFocusStaffbowOrVanillaCast(ServerPlayer player, MagicData magicData) {
        if (magicData.getPlayerCastingItem().getItem() instanceof FocusStaffbow) {
            clearFocusStaffbowState(player, magicData.getCastType() == CastType.CONTINUOUS, true);
            return;
        }

        var spell = magicData.getCastingSpell().getSpell();
        if (spell != null && spell != SpellRegistry.none()) {
            if (magicData.getCastType() != CastType.LONG) {
                MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, magicData.getCastSource());
            }
            if (magicData.getCastSource() == CastSource.SCROLL && magicData.getCastType() == CastType.CONTINUOUS) {
                Scroll.attemptRemoveScrollAfterCast(player);
            }
            spell.onServerCastComplete(player.level(), magicData.getCastingSpellLevel(), player, magicData, true);
        }
    }

    private static void updateContinuousChargeModifier(ServerPlayer player, AbstractSpell spell, int spellLevel,
                                                       FocusStaffbowCastState state, long totalHeldTicks) {
        var sampledTicks = Math.max(0L, totalHeldTicks);
        if (state.lastChargeSampledTicks == sampledTicks) {
            return;
        }

        var finalMultiplier = FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(sampledTicks);
        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute != null) {
            spellPowerAttribute.removeModifier(OVERCHARGE_SPELL_POWER_MODIFIER_ID);
            if (finalMultiplier > 1.0D) {
                spellPowerAttribute.addTransientModifier(new AttributeModifier(
                        OVERCHARGE_SPELL_POWER_MODIFIER_ID,
                        OVERCHARGE_SPELL_POWER_MODIFIER_NAME,
                        finalMultiplier - 1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData != null) {
            codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, currentState -> {
                if (currentState.isContinuous() && currentState.matches(spell, spellLevel, resolveCastSource(state.castSource), state.castingSlot)) {
                    currentState.lastChargeSampledTicks = sampledTicks;
                }
            });
        }
    }

    private static double resolveContinuousChargeMultiplier(long totalHeldTicks) {
        return FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(totalHeldTicks);
    }

    private static int resolveContinuousManaCost(ServerPlayer player, AbstractSpell spell, FocusStaffbowCastState state,
                                                 CastSource castSource, double chargeMultiplier) {
        if (!castSource.consumesMana() || player.getAbilities().instabuild) {
            return 0;
        }

        return FocusStaffbowChargeLogic.computeScaledManaCost(spell.getManaCost(state.spellLevel), chargeMultiplier);
    }

    private static CastSource resolveCastSource(String castSourceName) {
        try {
            return CastSource.valueOf(castSourceName);
        } catch (IllegalArgumentException ignored) {
            return CastSource.NONE;
        }
    }

    private static CompoundTag createCastStateData(AbstractSpell spell, long startedGameTime, int requiredCastTicks,
                                                   int chargeBaselineTicks, int baseManaCost,
                                                   FocusStaffbowCastState.Mode mode, int chargeUpdateIntervalTicks) {
        var data = new CompoundTag();
        data.putString("castMode", mode.name().toLowerCase(java.util.Locale.ROOT));
        data.putString("spellId", spell.getSpellId());
        data.putLong("startedGameTime", startedGameTime);
        data.putInt("requiredCastTicks", requiredCastTicks);
        data.putInt("chargeBaselineTicks", Math.max(0, chargeBaselineTicks));
        data.putInt("baseManaCost", Math.max(0, baseManaCost));
        data.putInt("chargeUpdateIntervalTicks", Math.max(1, chargeUpdateIntervalTicks));
        return data;
    }

    private static @Nullable SpellSelectionManager.SelectionOption resolveSelection(ServerPlayer player) {
        return new SpellSelectionManager(player).getSelection();
    }

    private static void cancelPendingPresentation(ServerPlayer player, ItemStack focusStaffbowStack, String spellId) {
        Networks.sendToTrackingEntityAndSelf(player, new SyncFocusStaffbowPresentationPacket(
                player.getUUID(),
                spellId,
                SyncFocusStaffbowPresentationPacket.PresentationAction.CANCEL_PENDING
        ));
        if (focusStaffbowStack.getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerIdleAnimation(player, focusStaffbowStack);
        }
    }

    private static void removeOverchargeModifier(@Nullable AttributeInstance spellPowerAttribute) {
        if (spellPowerAttribute != null) {
            spellPowerAttribute.removeModifier(OVERCHARGE_SPELL_POWER_MODIFIER_ID);
        }
    }

    private static void syncPendingMagicDataSimulation(MagicData magicData, AbstractSpell spell, int spellLevel,
                                                       CastSource castSource, int requiredCastTicks, long elapsedTicks,
                                                       ItemStack castingItem) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(spellLevel);
        accessor.apprenticecodex$setCastDuration(requiredCastTicks);
        accessor.apprenticecodex$setCastDurationRemaining(Math.max(0, requiredCastTicks - (int) Math.max(0L, elapsedTicks)));
        accessor.apprenticecodex$setCastSource(castSource);
        accessor.apprenticecodex$setCastType(spell.getCastType());
        magicData.setPlayerCastingItem(castingItem);
    }

    private static void syncContinuousMagicDataSimulation(MagicData magicData, AbstractSpell spell, int spellLevel,
                                                          CastSource castSource, int requiredCastTicks, long elapsedTicks,
                                                          ItemStack castingItem) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(spellLevel);
        accessor.apprenticecodex$setCastDuration(requiredCastTicks);
        accessor.apprenticecodex$setCastDurationRemaining(
                FocusStaffbowChargeLogic.computeContinuousCastDurationRemaining(elapsedTicks, requiredCastTicks)
        );
        accessor.apprenticecodex$setCastSource(castSource);
        accessor.apprenticecodex$setCastType(spell.getCastType());
        magicData.setPlayerCastingItem(castingItem);
    }

    private static void clearPendingMagicDataSimulation(MagicData magicData) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(0);
        accessor.apprenticecodex$setCastDuration(0);
        accessor.apprenticecodex$setCastDurationRemaining(0);
        accessor.apprenticecodex$setCastSource(CastSource.NONE);
        accessor.apprenticecodex$setCastType(CastType.NONE);
        magicData.setPlayerCastingItem(ItemStack.EMPTY);
    }

    private static void cleanupPendingSpellArtifacts(ServerPlayer player, @Nullable ICastData castData) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (!(castData instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData summonCastData)) {
            return;
        }

        // pending 中断では onServerCastComplete を呼べないため、pre-cast で出した summon だけはここで明示的に片付ける。
        var summonEntity = summonCastData.getEntity(serverLevel);
        if (summonEntity instanceof SummonWeaponEntity summonWeaponEntity) {
            summonWeaponEntity.releaseWeapon();
        }
    }

    private static void syncManaToClient(ServerPlayer player, MagicData magicData) {
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
