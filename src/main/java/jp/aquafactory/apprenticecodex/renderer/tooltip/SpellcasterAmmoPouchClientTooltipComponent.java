package jp.aquafactory.apprenticecodex.renderer.tooltip;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouchTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SpellcasterAmmoPouchClientTooltipComponent implements ClientTooltipComponent {
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/bundle.png");

    private final NonNullList<ItemStack> items;
    private final int highlightedIndex;
    private final boolean full;

    public SpellcasterAmmoPouchClientTooltipComponent(SpellcasterAmmoPouchTooltip tooltip) {
        items = tooltip.items();
        highlightedIndex = tooltip.highlightedIndex();
        full = tooltip.full();
    }

    @Override
    public int getHeight() {
        return gridSizeY() * 20 + 6;
    }

    @Override
    public int getWidth(Font font) {
        return gridSizeX() * 18 + 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        var columns = gridSizeX();
        var rows = gridSizeY();
        var itemIndex = 0;

        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < columns; ++column) {
                renderSlot(x + column * 18 + 1, y + row * 20 + 1, itemIndex++, guiGraphics, font);
            }
        }

        drawBorder(x, y, columns, rows, guiGraphics);
    }

    private void renderSlot(int x, int y, int itemIndex, GuiGraphics guiGraphics, Font font) {
        if (itemIndex >= items.size()) {
            blit(guiGraphics, x, y, full ? Texture.BLOCKED_SLOT : Texture.SLOT);
            return;
        }

        var stack = items.get(itemIndex);
        blit(guiGraphics, x, y, Texture.SLOT);
        guiGraphics.renderItem(stack, x + 1, y + 1, itemIndex);
        guiGraphics.renderItemDecorations(font, stack, x + 1, y + 1, getCountLabel(stack));
        if (itemIndex == highlightedIndex) {
            AbstractContainerScreen.renderSlotHighlight(guiGraphics, x + 1, y + 1, 0);
        }
    }

    private String getCountLabel(ItemStack stack) {
        var count = stack.getCount();
        if (count < 1000) {
            return null;
        }

        // 4桁以上はツールチップ枠内に収まらないため、簡略表記に切り替える。
        return count / 1000 + "K";
    }

    private void drawBorder(int x, int y, int slotWidth, int slotHeight, GuiGraphics guiGraphics) {
        blit(guiGraphics, x, y, Texture.BORDER_CORNER_TOP);
        blit(guiGraphics, x + slotWidth * 18 + 1, y, Texture.BORDER_CORNER_TOP);

        for (int i = 0; i < slotWidth; ++i) {
            blit(guiGraphics, x + 1 + i * 18, y, Texture.BORDER_HORIZONTAL_TOP);
            blit(guiGraphics, x + 1 + i * 18, y + slotHeight * 20, Texture.BORDER_HORIZONTAL_BOTTOM);
        }

        for (int i = 0; i < slotHeight; ++i) {
            blit(guiGraphics, x, y + i * 20 + 1, Texture.BORDER_VERTICAL);
            blit(guiGraphics, x + slotWidth * 18 + 1, y + i * 20 + 1, Texture.BORDER_VERTICAL);
        }

        blit(guiGraphics, x, y + slotHeight * 20, Texture.BORDER_CORNER_BOTTOM);
        blit(guiGraphics, x + slotWidth * 18 + 1, y + slotHeight * 20, Texture.BORDER_CORNER_BOTTOM);
    }

    private void blit(GuiGraphics guiGraphics, int x, int y, Texture texture) {
        guiGraphics.blit(TEXTURE_LOCATION, x, y, 0, texture.x, texture.y, texture.w, texture.h, 128, 128);
    }

    private int gridSizeX() {
        return Math.max(2, (int) Math.ceil(Math.sqrt(items.size() + 1.0D)));
    }

    private int gridSizeY() {
        return (int) Math.ceil((items.size() + 1.0D) / gridSizeX());
    }

    private enum Texture {
        SLOT(0, 0, 18, 20),
        BLOCKED_SLOT(0, 40, 18, 20),
        BORDER_VERTICAL(0, 18, 1, 20),
        BORDER_HORIZONTAL_TOP(0, 20, 18, 1),
        BORDER_HORIZONTAL_BOTTOM(0, 60, 18, 1),
        BORDER_CORNER_TOP(0, 20, 1, 1),
        BORDER_CORNER_BOTTOM(0, 60, 1, 1);

        private final int x;
        private final int y;
        private final int w;
        private final int h;

        Texture(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
