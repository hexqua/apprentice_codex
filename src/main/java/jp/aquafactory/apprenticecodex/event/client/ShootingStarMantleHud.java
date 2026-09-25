package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ShootingStarMantleHud {
    public static final ShootingStarMantleHud INSTANCE = new ShootingStarMantleHud();
    private static final ResourceLocation CONTAINER = texture("energy_container");
    private static final ResourceLocation FULL = texture("energy_full");
    private static final ResourceLocation HALF = texture("energy_half");
    private static final ResourceLocation CHARGING_FULL = texture("charging_full");
    private static final ResourceLocation CHARGING_HALF = texture("charging_half");
    private static final ResourceLocation BLINK = texture("energy_blinking_overlay");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type())
            INSTANCE.render(event.getGuiGraphics());
    }

    private void render(@NotNull GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.options.hideGui || player == null || player.isSpectator()
                || ShootingStarMantleRuntime.findEquipped(player).isEmpty()) return;
        var state = ShootingStarMantleRuntime.state(player);
        if (!state.equipped) return;
        int right = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - ((ForgeGui) minecraft.gui).rightHeight;
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            blit(graphics, CONTAINER, x, y);
            int fill = state.energy * 20 / state.maxEnergy - i * 2;
            if (fill >= 2) blit(graphics, state.recovering ? CHARGING_FULL : FULL, x, y);
            else if (fill == 1) blit(graphics, state.recovering ? CHARGING_HALF : HALF, x, y);
            if (state.blinkTicks > 0 && state.blinkTicks / 3 % 2 == 1) blit(graphics, BLINK, x, y);
        }
        ((ForgeGui) minecraft.gui).rightHeight += 10;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/shooting_star_mantle_" + name + ".png");
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 9, 9, 9, 9);
    }
}
