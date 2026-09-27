package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class MirageAvoidanceClientController {
    private MirageAvoidanceClientController() {
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!isActive()) {
            return;
        }

        var input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isActive()) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!isActive() || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    public static boolean isActive() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return false;
        }

        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return false;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        return spellData != null
                && MirageAvoidanceEvents.isActive(level, spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
    }

    public static void showDuringEffectMessage() {
        Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("ui.apprenticecodex.during_effect").withStyle(ChatFormatting.RED),
                false
        );
    }
}
