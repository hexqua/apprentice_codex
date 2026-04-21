package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ElementalBowClientRenderState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowCastBarHudRenderEvent {
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

        var remainingTime = Utils.timeFromTicks(castBarState.remainingTicks(), 1);
        CastBarHudRenderer.render(
                event.getGuiGraphics(),
                minecraft.font,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                castBarState.completionPercent(),
                remainingTime
        );
    }
}
