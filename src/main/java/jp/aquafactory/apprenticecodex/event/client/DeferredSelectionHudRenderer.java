package jp.aquafactory.apprenticecodex.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * スニーク解除まで確定しない選択UIの共通描画。
 * 項目データはアイテムとテクスチャアイコンの両方を扱い、将来の選択種別追加を描画側から分離する。
 */
public final class DeferredSelectionHudRenderer {
    private static final int MAX_SLOTS_PER_ROW = 9;
    private static final int SLOT_OUTER_SIZE = 18;
    private static final int SLOT_STEP = SLOT_OUTER_SIZE - 1;
    private static final int SLOT_PADDING = 1;
    private static final int ICON_SIZE = 16;
    private static final int LABEL_OFFSET_FROM_CROSSHAIR = 12;
    private static final int SLOT_OFFSET_FROM_LABEL = 14;
    private static final int ROW_GAP = 2;
    private static final int SLOT_BACKGROUND_COLOR = 0xD0101010;
    private static final int SLOT_FILL_COLOR = 0xE0202327;
    private static final int SLOT_BORDER_COLOR = 0xFF6D7175;
    private static final int SLOT_SELECTED_BORDER_COLOR = 0xFFFFD95C;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int COOLDOWN_OVERLAY_COLOR = 0x80FFFFFF;

    private DeferredSelectionHudRenderer() {
    }

    public static void render(
            GuiGraphics guiGraphics,
            Font font,
            int screenWidth,
            int screenHeight,
            List<View> views,
            int selectedIndex
    ) {
        if (views.isEmpty() || selectedIndex < 0 || selectedIndex >= views.size()) {
            return;
        }

        int rowCount = Math.max(1, (views.size() + MAX_SLOTS_PER_ROW - 1) / MAX_SLOTS_PER_ROW);
        int maxRowSlots = rowCount > 1 ? MAX_SLOTS_PER_ROW : views.size();
        int maxRowWidth = computeRowWidth(maxRowSlots);
        int startX = screenWidth / 2 - maxRowWidth / 2;
        var crosshairY = screenHeight / 2;
        var label = views.get(selectedIndex).displayName();
        var labelX = screenWidth / 2 - font.width(label) / 2;
        var labelY = crosshairY + LABEL_OFFSET_FROM_CROSSHAIR;
        var firstRowY = labelY + SLOT_OFFSET_FROM_LABEL;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        guiGraphics.drawString(font, label, labelX, labelY, LABEL_COLOR, true);

        for (var row = 0; row < rowCount; row++) {
            var rowStartIndex = row * MAX_SLOTS_PER_ROW;
            var rowSlotCount = Math.min(MAX_SLOTS_PER_ROW, views.size() - rowStartIndex);
            var rowY = firstRowY + row * (SLOT_OUTER_SIZE + ROW_GAP);

            for (var column = 0; column < rowSlotCount; column++) {
                renderSlotContent(guiGraphics, font, startX, rowY, column, views.get(rowStartIndex + column));
            }
            for (var column = 0; column < rowSlotCount; column++) {
                if (rowStartIndex + column != selectedIndex) {
                    renderSlotBorder(guiGraphics, startX, rowY, column, SLOT_BORDER_COLOR);
                }
            }

            var selectedColumn = selectedIndex - rowStartIndex;
            if (selectedColumn >= 0 && selectedColumn < rowSlotCount) {
                renderSlotBorder(guiGraphics, startX, rowY, selectedColumn, SLOT_SELECTED_BORDER_COLOR);
            }
        }
        guiGraphics.pose().popPose();
    }

    private static int computeRowWidth(int slotCount) {
        return slotCount <= 0 ? 0 : SLOT_OUTER_SIZE + (slotCount - 1) * SLOT_STEP;
    }

    private static void renderSlotContent(
            GuiGraphics guiGraphics,
            Font font,
            int rowX,
            int rowY,
            int column,
            View view
    ) {
        int slotX = rowX + column * SLOT_STEP;
        int itemX = slotX + SLOT_PADDING;
        int itemY = rowY + SLOT_PADDING;

        guiGraphics.fill(slotX, rowY, slotX + SLOT_OUTER_SIZE, rowY + SLOT_OUTER_SIZE, SLOT_BACKGROUND_COLOR);
        guiGraphics.fill(
                slotX + 1,
                rowY + 1,
                slotX + SLOT_OUTER_SIZE - 1,
                rowY + SLOT_OUTER_SIZE - 1,
                SLOT_FILL_COLOR
        );

        if (view.textureIcon() != null) {
            guiGraphics.blit(view.textureIcon(), itemX, itemY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else if (!view.itemIcon().isEmpty()) {
            guiGraphics.renderItem(view.itemIcon(), itemX, itemY);
        }
        renderCooldownOverlay(guiGraphics, itemX, itemY, view);

        if (view.badgeText() != null && !view.badgeText().isEmpty()) {
            var badgeX = slotX + SLOT_OUTER_SIZE - font.width(view.badgeText()) - 1;
            var badgeY = rowY + SLOT_OUTER_SIZE - font.lineHeight;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(
                    view.badgeText(),
                    badgeX,
                    badgeY,
                    view.badgeColor(),
                    true,
                    guiGraphics.pose().last().pose(),
                    buffers,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );
            guiGraphics.pose().popPose();
            guiGraphics.flush();
        }
    }

    private static void renderCooldownOverlay(GuiGraphics guiGraphics, int itemX, int itemY, View view) {
        if (!view.cooldownActive() || view.cooldownFillRatio() <= 0.0F) {
            return;
        }

        int overlayHeight = Math.max(1, Math.round(ICON_SIZE * view.cooldownFillRatio()));
        int overlayTop = itemY + ICON_SIZE - overlayHeight;
        guiGraphics.fill(itemX, overlayTop, itemX + ICON_SIZE, itemY + ICON_SIZE, COOLDOWN_OVERLAY_COLOR);
    }

    private static void renderSlotBorder(
            GuiGraphics guiGraphics,
            int rowX,
            int rowY,
            int column,
            int borderColor
    ) {
        int slotX = rowX + column * SLOT_STEP;
        guiGraphics.fill(slotX, rowY, slotX + SLOT_OUTER_SIZE, rowY + 1, borderColor);
        guiGraphics.fill(
                slotX,
                rowY + SLOT_OUTER_SIZE - 1,
                slotX + SLOT_OUTER_SIZE,
                rowY + SLOT_OUTER_SIZE,
                borderColor
        );
        guiGraphics.fill(slotX, rowY, slotX + 1, rowY + SLOT_OUTER_SIZE, borderColor);
        guiGraphics.fill(
                slotX + SLOT_OUTER_SIZE - 1,
                rowY,
                slotX + SLOT_OUTER_SIZE,
                rowY + SLOT_OUTER_SIZE,
                borderColor
        );
    }

    public record View(
            Component displayName,
            ItemStack itemIcon,
            @Nullable ResourceLocation textureIcon,
            @Nullable String badgeText,
            int badgeColor,
            boolean cooldownActive,
            float cooldownFillRatio
    ) {
        public View {
            itemIcon = itemIcon.copy();
            cooldownFillRatio = Math.max(0.0F, Math.min(1.0F, cooldownFillRatio));
        }

        public static View forItem(Component displayName, ItemStack icon, String badgeText, int badgeColor) {
            return new View(displayName, icon, null, badgeText, badgeColor, false, 0.0F);
        }
    }
}
