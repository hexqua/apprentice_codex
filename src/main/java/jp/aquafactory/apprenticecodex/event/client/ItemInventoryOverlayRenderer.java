package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemInventoryOverlayRenderer {
    private static final int SLOT_SIZE = 16;
    private static final int OVERLAY_SIZE = 8;
    private static final int OVERLAY_OFFSET = SLOT_SIZE - OVERLAY_SIZE;
    private static final int SPELL_ICON_TEXTURE_SIZE = 16;

    private ItemInventoryOverlayRenderer() {
    }

    public static void renderIfPresent(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        var overlay = resolveOverlay(stack);
        if (overlay == null) {
            return;
        }

        int overlayX = x + OVERLAY_OFFSET;
        int overlayY = y + OVERLAY_OFFSET;
        if (overlay.textureIcon() != null) {
            renderTextureIcon(guiGraphics, overlay.textureIcon(), overlayX, overlayY);
            return;
        }

        renderItemIcon(guiGraphics, overlay.iconStack(), overlayX, overlayY);
    }

    private static @Nullable OverlayView resolveOverlay(ItemStack stack) {
        var elementalBowOverlay = ElementalBow.getInventoryOverlayView(stack);
        if (elementalBowOverlay != null) {
            return new OverlayView(
                    elementalBowOverlay.iconStack(),
                    elementalBowOverlay.iconKind() == ElementalBow.SelectionIconKind.SPELL
                            ? elementalBowOverlay.spellIcon()
                            : null
            );
        }

        var gauntletOverlay = ScrollcasterGauntlet.getInventoryOverlayIconStack(stack);
        if (!gauntletOverlay.isEmpty()) {
            return new OverlayView(gauntletOverlay, null);
        }

        return null;
    }

    private static void renderTextureIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y) {
        guiGraphics.blit(
                icon,
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

    private record OverlayView(
            ItemStack iconStack,
            @Nullable ResourceLocation textureIcon
    ) {
        private OverlayView {
            iconStack = iconStack.copy();
        }
    }
}
