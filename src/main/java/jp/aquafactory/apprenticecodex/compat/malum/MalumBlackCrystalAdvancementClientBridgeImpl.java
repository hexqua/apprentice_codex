package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.client.VoidRevelationHandler;
import com.sammy.malum.client.VoidRevelationHandler.RevelationType;
import com.sammy.malum.core.handlers.hiding.HiddenTagHandler;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMalumBlackCrystalRevealedPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MalumBlackCrystalAdvancementClientBridgeImpl {
    private static boolean registered;

    private MalumBlackCrystalAdvancementClientBridgeImpl() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        // 登録時にもlistenerが呼ばれるため、接続前は送らずログイン時に既解禁分を同期する。
        HiddenTagHandler.registerHiddenItemListener(MalumBlackCrystalAdvancementClientBridgeImpl::sendIfRevealed);
    }

    static void sendIfRevealed() {
        if (Minecraft.getInstance().getConnection() != null
                && VoidRevelationHandler.hasSeenTheRevelation(RevelationType.BLACK_CRYSTAL)) {
            Networks.sendToServer(new ClientMalumBlackCrystalRevealedPacket());
        }
    }
}
