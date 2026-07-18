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
        if (!event.isAttack()) {
            return;
        }

        // Epic Fight 21.17.3.1 は非 LivingEntity を照準すると ComboAttacks を開始しないため、
        // Epic Fight Attack が実際に押され、且つ処理可能な入力だけを委譲する。
        // 通常攻撃と別割り当てなら NeoForge 側で処理する。
        if (isEpicFightBattleMode()
                && EpicFightClientCompat.isAttackActive()
                && EpicFightClientCompat.canHandleAttackInput()) {
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
        var epicFightBattleMode = isEpicFightBattleMode();
        var attackActive = epicFightBattleMode
                ? EpicFightClientCompat.isAttackActive()
                : Minecraft.getInstance().options.keyAttack.isDown();

        if (epicFightBattleMode && attackActive) {
            trySendUnhandledEpicFightAttackCast();
        } else if (!attackActive) {
            attackLocked = false;
        }
    }

    private static void trySendUnhandledEpicFightAttackCast() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || EpicFightClientCompat.canHandleAttackInput()) {
            return;
        }

        // Controlify 2.5.0 では Epic Fight の攻撃アクションと通常攻撃が個別に再割り当てできる。
        // 前者だけが押されると NeoForge の InteractionKeyMappingTriggered を経由しないため、
        // Epic Fight が処理できない攻撃入力を tick 側で補足する。
        trySendMainhandCast();
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID) && EpicFightClientCompat.isBattleMode();
    }
}
