package jp.aquafactory.apprenticecodex.event.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class CastBarHudRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/icons.png");
    private static final int IMAGE_WIDTH = 54;
    private static final int COMPLETION_BAR_WIDTH = 44;
    private static final int IMAGE_HEIGHT = 21;
    private static final int TEXTURE_SIZE = 256;

    private CastBarHudRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight,
                              float completionPercent, String labelText) {
        int barX = screenWidth / 2 - IMAGE_WIDTH / 2;
        int barY = screenHeight / 2 + screenHeight / 8;
        int completionWidth = (int) (COMPLETION_BAR_WIDTH * Mth.clamp(completionPercent, 0.0F, 1.0F)
                + (IMAGE_WIDTH - COMPLETION_BAR_WIDTH) / 2.0F);
        int textX = barX + (IMAGE_WIDTH - font.width(labelText)) / 2;
        int textY = barY + IMAGE_HEIGHT / 2 - font.lineHeight / 2 + 1;

        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 2, IMAGE_WIDTH, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 3, completionWidth, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.drawString(font, labelText, textX, textY, 0xFFFFFF);
    }

    public static void render(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight,
                              float completionPercent,
                              String primaryLabelText, int primaryLabelColor,
                              String secondaryLabelText, int secondaryLabelColor) {
        int barX = screenWidth / 2 - IMAGE_WIDTH / 2;
        int barY = screenHeight / 2 + screenHeight / 8;
        int completionWidth = (int) (COMPLETION_BAR_WIDTH * Mth.clamp(completionPercent, 0.0F, 1.0F)
                + (IMAGE_WIDTH - COMPLETION_BAR_WIDTH) / 2.0F);
        int totalTextWidth = font.width(primaryLabelText) + font.width(secondaryLabelText);
        int textX = barX + (IMAGE_WIDTH - totalTextWidth) / 2;
        int textY = barY + IMAGE_HEIGHT / 2 - font.lineHeight / 2 + 1;

        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 2, IMAGE_WIDTH, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.blit(TEXTURE, barX, barY, 0, IMAGE_HEIGHT * 3, completionWidth, IMAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.drawString(font, primaryLabelText, textX, textY, primaryLabelColor);
        guiGraphics.drawString(font, secondaryLabelText, textX + font.width(primaryLabelText), textY, secondaryLabelColor);
    }
}
