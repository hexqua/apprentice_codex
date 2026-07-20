package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientCastIntent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ChargecastCatalystbookCastBarHudRenderEvent {
    private ChargecastCatalystbookCastBarHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()
                || !ClientMagicData.isCasting() || ClientMagicData.getCastDuration() <= 0) {
            return;
        }
        var spell = SpellRegistry.getSpell(ClientMagicData.getCastingSpellId());
        if (!ChargecastCatalystbookClientCastIntent.isActive(minecraft.player.getUUID(), spell)) {
            return;
        }

        CastBarHudRenderer.render(
                event.getGuiGraphics(),
                minecraft.font,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                ClientMagicData.getCastCompletionPercent(),
                Utils.timeFromTicks(Math.max(0, ClientMagicData.getCastDurationRemaining()), 1)
        );
    }
}
