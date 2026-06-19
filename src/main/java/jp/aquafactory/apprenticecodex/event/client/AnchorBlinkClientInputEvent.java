package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientAnchorBlinkPacket;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class AnchorBlinkClientInputEvent {
    private static boolean previousJumpDown;

    private AnchorBlinkClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;

        // 先行入力を受け付けるため、アンカー未着弾時はキー入力を常にfalseとして保持し直すようにする.
        if (player == null || minecraft.level == null || minecraft.screen != null || !hasReadyAnchor(minecraft)) {
            previousJumpDown = false;
            return;
        }

        var jumpDown = minecraft.options.keyJump.isDown();
        if (!jumpDown) {
            previousJumpDown = false;
            return;
        }

        if (!previousJumpDown) {
            Networks.sendToServer(new ClientAnchorBlinkPacket());
        }
        previousJumpDown = true;
    }

    private static boolean hasReadyAnchor(Minecraft minecraft) {
        var player = minecraft.player;
        var level = minecraft.level;
        return player != null && level != null && !level.getEntitiesOfClass(
                AnchorBlinkDaggerEntity.class,
                player.getBoundingBox().inflate(128.0D),
                dagger -> dagger.isReadyAnchorFor(player)
        ).isEmpty();
    }
}
