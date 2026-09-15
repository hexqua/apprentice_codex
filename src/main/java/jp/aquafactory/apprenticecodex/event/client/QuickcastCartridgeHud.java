package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class QuickcastCartridgeHud implements LayeredDraw.Layer {
    public static final QuickcastCartridgeHud INSTANCE = new QuickcastCartridgeHud();
    private static final ResourceLocation CONTAINER = texture("charge_container");
    private static final ResourceLocation AVAILABLE = texture("charge_available_full");
    private static final ResourceLocation CHARGING_FULL = texture("charge_charging_full");
    private static final ResourceLocation CHARGING_HALF = texture("charge_charging_half");
    private static final ResourceLocation BLINK = texture("charge_blinking_overlay");

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.options.hideGui || player == null || player.isSpectator()
                || !QuickcastCartridgeClientState.equipped() || QuickcastCartridgeCasting.findEquipped(player).isEmpty()) return;
        int right = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - minecraft.gui.rightHeight;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
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
        minecraft.gui.rightHeight += 10;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/cartridge_" + name + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 9, 9, 9, 9);
    }
}
