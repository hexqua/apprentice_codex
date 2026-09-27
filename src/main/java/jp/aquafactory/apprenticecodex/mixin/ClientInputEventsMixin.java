package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.network.casting.CastPacket;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.event.client.ClientBlockTargetSyncService;
import jp.aquafactory.apprenticecodex.event.client.QuickcastCartridgeClientEvents;
import jp.aquafactory.apprenticecodex.event.client.QuickcastCartridgeClientState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceClientController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientInputEvents.class, remap = false)
public abstract class ClientInputEventsMixin {

    // ISS 3.15.4 では入力処理が handleInputEvent から handleKeybinds へ移動したため、
    // キー入力由来の送信点だけを差し替えて対象ブロック指定処理を維持する.
    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
                    ordinal = 0
            )
    )
    private static void redirectCastPacket(CustomPacketPayload packet, CustomPacketPayload[] extraPackets) {
        if (packet instanceof CastPacket) {
            if (apprentice_codex$shouldBlockFocusStaffbowShortcut()
                    || apprentice_codex$shouldBlockMirageAvoidanceEffectInput()) {
                return;
            }
            if (QuickcastCartridgeClientEvents.sendSelectedCast(-1)) {
                return;
            }
            QuickcastCartridgeClientState.interrupt();
            if (apprentice_codex$trySendSelectedSpellCast()) {
                return;
            }
        }

        PacketDistributor.sendToServer(packet, extraPackets);
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
                    ordinal = 1
            )
    )
    private static void redirectQuickCastPacket(CustomPacketPayload packet, CustomPacketPayload[] extraPackets) {
        if (packet instanceof QuickCastPacket quickCastPacket) {
            if (apprentice_codex$shouldBlockFocusStaffbowShortcut()
                    || apprentice_codex$shouldBlockMirageAvoidanceEffectInput()) {
                return;
            }
            if (QuickcastCartridgeClientEvents.sendSelectedCast(
                    ((QuickCastPacketAccessor) quickCastPacket).apprenticecodex$getSlot())) {
                return;
            }
            QuickcastCartridgeClientState.interrupt();
            if (apprentice_codex$trySendTargetedQuickCast(quickCastPacket)) {
                return;
            }
        }

        PacketDistributor.sendToServer(packet, extraPackets);
    }

    @Unique
    private static boolean apprentice_codex$trySendSelectedSpellCast() {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var spellData = selectionManager.getSelectedSpellData();
        return ClientBlockTargetSyncService.trySendForSelectedCast(spellData, -1);
    }

    @Unique
    private static boolean apprentice_codex$trySendTargetedQuickCast(QuickCastPacket quickCastPacket) {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return false;
        }

        var quickCastSlot = ((QuickCastPacketAccessor) quickCastPacket).apprenticecodex$getSlot();
        var spellData = selectionManager.getSpellData(quickCastSlot);
        return ClientBlockTargetSyncService.trySendForSelectedCast(spellData, quickCastSlot);
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
}
