package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientRenderState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowCastBarHudRenderEvent {
    private ElementalBowCastBarHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
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

        var remainingTime = Utils.timeFromTicks(castBarState.remainingTicks(), 1);
        CastBarHudRenderer.render(
                event.getGuiGraphics(),
                minecraft.font,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                castBarState.completionPercent(),
                remainingTime
        );
    }
}
