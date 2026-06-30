package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientBlockTargetUseInputEvent {
    private ClientBlockTargetUseInputEvent() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        var player = minecraft.player;
        if (player == null) {
            return;
        }

        var resolvedSpell = RightClickSpellResolver.resolve(player);
        if (resolvedSpell.isEmpty()) {
            return;
        }

        ClientBlockTargetSyncService.trySendForRightClick(resolvedSpell.get());
    }
}
