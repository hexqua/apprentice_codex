package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class BroomControlHelpOverlayEvent {
    private static int displayedBroomId = -1;

    private BroomControlHelpOverlayEvent() {
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null
                || !(player.getVehicle() instanceof AbstractBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            displayedBroomId = -1;
            return;
        }
        if (displayedBroomId == broom.getId()) {
            return;
        }

        displayedBroomId = broom.getId();
        // バニラの騎乗案内は乗客同期時に後から設定されるため、描画直前に置換してちらつきを防ぐ。
        minecraft.gui.setOverlayMessage(broom.createControlHelpMessage().copy()
                .withStyle(ChatFormatting.WHITE), false);
    }
}
