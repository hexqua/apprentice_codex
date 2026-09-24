package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class ShootingStarMantleHud implements LayeredDraw.Layer {
    public static final ShootingStarMantleHud INSTANCE = new ShootingStarMantleHud();
    private static final ResourceLocation CONTAINER = texture("energy_container");
    private static final ResourceLocation FULL = texture("energy_full");
    private static final ResourceLocation HALF = texture("energy_half");
    private static final ResourceLocation CHARGING_FULL = texture("charging_full");
    private static final ResourceLocation CHARGING_HALF = texture("charging_half");
    private static final ResourceLocation BLINK = texture("energy_blinking_overlay");

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.options.hideGui || player == null || player.isSpectator()
                || ShootingStarMantleRuntime.findEquipped(player).isEmpty()) return;
        var state = ShootingStarMantleRuntime.state(player);
        if (!state.equipped) return;
        int right = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - minecraft.gui.rightHeight;
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            blit(graphics, CONTAINER, x, y);
            int fill = state.energy * 20 / state.maxEnergy - i * 2;
            if (fill >= 2) blit(graphics, state.recovering ? CHARGING_FULL : FULL, x, y);
            else if (fill == 1) blit(graphics, state.recovering ? CHARGING_HALF : HALF, x, y);
            if (state.blinkTicks > 0 && state.blinkTicks / 3 % 2 == 1) blit(graphics, BLINK, x, y);
        }
        minecraft.gui.rightHeight += 10;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/shooting_star_mantle_" + name + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 9, 9, 9, 9);
    }
}
