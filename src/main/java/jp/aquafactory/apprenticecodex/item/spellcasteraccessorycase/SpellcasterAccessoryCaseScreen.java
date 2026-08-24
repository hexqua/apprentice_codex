package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfigureSpellcasterAccessoryCaseMenuPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.common.inventory.CurioSlot;

public final class SpellcasterAccessoryCaseScreen extends AbstractContainerScreen<SpellcasterAccessoryCaseMenu> {
    private static final int BASE_IMAGE_WIDTH = 176;
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation CURIOS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("curios", "textures/gui/inventory.png");
    private static final ResourceLocation CURIOS_PANEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex",
                    "textures/gui/sprites/spellcaster_accessory_case_panel.png"
            );
    private static final int CURIOS_PANEL_TEXTURE_SIZE = 12;
    private static final int CURIOS_PANEL_BORDER_SIZE = 5;
    private static final int CURIOS_PANEL_CENTER_SIZE = 2;
    private int appliedCuriosPanelWidth = -1;

    public SpellcasterAccessoryCaseScreen(
            SpellcasterAccessoryCaseMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageHeight = 168;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        var maxVisibleCuriosColumns =
                ApprenticeCodexClientConfig.spellcasterAccessoryCaseMaxVisibleCuriosColumns();
        menu.configureMaxVisibleCuriosColumns(maxVisibleCuriosColumns);
        imageWidth = BASE_IMAGE_WIDTH + menu.getCuriosPanelWidth();
        super.init();
        appliedCuriosPanelWidth = -1;
        syncLayoutGeometry();
        Networks.sendToServer(new ClientConfigureSpellcasterAccessoryCaseMenuPacket(
                menu.containerId,
                maxVisibleCuriosColumns
        ));
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        syncLayoutGeometry();
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        var mainLeft = leftPos + menu.getCuriosPanelWidth();
        gui.blit(CONTAINER_TEXTURE, mainLeft, topPos, 0, 0, BASE_IMAGE_WIDTH, 71);
        gui.blit(CONTAINER_TEXTURE, mainLeft, topPos + 71, 0, 126, BASE_IMAGE_WIDTH, 96);
        renderCuriosPanel(gui);
        renderCuriosSlots(gui);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        if (hoveredSlot instanceof CurioSlot curioSlot
                && !curioSlot.hasItem()
                && menu.getCarried().isEmpty()) {
            gui.renderTooltip(font, Component.literal(curioSlot.getSlotName()), mouseX, mouseY);
            return;
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(
            double mouseX,
            double mouseY,
            int guiLeft,
            int guiTop,
            int mouseButton
    ) {
        return mouseX < guiLeft
                || mouseY < guiTop
                || mouseX >= guiLeft + imageWidth
                || mouseY >= guiTop + imageHeight;
    }

    private void syncLayoutGeometry() {
        var panelWidth = menu.getCuriosPanelWidth();
        if (appliedCuriosPanelWidth == panelWidth) {
            return;
        }

        appliedCuriosPanelWidth = panelWidth;
        imageWidth = BASE_IMAGE_WIDTH + panelWidth;
        leftPos = (width - BASE_IMAGE_WIDTH) / 2 - panelWidth;
        titleLabelX = panelWidth + 8;
        inventoryLabelX = panelWidth + 8;
    }

    private void renderCuriosPanel(GuiGraphics gui) {
        if (!menu.isCuriosPanelVisible()) {
            return;
        }

        var panelWidth = 10 + menu.getVisibleCuriosColumnCount() * 18;
        var panelHeight = 10 + menu.getVisibleCuriosRowCount() * 18;
        blitCuriosPanelNineSliced(gui, leftPos + 1, topPos + 2, panelWidth, panelHeight);
    }

    private static void blitCuriosPanelNineSliced(GuiGraphics gui, int x, int y, int width, int height) {
        var border = CURIOS_PANEL_BORDER_SIZE;
        var right = x + width - border;
        var bottom = y + height - border;
        var centerWidth = width - border * 2;
        var centerHeight = height - border * 2;
        var sourceRight = CURIOS_PANEL_TEXTURE_SIZE - border;
        var sourceBottom = CURIOS_PANEL_TEXTURE_SIZE - border;

        gui.blit(CURIOS_PANEL_SPRITE, x, y, 0, 0, border, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blitRepeating(CURIOS_PANEL_SPRITE, x + border, y, centerWidth, border,
                border, 0, CURIOS_PANEL_CENTER_SIZE, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blit(CURIOS_PANEL_SPRITE, right, y, sourceRight, 0, border, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);

        gui.blitRepeating(CURIOS_PANEL_SPRITE, x, y + border, border, centerHeight,
                0, border, border, CURIOS_PANEL_CENTER_SIZE,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blitRepeating(CURIOS_PANEL_SPRITE, x + border, y + border, centerWidth, centerHeight,
                border, border, CURIOS_PANEL_CENTER_SIZE, CURIOS_PANEL_CENTER_SIZE,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blitRepeating(CURIOS_PANEL_SPRITE, right, y + border, border, centerHeight,
                sourceRight, border, border, CURIOS_PANEL_CENTER_SIZE,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);

        gui.blit(CURIOS_PANEL_SPRITE, x, bottom, 0, sourceBottom, border, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blitRepeating(CURIOS_PANEL_SPRITE, x + border, bottom, centerWidth, border,
                border, sourceBottom, CURIOS_PANEL_CENTER_SIZE, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
        gui.blit(CURIOS_PANEL_SPRITE, right, bottom, sourceRight, sourceBottom, border, border,
                CURIOS_PANEL_TEXTURE_SIZE, CURIOS_PANEL_TEXTURE_SIZE);
    }

    private void renderCuriosSlots(GuiGraphics gui) {
        if (!menu.isCuriosPanelVisible()) {
            return;
        }
        for (var slot : menu.slots) {
            if (slot instanceof CurioSlot) {
                gui.blit(CURIOS_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 7, 7, 18, 18);
            }
        }
    }
}
