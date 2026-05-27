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
    private static final int DISABLED_SLOT_U = 176;
    private static final int DISABLED_SLOT_V = 0;
    private static final int DISABLED_SLOT_SIZE = 16;
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
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderSelectedRowHighlight(gui);
        renderDisabledSlotOverlays(gui);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        var disabledScrollSlot = findHoveredDisabledScrollSlot(mouseX, mouseY);
        if (disabledScrollSlot >= 0) {
            var tooltip = menu.getScrollItem(disabledScrollSlot).isEmpty()
                    ? Component.translatable("container.apprenticecodex.archivists_grimoire.tooltip.unable_scroll_slot.empty")
                    : Component.translatable("container.apprenticecodex.archivists_grimoire.tooltip.unable_scroll_slot.scroll");
            gui.renderTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        super.renderTooltip(gui, mouseX, mouseY);
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

    private void renderDisabledSlotOverlays(GuiGraphics gui) {
        for (var slot = 0; slot < ArchivistsGrimoire.SLOT_COUNT; ++slot) {
            if (menu.isScrollSlotEnabled(slot)) {
                continue;
            }

            var x = GRIMOIRE_SLOT_X + slot % ArchivistsGrimoire.COLUMN_COUNT * SLOT_SIZE + 1;
            var y = GRIMOIRE_SLOT_Y + slot / ArchivistsGrimoire.COLUMN_COUNT * SLOT_SIZE + 1;
            gui.blit(TEXTURE, leftPos + x, topPos + y, DISABLED_SLOT_U, DISABLED_SLOT_V, DISABLED_SLOT_SIZE, DISABLED_SLOT_SIZE);
        }
    }

    private int findHoveredDisabledScrollSlot(int mouseX, int mouseY) {
        for (var slot = 0; slot < ArchivistsGrimoire.SLOT_COUNT; ++slot) {
            if (menu.isScrollSlotEnabled(slot)) {
                continue;
            }

            var slotX = leftPos + GRIMOIRE_SLOT_X + slot % ArchivistsGrimoire.COLUMN_COUNT * SLOT_SIZE + 1;
            var slotY = topPos + GRIMOIRE_SLOT_Y + slot / ArchivistsGrimoire.COLUMN_COUNT * SLOT_SIZE + 1;
            if (mouseX >= slotX && mouseX < slotX + DISABLED_SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + DISABLED_SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }
}
