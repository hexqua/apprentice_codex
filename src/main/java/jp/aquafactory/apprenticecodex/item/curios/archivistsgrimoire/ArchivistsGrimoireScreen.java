package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class ArchivistsGrimoireScreen extends AbstractContainerScreen<ArchivistsGrimoireMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/archivists_grimoire.png");
    private static final int SELECTED_ROW_HIGHLIGHT_U = 0;
    private static final int SELECTED_ROW_HIGHLIGHT_V = 222;
    private static final int SELECTED_ROW_HIGHLIGHT_WIDTH = 164;
    private static final int SELECTED_ROW_HIGHLIGHT_HEIGHT = 20;
    private static final int GRIMOIRE_SLOT_X = 8;
    private static final int GRIMOIRE_SLOT_Y = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SELECTED_ROW_HIGHLIGHT_OFFSET = -2;

    public ArchivistsGrimoireScreen(ArchivistsGrimoireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 129;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderSelectedRowHighlight(gui);
    }

    private void renderSelectedRowHighlight(GuiGraphics gui) {
        var selectedRow = ArchivistsGrimoire.getSelectedRow(menu.getGrimoireStack());
        var x = leftPos + GRIMOIRE_SLOT_X + SELECTED_ROW_HIGHLIGHT_OFFSET;
        var y = topPos + GRIMOIRE_SLOT_Y + selectedRow * SLOT_SIZE + SELECTED_ROW_HIGHLIGHT_OFFSET;
        gui.blit(
                TEXTURE,
                x,
                y,
                SELECTED_ROW_HIGHLIGHT_U,
                SELECTED_ROW_HIGHLIGHT_V,
                SELECTED_ROW_HIGHLIGHT_WIDTH,
                SELECTED_ROW_HIGHLIGHT_HEIGHT);
    }
}
