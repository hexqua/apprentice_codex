package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class QuickcastCartridgeReloadHud {
    private QuickcastCartridgeReloadHud() {}

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()
                || !QuickcastCartridgeClientState.equipped() || !QuickcastCartridgeClientState.reloading()
                || QuickcastCartridgeCasting.findEquipped(minecraft.player).isEmpty()
                || ClientMagicData.isCasting()) return;
        // 表示だけを共用し、Iron'sの詠唱状態やマナ・CD処理には参加させない。
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        CastBarHudRenderer.render(event.getGuiGraphics(), minecraft.font,
                event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(),
                1.0F - QuickcastCartridgeClientState.reloadProgress(partialTick),
                Utils.timeFromTicks((float) QuickcastCartridgeClientState.reloadRemainingTicks(partialTick), 1));
    }
}
