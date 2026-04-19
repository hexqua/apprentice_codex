package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
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
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FocusStaffbowCastManager {
    private static final int MINIMUM_SPECIAL_CAST_TICKS = 40;
    private static final UUID OVERCHARGE_SPELL_POWER_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");
    private static final String OVERCHARGE_SPELL_POWER_MODIFIER_NAME = "apprenticecodex.focus_staffbow.overcharge";

    private FocusStaffbowCastManager() {
    }

    public static boolean handleSelectedSpellInput(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return handleResolvedInput(serverPlayer, resolveSelection(serverPlayer, -1), stack, null);
    }

    public static void handleClientPacketInput(ServerPlayer player, int quickCastSlot, ResourceLocation spellId, BlockTargetData targetData) {
        var selection = resolveSelection(player, quickCastSlot);
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return;
        }
        if (!selection.spellData.getSpell().getSpellResource().equals(spellId)) {
            return;
        }

        handleResolvedInput(player, selection, player.getMainHandItem(), targetData);
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

        if (shouldCancelPendingState(player, state)) {
            resetPendingState(player, true);
        }
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

        var spellId = state.spellId;
        spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
        if (syncClientCancel) {
            PacketDistributor.sendToPlayer(player, new OnCastFinishedPacket(player.getUUID(), spellId, true));
        }
    }

    private static boolean handleResolvedInput(ServerPlayer player, @Nullable SpellSelectionManager.SelectionOption selection,
                                               ItemStack focusStaffbowStack, @Nullable BlockTargetData targetData) {
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
            return withPendingTarget(player, spell, targetData, () ->
                    spell.attemptInitiateCast(focusStaffbowStack, spellLevel, player.level(), player, castSource, true, castingSlot)
            );
        }

        var codexSpellData = Capabilities.getSpellDataOrNull(player);
        if (codexSpellData == null) {
            return false;
        }
        var state = codexSpellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (state.isActive()) {
            if (state.matches(spell, spellLevel, castSource, castingSlot)) {
                if (!state.isReady(player.level().getGameTime())) {
                    return true;
                }

                return withPendingTarget(player, spell, targetData, () ->
                        confirmCast(player, focusStaffbowStack, spell, spellLevel, castSource, castingSlot, codexSpellData, state)
                );
            }

            var previousSpellId = state.spellId;
            codexSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
            PacketDistributor.sendToPlayer(player, new OnCastFinishedPacket(player.getUUID(), previousSpellId, true));
        }

        beginPendingCast(player, spell, spellLevel, castSource, castingSlot, codexSpellData);
        return true;
    }

    private static void beginPendingCast(ServerPlayer player, AbstractSpell spell, int spellLevel,
                                         io.redspace.ironsspellbooks.api.spells.CastSource castSource, String castingSlot,
                                         jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData) {
        var originalEffectiveCastTicks = Math.max(spell.getEffectiveCastTime(spellLevel, player), 0);
        var requiredCastTicks = Math.max(MINIMUM_SPECIAL_CAST_TICKS, originalEffectiveCastTicks);
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

        // 1回目入力では通常の cast state を使わず、疑似的に HUD だけ詠唱状態へ入れて auto-cast を防ぐ。
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
        PacketDistributor.sendToPlayer(player, new UpdateCastingStatePacket(
                spell.getSpellId(),
                spellLevel,
                requiredCastTicks,
                castSource,
                castingSlot
        ));
    }

    private static boolean confirmCast(ServerPlayer player, ItemStack focusStaffbowStack, AbstractSpell spell, int spellLevel,
                                       io.redspace.ironsspellbooks.api.spells.CastSource castSource, String castingSlot,
                                       jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData codexSpellData,
                                       jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState state) {
        var currentGameTime = player.level().getGameTime();
        var elapsedTicks = state.getElapsedTicks(currentGameTime);
        var overchargeTicks = state.getOverchargeTicks(currentGameTime);
        var rawMultiplier = 0.5D + overchargeTicks / (double) state.requiredCastTicks;
        var finalMultiplier = Math.max(1.0D, rawMultiplier);
        var schoolType = spell.getSchoolType();
        var globalSpellPowerAttribute = player.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
        var schoolSpellPowerAttribute = schoolType == null ? 1.0D : schoolType.getPowerFor(player);
        var schoolFinalSpellPowerAttribute = globalSpellPowerAttribute * schoolSpellPowerAttribute;
        var chargedSchoolFinalSpellPowerAttribute = schoolFinalSpellPowerAttribute * finalMultiplier;

        ApprenticeCodex.LOGGER.info(
                "FocusStaffbow charge confirm: player={}, spell={}, school={}, startedGameTime={}, currentGameTime={}, elapsedTicks={}, requiredCastTicks={}, overchargeTicks={}, formula='max(1.0, 0.5 + overchargeTicks / requiredCastTicks)', rawMultiplier={}, appliedMultiplier={}",
                player.getGameProfile().getName(),
                spell.getSpellId(),
                schoolType == null ? "none" : schoolType.getId(),
                state.startedGameTime,
                currentGameTime,
                elapsedTicks,
                state.requiredCastTicks,
                overchargeTicks,
                rawMultiplier,
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

        var magicData = MagicData.getPlayerMagicData(player);
        var castResult = spell.canBeCastedBy(spellLevel, castSource, magicData, player);
        if (castResult.message != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        magicData.resetAdditionalCastData();
        if (!castResult.isSuccess()
                || !spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)
                || MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(player, spell.getSpellId(), spellLevel, spell.getSchoolType(), castSource))) {
            PacketDistributor.sendToPlayer(player, new OnCastFinishedPacket(player.getUUID(), spell.getSpellId(), true));
            return false;
        }

        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute == null) {
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
            spell.onServerPreCast(player.level(), spellLevel, player, magicData);
            PacketDistributor.sendToPlayer(player, new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, 0, castSource, castingSlot));
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new OnCastStartedPacket(player.getUUID(), spell.getSpellId(), spellLevel));
            spell.onServerCastTick(player.level(), spellLevel, player, magicData);
            spell.castSpell(player.level(), spellLevel, player, castSource, true);
            spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
            return true;
        } finally {
            removeOverchargeModifier(spellPowerAttribute);
        }
    }

    private static boolean shouldCancelPendingState(ServerPlayer player,
                                                    jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState state) {
        if (!(player.getMainHandItem().getItem() instanceof FocusStaffbow)) {
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

    private static @Nullable SpellSelectionManager.SelectionOption resolveSelection(ServerPlayer player, int quickCastSlot) {
        var selectionManager = new SpellSelectionManager(player);
        return quickCastSlot >= 0 ? selectionManager.getSpellSlot(quickCastSlot) : selectionManager.getSelection();
    }

    private static boolean withPendingTarget(ServerPlayer player, AbstractSpell spell, @Nullable BlockTargetData targetData,
                                             java.util.function.BooleanSupplier action) {
        if (targetData == null || !targetData.hasTarget()) {
            return action.getAsBoolean();
        }

        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);
        try {
            return action.getAsBoolean();
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
        }
    }

    private static void removeOverchargeModifier(@Nullable AttributeInstance spellPowerAttribute) {
        if (spellPowerAttribute != null) {
            spellPowerAttribute.removeModifier(OVERCHARGE_SPELL_POWER_MODIFIER_ID);
        }
    }
}
