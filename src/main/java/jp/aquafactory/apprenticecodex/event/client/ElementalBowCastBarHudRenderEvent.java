package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ElementalBowClientRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowCastBarHudRenderEvent {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/icons.png");
    private static final int IMAGE_WIDTH = 54;
    private static final int COMPLETION_BAR_WIDTH = 44;
    private static final int IMAGE_HEIGHT = 21;
    private static final int TEXTURE_SIZE = 256;

    private ElementalBowCastBarHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }

        var castBarState = ElementalBowClientRenderState.resolveCastBarState();
        if (!castBarState.visible()) {
            return;
        }

        var guiGraphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int barX = screenWidth / 2 - IMAGE_WIDTH / 2;
        int barY = screenHeight / 2 + screenHeight / 8;
        int completionWidth = (int) (COMPLETION_BAR_WIDTH * castBarState.completionPercent() + (IMAGE_WIDTH - COMPLETION_BAR_WIDTH) / 2.0F);
        var remainingTime = Utils.timeFromTicks(castBarState.remainingTicks(), 1);
        var font = minecraft.font;
        int textX = barX + (IMAGE_WIDTH - font.width(remainingTime)) / 2;
        int textY = barY + IMAGE_HEIGHT / 2 - font.lineHeight / 2 + 1;

        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 2, IMAGE_WIDTH, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 3, completionWidth, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.drawString(font, remainingTime, textX, textY, 0xFFFFFF);
    }
}
