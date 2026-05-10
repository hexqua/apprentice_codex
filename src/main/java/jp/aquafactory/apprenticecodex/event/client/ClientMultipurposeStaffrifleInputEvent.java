package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientMultipurposeStaffrifleInputEvent {
    private ClientMultipurposeStaffrifleInputEvent() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()) {
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)) {
            return;
        }

        if (ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(player)) {
            return;
        }

        sendSpecialCast(minecraft, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()) {
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)
                || !MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)
                || !minecraft.options.keyAttack.isDown()) {
            return;
        }

        sendSpecialCast(minecraft, true);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (!MultipurposeStaffrifleClientAdsState.shouldHandleAsAds(event.getPlayer())) {
            return;
        }

        event.setNewFovModifier(event.getFovModifier() * MultipurposeStaffrifle.getAdsFovModifier());
    }

    public static void sendSpecialCast(Minecraft minecraft, boolean adsFullAuto) {
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        ClientMultipurposeStaffrifleCastContext.beginPending(player.getUUID(), player.getMainHandItem());
        Networks.sendToServer(new ClientMultipurposeStaffrifleCastPacket(adsFullAuto));
    }
}
