package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientSpellgunInputEvent {
    private static boolean attackLocked;

    private ClientSpellgunInputEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        if (attackLocked) {
            return;
        }

        attackLocked = true;
        var spellData = spellgun.getImbuedSpellData(player.getMainHandItem());
        var targetData = ClientBlockTargetSyncService.captureForEmbeddedCast(spellData);
        Networks.sendToServer(new ClientSpellgunCastPacket(targetData));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            attackLocked = false;
        }
    }
}
