package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.casting.CastPacket;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.event.client.ClientPlacementPreviewManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMirageAvoidanceCastPacket;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidance;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceClientController;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.ClientBlockTargetingHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientInputEvents.class, remap = false)
public abstract class ClientInputEventsMixin {

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/setup/PacketDistributor;sendToServer(Ljava/lang/Object;)V",
                    ordinal = 0
            )
    )
    private static void redirectCastPacket(Object message) {
        if (message instanceof CastPacket) {
            if (apprentice_codex$shouldBlockFocusStaffbowShortcut()
                    || apprentice_codex$shouldBlockMirageAvoidanceEffectInput()) {
                return;
            }
            apprentice_codex$rememberMirageAvoidanceDirection();
            if (apprentice_codex$trySendSelectedMirageAvoidanceCast()
                    || apprentice_codex$trySendSelectedSpellCast()) {
                return;
            }
        }

        PacketDistributor.sendToServer(message);
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/setup/PacketDistributor;sendToServer(Ljava/lang/Object;)V",
                    ordinal = 1
            )
    )
    private static void redirectQuickCastPacket(Object message) {
        if (message instanceof QuickCastPacket quickCastPacket) {
            if (apprentice_codex$shouldBlockFocusStaffbowShortcut()
                    || apprentice_codex$shouldBlockMirageAvoidanceEffectInput()) {
                return;
            }
            apprentice_codex$rememberMirageAvoidanceDirection();
            if (apprentice_codex$trySendMirageAvoidanceQuickCast(quickCastPacket)
                    || apprentice_codex$trySendTargetedQuickCast(quickCastPacket)) {
                return;
            }
        }

        PacketDistributor.sendToServer(message);
    }

    @Unique
    private static boolean apprentice_codex$trySendSelectedSpellCast() {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var spellData = selectionManager.getSelectedSpellData();
        return apprentice_codex$trySendTargetedCastPacket(spellData, -1);
    }

    @Unique
    private static boolean apprentice_codex$trySendTargetedQuickCast(QuickCastPacket quickCastPacket) {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var quickCastSlot = ((QuickCastPacketAccessor) quickCastPacket).apprenticecodex$getSlot();
        var spellData = selectionManager.getSpellData(quickCastSlot);
        return apprentice_codex$trySendTargetedCastPacket(spellData, quickCastSlot);
    }

    @Unique
    private static boolean apprentice_codex$shouldBlockFocusStaffbowShortcut() {
        var player = Minecraft.getInstance().player;
        return player != null && FocusStaffbowClientCastState.hasPendingCast(player);
    }

    @Unique
    private static boolean apprentice_codex$shouldBlockMirageAvoidanceEffectInput() {
        if (!MirageAvoidanceClientController.isActive()) {
            return false;
        }

        MirageAvoidanceClientController.showDuringEffectMessage();
        return true;
    }

    @Unique
    private static boolean apprentice_codex$trySendSelectedMirageAvoidanceCast() {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var spellData = selectionManager.getSelectedSpellData();
        return apprentice_codex$trySendMirageAvoidanceCastPacket(spellData, -1);
    }

    @Unique
    private static boolean apprentice_codex$trySendMirageAvoidanceQuickCast(QuickCastPacket quickCastPacket) {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var quickCastSlot = ((QuickCastPacketAccessor) quickCastPacket).apprenticecodex$getSlot();
        var spellData = selectionManager.getSpellData(quickCastSlot);
        return apprentice_codex$trySendMirageAvoidanceCastPacket(spellData, quickCastSlot);
    }

    @Unique
    private static boolean apprentice_codex$trySendMirageAvoidanceCastPacket(SpellData spellData, int quickCastSlot) {
        if (spellData == SpellData.EMPTY || !(spellData.getSpell() instanceof MirageAvoidance)) {
            return false;
        }

        var input = MirageAvoidanceClientController.captureCurrentInput();
        Networks.sendToServer(new ClientMirageAvoidanceCastPacket(quickCastSlot, input.forward(), input.strafe()));
        return true;
    }

    @Unique
    private static void apprentice_codex$rememberMirageAvoidanceDirection() {
        var input = MirageAvoidanceClientController.captureCurrentInput();
        Networks.sendToServer(ClientMirageAvoidanceCastPacket.rememberInput(input.forward(), input.strafe()));
    }

    @Unique
    private static boolean apprentice_codex$trySendTargetedCastPacket(SpellData spellData, int quickCastSlot) {
        if (spellData == SpellData.EMPTY) {
            return false;
        }

        var spell = spellData.getSpell();
        if (!(spell instanceof IClientBlockTargetingSpell targetingSpell)) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var targetData = apprentice_codex$captureTargetData(spellData, player, spellLevel);
        ClientPlacementPreviewManager.rememberPendingTarget(spell.getSpellResource(), targetData);
        Networks.sendToServer(new ClientBlockTargetCastPacket(quickCastSlot, spell.getSpellResource(), targetData));
        return true;
    }

    @Unique
    private static BlockTargetData apprentice_codex$captureTargetData(SpellData spellData, net.minecraft.world.entity.player.Player player,
                                                                      int spellLevel) {
        var spell = spellData.getSpell();
        if (spell instanceof IClientBlockTargetCaptureSpell customCaptureSpell) {
            return customCaptureSpell.captureClientBlockTarget(player, spellLevel);
        }
        if (spell instanceof IClientBlockTargetingSpell targetingSpell) {
            return ClientBlockTargetingHelper.captureOutlinedTarget(
                    player,
                    targetingSpell.getClientBlockTargetingRange(spellLevel, player)
            );
        }
        return new BlockTargetData();
    }
}
