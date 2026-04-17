package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.ElementalBow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ElementalBowInventoryOverlayRenderer {
    private static final int SLOT_SIZE = 16;
    private static final int OVERLAY_SIZE = 8;
    private static final int OVERLAY_OFFSET = SLOT_SIZE - OVERLAY_SIZE;
    private static final int SPELL_ICON_TEXTURE_SIZE = 16;

    private ElementalBowInventoryOverlayRenderer() {
    }

    public static void renderIfPresent(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        var overlay = ElementalBow.getInventoryOverlayView(stack);
        if (overlay == null) {
            return;
        }

        int overlayX = x + OVERLAY_OFFSET;
        int overlayY = y + OVERLAY_OFFSET;
        if (overlay.iconKind() == ElementalBow.SelectionIconKind.SPELL) {
            renderSpellIcon(guiGraphics, overlay, overlayX, overlayY);
            return;
        }

        renderItemIcon(guiGraphics, overlay.iconStack(), overlayX, overlayY);
    }

    private static void renderSpellIcon(GuiGraphics guiGraphics, ElementalBow.InventoryOverlayView overlay, int x, int y) {
        if (overlay.spellIcon() == null) {
            return;
        }

        guiGraphics.blit(
                overlay.spellIcon(),
                x,
                y,
                0,
                0,
                OVERLAY_SIZE,
                OVERLAY_SIZE,
                SPELL_ICON_TEXTURE_SIZE,
                SPELL_ICON_TEXTURE_SIZE
        );
    }

    private static void renderItemIcon(GuiGraphics guiGraphics, ItemStack iconStack, int x, int y) {
        if (iconStack.isEmpty()) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.renderItem(iconStack, x * 2, y * 2);
        guiGraphics.pose().popPose();
    }
}
