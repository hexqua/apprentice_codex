package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class QuickcastCartridgeHud {
    public static final QuickcastCartridgeHud INSTANCE = new QuickcastCartridgeHud();
    private static final ResourceLocation CONTAINER = texture("charge_container");
    private static final ResourceLocation AVAILABLE = texture("charge_available_full");
    private static final ResourceLocation CHARGING_FULL = texture("charge_charging_full");
    private static final ResourceLocation CHARGING_HALF = texture("charge_charging_half");
    private static final ResourceLocation BLINK = texture("charge_blinking_overlay");

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type())
            INSTANCE.render(event.getGuiGraphics(), event.getPartialTick());
    }

    private void render(GuiGraphics graphics, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.options.hideGui || player == null || player.isSpectator()
                || !QuickcastCartridgeClientState.equipped() || QuickcastCartridgeCasting.findEquipped(player).isEmpty()) return;
        int right = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - ((ForgeGui) minecraft.gui).rightHeight;
        int steps = QuickcastCartridgeClientState.chargeSteps(partialTick);
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            blit(graphics, CONTAINER, x, y);
            int fill = steps - i * 2;
            if (QuickcastCartridgeClientState.available()) blit(graphics, AVAILABLE, x, y);
            else if (fill >= 2) blit(graphics, CHARGING_FULL, x, y);
            else if (fill == 1) blit(graphics, CHARGING_HALF, x, y);
            if (QuickcastCartridgeClientState.blink()) blit(graphics, BLINK, x, y);
        }
        ((ForgeGui) minecraft.gui).rightHeight += 10;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/cartridge_" + name + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 9, 9, 9, 9);
    }
}
