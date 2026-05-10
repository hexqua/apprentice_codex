package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class MultipurposeStaffrifleClientAdsState {
    private MultipurposeStaffrifleClientAdsState() {
    }

    public static boolean shouldHandleAsAds(@Nullable LivingEntity entity) {
        if (entity instanceof LocalPlayer localPlayer && isLocalAdsKeyHeld(localPlayer)) {
            return true;
        }

        return MultipurposeStaffrifle.isAdsUse(entity);
    }

    public static boolean isLocalAdsKeyHeld(@Nullable LocalPlayer player) {
        var minecraft = Minecraft.getInstance();
        // 詠唱開始でバニラの使用状態が解除されても、右クリック保持中はADSを継続する。
        return player != null
                && player == minecraft.player
                && minecraft.screen == null
                && !player.isSpectator()
                && minecraft.options.keyUse.isDown()
                && player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle;
    }
}
