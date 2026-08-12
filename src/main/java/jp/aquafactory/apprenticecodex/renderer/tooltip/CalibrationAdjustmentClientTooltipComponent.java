package jp.aquafactory.apprenticecodex.renderer.tooltip;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CalibrationAdjustmentClientTooltipComponent implements ClientTooltipComponent {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/gui/calibration_slot.png"
    );
    private static final int TEXTURE_SIZE = 64;
    private static final int SLOT_WIDTH = 21;
    private static final int SLOT_HEIGHT = 22;
    private static final int SLOT_STEP = SLOT_WIDTH - 1;
    private static final int LEFT_FRAME_U = 0;
    private static final int MIDDLE_FRAME_U = 21;
    private static final int RIGHT_FRAME_U = 43;
    private static final int CONNECTED_FRAME_V = 0;
    private static final int SINGLE_FRAME_U = 0;
    private static final int SINGLE_FRAME_V = 23;
    private static final int SINGLE_FRAME_SIZE = 22;
    private static final int ITEM_OFFSET = 3;
    private static final int BOTTOM_PADDING = 4;

    private final List<ItemStack> items;

    public CalibrationAdjustmentClientTooltipComponent(CalibrationAdjustmentTooltip tooltip) {
        items = tooltip.items();
    }

    @Override
    public int getHeight() {
        return SLOT_HEIGHT + BOTTOM_PADDING;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        if (items.size() == 1) {
            return SINGLE_FRAME_SIZE;
        }
        return items.isEmpty() ? 0 : SLOT_WIDTH + (items.size() - 1) * SLOT_STEP;
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
        if (items.size() == 1) {
            guiGraphics.blit(
                    TEXTURE,
                    x,
                    y,
                    SINGLE_FRAME_U,
                    SINGLE_FRAME_V,
                    SINGLE_FRAME_SIZE,
                    SINGLE_FRAME_SIZE,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
            renderItem(guiGraphics, items.get(0), x + ITEM_OFFSET, y + ITEM_OFFSET, 0);
            return;
        }

        for (var slot = 0; slot < items.size(); ++slot) {
            var slotX = x + slot * SLOT_STEP;
            var frameU = slot == 0 ? LEFT_FRAME_U
                    : slot == items.size() - 1 ? RIGHT_FRAME_U : MIDDLE_FRAME_U;
            guiGraphics.blit(
                    TEXTURE,
                    slotX,
                    y,
                    frameU,
                    CONNECTED_FRAME_V,
                    SLOT_WIDTH,
                    SLOT_HEIGHT,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
            var itemX = slotX + (slot == items.size() - 1 ? ITEM_OFFSET - 1 : ITEM_OFFSET);
            renderItem(guiGraphics, items.get(slot), itemX, y + ITEM_OFFSET, slot);
        }
    }

    private static void renderItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int seed) {
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x, y, seed);
        }
    }
}
