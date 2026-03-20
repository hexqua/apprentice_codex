package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public final class AtelierStationScreen extends AbstractContainerScreen<AtelierStationMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/atelier_station.png");
    private static final Component FLASK_LABEL =
            Component.translatable("container.apprenticecodex.atelier_station.flask");

    public AtelierStationScreen(AtelierStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        gui.drawString(font, FLASK_LABEL, 43, 41, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
