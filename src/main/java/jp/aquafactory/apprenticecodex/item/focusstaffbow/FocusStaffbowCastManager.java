package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastFinishedPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowCastStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowPresentationPacket;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
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
        if (!state.isActive()) {
            return;
        }

        var spell = SpellRegistry.getSpell(state.spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            resetPendingState(serverPlayer, true);
            return;
        }

        var currentGameTime = serverPlayer.level().getGameTime();
        var totalCastTicks = Math.max(state.getElapsedTicks(currentGameTime), Math.max(0, drawDuration));
        if (totalCastTicks < state.requiredCastTicks) {
            ApprenticeCodex.LOGGER.info(
                    "FocusStaffbow charge cancelled: player={}, spell={}, startedGameTime={}, currentGameTime={}, totalCastTicks={}, requiredCastTicks={}",
                    serverPlayer.getGameProfile().getName(),
                    spell.getSpellId(),
                    state.startedGameTime,
                    currentGameTime,
                    totalCastTicks,
                    state.requiredCastTicks
            );
            resetPendingState(serverPlayer, true);
            return;
        }

        confirmCast(serverPlayer, stack, spell, state, codexSpellData, totalCastTicks, currentGameTime);
    }

    public static void tick(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isActive()) {
            return;
        }

        var spell = SpellRegistry.getSpell(state.spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            resetPendingState(player, true);
            return;
        }

        if (shouldCancelPendingState(player, state)) {
            resetPendingState(player, true);
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
        resetPendingState(player, true);
    }

    public static void resetPendingState(ServerPlayer player, boolean syncClientCancel) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isActive()) {
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
            CancelCastPacket.cancelCast(player, magicData.getCastType() != CastType.LONG);
        }
        if (magicData.isCasting()) {
            return false;
        }

        if (spell.getCastType() == CastType.CONTINUOUS) {
            resetPendingState(player, true);
            return spell.attemptInitiateCast(focusStaffbowStack, spellLevel, player.level(), player, castSource, true, castingSlot);
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return false;
        }

        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (state.isActive()) {
            if (state.matches(spell, spellLevel, castSource, castingSlot)) {
                return true;
            }

            resetPendingState(player, true);
        }

        return beginPendingCast(player, focusStaffbowStack, spell, spellLevel, castSource, castingSlot, codexSpellData);
    }

    private static boolean beginPendingCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell, int spellLevel,
                                            CastSource castSource, String castingSlot,
                                            jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData) {
        var originalEffectiveCastTicks = Math.max(spell.getEffectiveCastTime(spellLevel, player), 0);
        var requiredCastTicks = FocusStaffbowChargeLogic.normalizeRequiredCastTicks(originalEffectiveCastTicks);
        var schoolType = spell.getSchoolType();
        var globalSpellPowerAttribute = player.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
        var schoolSpellPowerAttribute = schoolType == null ? 1.0D : schoolType.getPowerFor(player);
        var schoolFinalSpellPowerAttribute = globalSpellPowerAttribute * schoolSpellPowerAttribute;

        ApprenticeCodex.LOGGER.info(
                "FocusStaffbow charge setup: player={}, spell={}, school={}, castType={}, originalEffectiveCastTicks={}, normalizedRequiredCastTicks={}, globalSpellPowerAttribute={}, schoolSpellPowerAttribute={}, schoolFinalSpellPowerAttribute={}",
                player.getGameProfile().getName(),
                spell.getSpellId(),
                schoolType == null ? "none" : schoolType.getId(),
                spell.getCastType(),
                originalEffectiveCastTicks,
                requiredCastTicks,
                globalSpellPowerAttribute,
                schoolSpellPowerAttribute,
                schoolFinalSpellPowerAttribute
        );

        var magicData = MagicData.getPlayerMagicData(player);
        var castResult = spell.canBeCastedBy(spellLevel, castSource, magicData, player);
        if (castResult.message != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        if (!castResult.isSuccess()
                || !spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)
                || MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(player, spell.getSpellId(), spellLevel, spell.getSchoolType(), castSource))) {
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            return false;
        }

        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        // 通常の cast state を使うと auto-cast へ入るため、special charge は専用 state と HUD 同期だけで管理する。
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, state -> state.start(
                spell,
                spellLevel,
                castSource,
                castingSlot,
                player.level().getGameTime(),
                requiredCastTicks,
                player.level().dimension().location().toString(),
                player.getInventory().selected
        ));
        syncPendingMagicDataSimulation(magicData, spell, spellLevel, castSource, requiredCastTicks, 0L, focusStaffbowStack);
        FocusStaffbowStartSoundContext.runSuppressed(player.getUUID(), () ->
                spell.onServerPreCast(player.level(), spellLevel, player, magicData)
        );
        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::markPreCastStarted);
        Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(createPendingStateData(
                spell,
                player.level().getGameTime(),
                requiredCastTicks
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

    private static boolean confirmCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell,
                                       FocusStaffbowCastState state,
                                       jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData,
                                       long totalCastTicks, long currentGameTime) {
        var spellLevel = state.spellLevel;
        var castSource = resolveCastSource(state.castSource);
        var castingSlot = state.castingSlot;
        var configuredMaxMultiplier = ApprenticeCodexServerConfig.focusStaffbowMaxChargeMultiplier();
        var rawMultiplier = FocusStaffbowChargeLogic.computeRawChargeMultiplier(totalCastTicks, state.requiredCastTicks);
        var finalMultiplier = FocusStaffbowChargeLogic.clampChargeMultiplier(rawMultiplier, configuredMaxMultiplier);
        var schoolType = spell.getSchoolType();
        var globalSpellPowerAttribute = player.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
        var schoolSpellPowerAttribute = schoolType == null ? 1.0D : schoolType.getPowerFor(player);
        var schoolFinalSpellPowerAttribute = globalSpellPowerAttribute * schoolSpellPowerAttribute;
        var chargedSchoolFinalSpellPowerAttribute = schoolFinalSpellPowerAttribute * finalMultiplier;

        ApprenticeCodex.LOGGER.info(
                "FocusStaffbow charge confirm: player={}, spell={}, school={}, startedGameTime={}, currentGameTime={}, totalCastTicks={}, requiredCastTicks={}, formula='clamp(totalCastTicks / requiredCastTicks, 1.0, maxChargeMultiplier)', rawMultiplier={}, configuredMaxMultiplier={}, appliedMultiplier={}",
                player.getGameProfile().getName(),
                spell.getSpellId(),
                schoolType == null ? "none" : schoolType.getId(),
                state.startedGameTime,
                currentGameTime,
                totalCastTicks,
                state.requiredCastTicks,
                rawMultiplier,
                configuredMaxMultiplier,
                finalMultiplier
        );
        ApprenticeCodex.LOGGER.info(
                "FocusStaffbow charge power: player={}, spell={}, school={}, globalSpellPowerAttribute={}, schoolSpellPowerAttribute={}, schoolFinalSpellPowerAttribute={}, chargedSchoolFinalSpellPowerAttribute={}",
                player.getGameProfile().getName(),
                spell.getSpellId(),
                schoolType == null ? "none" : schoolType.getId(),
                globalSpellPowerAttribute,
                schoolSpellPowerAttribute,
                schoolFinalSpellPowerAttribute,
                chargedSchoolFinalSpellPowerAttribute
        );

        codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
        Networks.sendToPlayer(player, new SyncFocusStaffbowCastStatePacket(null));

        var magicData = MagicData.getPlayerMagicData(player);
        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute == null) {
            cleanupPendingSpellArtifacts(player, magicData.getAdditionalCastData());
            magicData.resetAdditionalCastData();
            clearPendingMagicDataSimulation(magicData);
            cancelPendingPresentation(player, focusStaffbowStack, spell.getSpellId());
            PacketDistributor.sendToPlayer(player, new OnCastFinishedPacket(player.getUUID(), spell.getSpellId(), true));
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
        try {
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
            return true;
        } finally {
            removeOverchargeModifier(spellPowerAttribute);
        }
    }

    private static boolean shouldCancelPendingState(ServerPlayer player, FocusStaffbowCastState state) {
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
        return magicData.isCasting();
    }

    private static CastSource resolveCastSource(String castSourceName) {
        try {
            return CastSource.valueOf(castSourceName);
        } catch (IllegalArgumentException ignored) {
            return CastSource.NONE;
        }
    }

    private static CompoundTag createPendingStateData(AbstractSpell spell, long startedGameTime, int requiredCastTicks) {
        var data = new CompoundTag();
        data.putString("spellId", spell.getSpellId());
        data.putLong("startedGameTime", startedGameTime);
        data.putInt("requiredCastTicks", requiredCastTicks);
        data.putDouble("maxChargeMultiplier", ApprenticeCodexServerConfig.focusStaffbowMaxChargeMultiplier());
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
}
