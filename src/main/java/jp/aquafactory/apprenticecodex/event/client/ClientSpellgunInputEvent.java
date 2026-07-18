package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
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
        if (!event.isAttack() || isEpicFightBattleMode()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof AbstractSpellGunItem)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        trySendMainhandCast();
    }

    public static void trySendMainhandCast() {
        trySendMainhandCast(false);
    }

    public static void trySendEpicFightMainhandCast() {
        trySendMainhandCast(true);
    }

    private static void trySendMainhandCast(boolean deferToEpicFightAttack) {
        if (attackLocked) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun)) {
            return;
        }

        attackLocked = true;
        var spellData = spellgun.getImbuedSpellData(player.getMainHandItem());
        var targetData = ClientBlockTargetSyncService.captureForEmbeddedCast(spellData);
        Networks.sendToServer(new ClientSpellgunCastPacket(targetData, deferToEpicFightAttack));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!isAttackActive()) {
            attackLocked = false;
        }
    }

    private static boolean isAttackActive() {
        if (isEpicFightBattleMode()) {
            return EpicFightClientCompat.isAttackActive();
        }
        return Minecraft.getInstance().options.keyAttack.isDown();
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID) && EpicFightClientCompat.isBattleMode();
    }
}
