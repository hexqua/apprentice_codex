package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleAdsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class MultipurposeStaffrifleClientAdsState {
    private static LocalPlayer lastPlayer;
    private static boolean sentAiming;

    private MultipurposeStaffrifleClientAdsState() {
    }

    public static boolean shouldHandleAsAds(@Nullable LivingEntity entity) {
        if (entity instanceof LocalPlayer localPlayer) {
            return isLocalAdsKeyHeld(localPlayer);
        }

        return MultipurposeStaffrifle.isAdsUse(entity);
    }

    public static boolean isLocalAdsKeyHeld(@Nullable LocalPlayer player) {
        var minecraft = Minecraft.getInstance();
        // 詠唱開始でバニラの使用状態が解除されても、右クリック保持中はADSを継続する。
        return player != null
                && player == minecraft.player
                && minecraft.screen == null
                && player.isAlive()
                && !minecraft.isPaused()
                && !player.isSpectator()
                && !isEpicFightBattleMode()
                && minecraft.options.keyUse.isDown()
                && player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle;
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
            Networks.sendToServer(new ClientMultipurposeStaffrifleAdsPacket(aiming));
        }
        sentAiming = aiming;
    }

    public static boolean isScoped(@Nullable LivingEntity player) {
        return player instanceof LocalPlayer localPlayer
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()
                && isLocalAdsKeyHeld(localPlayer)
                && MultipurposeStaffrifle.hasSpyglass(player.getMainHandItem(), player.level().registryAccess());
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)
                && EpicFightClientCompat.isBattleMode();
    }
}
