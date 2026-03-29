package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.casting.CastPacket;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
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
    private static void redirectCastPacket(Object packet) {
        if (packet instanceof CastPacket && apprentice_codex$trySendSelectedSpellCast()) {
            return;
        }

        PacketDistributor.sendToServer(packet);
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/setup/PacketDistributor;sendToServer(Ljava/lang/Object;)V",
                    ordinal = 1
            )
    )
    private static void redirectQuickCastPacket(Object packet) {
        if (packet instanceof QuickCastPacket quickCastPacket && apprentice_codex$trySendTargetedQuickCast(quickCastPacket)) {
            return;
        }

        PacketDistributor.sendToServer(packet);
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
        var targetData = spell instanceof IClientBlockTargetCaptureSpell customCaptureSpell
                ? customCaptureSpell.captureClientBlockTarget(player, spellLevel)
                : ClientBlockTargetingHelper.captureOutlinedTarget(
                        player,
                        targetingSpell.getClientBlockTargetingRange(spellLevel, player)
                );
        Networks.sendToServer(new ClientBlockTargetCastPacket(quickCastSlot, spell.getSpellResource(), targetData));
        return true;
    }
}
