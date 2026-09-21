package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientFullautoRapidcastSpellrifleAdsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class FullautoRapidcastSpellrifleClientAdsState {
    private static LocalPlayer lastPlayer;
    private static boolean sentAiming;

    private FullautoRapidcastSpellrifleClientAdsState() {
    }

    public static boolean shouldHandleAsAds(@Nullable LivingEntity entity) {
        if (entity instanceof LocalPlayer localPlayer) {
            return isLocalAdsKeyHeld(localPlayer);
        }

        return FullautoRapidcastSpellrifle.isAdsUse(entity);
    }

    public static boolean isLocalAdsKeyHeld(@Nullable LocalPlayer player) {
        var minecraft = Minecraft.getInstance();
        // 詠唱開始でバニラの使用状態が解除されても、右クリック保持中はADSを継続する。
        return player != null
                && player == minecraft.player
                && minecraft.screen == null
                && !player.isSpectator()
                && player.isAlive()
                && !isEpicFightBattleMode()
                && minecraft.options.keyUse.isDown()
                && player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle;
    }

    public static void syncToServer() {
        var player = Minecraft.getInstance().player;
        if (player != lastPlayer) {
            lastPlayer = player;
            sentAiming = false;
        }
        if (player == null) {
            return;
        }
        boolean aiming = isLocalAdsKeyHeld(player);
        // 射撃による使用状態解除後も、ADS入力をサーバーへ維持する。
        if (aiming || sentAiming) {
            Networks.sendToServer(new ClientFullautoRapidcastSpellrifleAdsPacket(aiming));
        }
        sentAiming = aiming;
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)
                && EpicFightClientCompat.isBattleMode();
    }
}
