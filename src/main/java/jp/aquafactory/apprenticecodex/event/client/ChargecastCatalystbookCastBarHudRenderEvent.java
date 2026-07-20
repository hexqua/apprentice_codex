package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientCastIntent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ChargecastCatalystbookCastBarHudRenderEvent {
    private ChargecastCatalystbookCastBarHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()
                || !ClientMagicData.isCasting() || ClientMagicData.getCastDuration() <= 0) {
            return;
        }
        var spell = SpellRegistry.getSpell(ClientMagicData.getCastingSpellId());
        if (!ChargecastCatalystbookClientCastIntent.matchesActive(spell)) {
            return;
        }

        CastBarHudRenderer.render(
                event.getGuiGraphics(),
                minecraft.font,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                ClientMagicData.getCastCompletionPercent(),
                Utils.timeFromTicks(Math.max(0, ClientMagicData.getCastDurationRemaining()), 1)
        );
    }
}
