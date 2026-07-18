package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientSpellgunInputEvent {
    private static boolean attackLocked;

    private ClientSpellgunInputEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        // Epic Fight 20.14.17 は非 LivingEntity を照準すると BasicAttack を開始しないため、
        // Epic Fight Attack が実際に押され、且つ処理可能な入力だけを委譲する。
        // 通常攻撃と別割り当てなら Forge 側で処理する。1.21.1 側では入力 API と判定条件を再確認する。
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

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

        // Controlify 2.1.7 では Epic Fight の攻撃アクションと通常攻撃が個別に再割り当てできる。
        // 前者だけが押されると Forge の InteractionKeyMappingTriggered を経由しないため、
        // Epic Fight が処理できない攻撃入力を tick 側で補足する。1.21.1 側では入力 API と実行順を再確認する。
        trySendMainhandCast();
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID) && EpicFightClientCompat.isBattleMode();
    }
}
