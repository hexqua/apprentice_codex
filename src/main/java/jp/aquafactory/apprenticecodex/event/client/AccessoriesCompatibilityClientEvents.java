package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.accessories.AccessoriesCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class AccessoriesCompatibilityClientEvents {
    private static boolean warningPending;

    private AccessoriesCompatibilityClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        warningPending = AccessoriesCompatibility.isUnsupportedLayerLoaded();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        warningPending = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!warningPending) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        // ログイン中の画面初期化を避けて表示し、抑制時もその接続分の通知を消費する。
        warningPending = false;
        if (!ApprenticeCodexClientConfig.suppressAccessoriesCompatibilityLayerWarning()) {
            minecraft.gui.getChat().addMessage(Component.translatable(AccessoriesCompatibility.CHAT_WARNING_KEY)
                    .withStyle(ChatFormatting.RED));
        }
    }
}
