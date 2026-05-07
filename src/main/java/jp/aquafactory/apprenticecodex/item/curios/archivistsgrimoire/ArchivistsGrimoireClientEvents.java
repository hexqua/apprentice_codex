package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientChangeArchivistsGrimoireRowPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ArchivistsGrimoireClientEvents {
    private ArchivistsGrimoireClientEvents() {
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (!SpellWheelOverlay.instance.active || event.getScrollDeltaY() == 0.0D) {
            return;
        }

        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        if (spellbookStack == null || !(spellbookStack.getItem() instanceof ArchivistsGrimoire)) {
            return;
        }

        var delta = event.getScrollDeltaY() > 0.0D ? -1 : 1;
        if (!ArchivistsGrimoire.changeSelectedRowToPopulatedRow(spellbookStack, delta, player.registryAccess())) {
            return;
        }

        ClientMagicData.updateSpellSelectionManager();
        Networks.sendToServer(new ClientChangeArchivistsGrimoireRowPacket(delta));
        event.setCanceled(true);
    }
}
