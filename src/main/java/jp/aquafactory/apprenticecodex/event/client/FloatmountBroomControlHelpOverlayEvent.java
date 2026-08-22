package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FloatmountBroomControlHelpOverlayEvent {
    private static int displayedBroomId = -1;

    private FloatmountBroomControlHelpOverlayEvent() {
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderGuiOverlayEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null
                || !(player.getVehicle() instanceof FloatmountBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            displayedBroomId = -1;
            return;
        }
        if (displayedBroomId == broom.getId()) {
            return;
        }

        displayedBroomId = broom.getId();
        // バニラの騎乗案内は乗客同期時に後から設定されるため、描画直前に置換してちらつきを防ぐ。
        minecraft.gui.setOverlayMessage(Component.translatable(
                "ui.apprenticecodex.floatmount_broom.control_help",
                Component.keybind("key.jump"),
                Component.keybind("key.sprint"),
                Component.keybind("key.sneak")
        ).withStyle(ChatFormatting.WHITE), false);
    }
}
