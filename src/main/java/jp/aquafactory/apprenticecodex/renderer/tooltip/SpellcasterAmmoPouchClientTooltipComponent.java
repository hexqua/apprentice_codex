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
    private static final ResourceLocation BACKGROUND_SPRITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "container/bundle/background");

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
        return backgroundHeight() + 4;
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

        guiGraphics.blitSprite(BACKGROUND_SPRITE, x, y, getWidth(font), backgroundHeight());

        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < columns; ++column) {
                renderSlot(x + column * 18 + 1, y + row * 20 + 1, itemIndex++, guiGraphics, font);
            }
        }
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

    private void blit(GuiGraphics guiGraphics, int x, int y, Texture texture) {
        guiGraphics.blitSprite(texture.location, x, y, texture.width, texture.height);
    }

    private int gridSizeX() {
        return Math.max(2, (int) Math.ceil(Math.sqrt(items.size() + 1.0D)));
    }

    private int gridSizeY() {
        return (int) Math.ceil((items.size() + 1.0D) / gridSizeX());
    }

    private int backgroundHeight() {
        return gridSizeY() * 20 + 2;
    }

    private enum Texture {
        SLOT("container/bundle/slot", 18, 20),
        BLOCKED_SLOT("container/bundle/blocked_slot", 18, 20);

        private final ResourceLocation location;
        private final int width;
        private final int height;

        Texture(String path, int width, int height) {
            location = ResourceLocation.fromNamespaceAndPath("minecraft", path);
            this.width = width;
            this.height = height;
        }
    }
}
