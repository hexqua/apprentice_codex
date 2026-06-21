package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.systems.RenderSystem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class AutocastAmuletCooldownOverlayEvent {
    private static final int RETICLE_OFFSET_Y = 18;
    private static final int PANEL_PADDING_X = 1;
    private static final int PANEL_PADDING_Y = 1;
    private static final int PANEL_GAP = 2;
    private static final int PANEL_BACKGROUND_COLOR = 0xB0000000;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int THRESHOLD_TEXT_COLOR = 0xFFF0B44A;
    private static final int MANA_LOW_TEXT_COLOR = 0xFFFF6666;
    private static final int ICON_SIZE = 16;
    private static final int SPELL_ICON_TEXTURE_SIZE = 16;

    private AutocastAmuletCooldownOverlayEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        AutocastAmuletClientNotificationState.tick();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }

        var notification = AutocastAmuletClientNotificationState.getActiveNotification();
        if (notification == null) {
            return;
        }

        var guiGraphics = event.getGuiGraphics();
        var font = minecraft.font;
        var text = notification.displayText();
        var textWidth = font.width(text);
        var panelWidth = PANEL_PADDING_X * 2 + ICON_SIZE + PANEL_GAP + textWidth;
        var panelHeight = PANEL_PADDING_Y * 2 + ICON_SIZE;
        var panelX = event.getWindow().getGuiScaledWidth() / 2 - panelWidth / 2;
        var panelY = event.getWindow().getGuiScaledHeight() / 2 + RETICLE_OFFSET_Y;
        var iconX = panelX + PANEL_PADDING_X;
        var iconY = panelY + PANEL_PADDING_Y;
        var textX = iconX + ICON_SIZE + PANEL_GAP;
        var textY = iconY + ICON_SIZE - font.lineHeight;
        var alpha = AutocastAmuletClientNotificationState.getActiveNotificationAlpha();
        if (alpha <= 0.0F) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, applyAlpha(PANEL_BACKGROUND_COLOR, alpha));
        if (notification.itemIcon().isEmpty()) {
            // GUI テキストや塗りつぶしと違い、spell icon の blit はブレンド状態を明示しないと alpha が効かないことがある。
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(
                    notification.spellIcon(),
                    iconX,
                    iconY,
                    0,
                    0,
                    ICON_SIZE,
                    ICON_SIZE,
                    SPELL_ICON_TEXTURE_SIZE,
                    SPELL_ICON_TEXTURE_SIZE
            );
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        } else {
            guiGraphics.renderItem(notification.itemIcon(), iconX, iconY);
        }
        guiGraphics.drawString(font, text, textX, textY, applyAlpha(resolveTextColor(notification), alpha), true);
        guiGraphics.pose().popPose();
    }

    private static int resolveTextColor(AutocastAmuletNotificationController.NotificationEntry notification) {
        return switch (notification.type()) {
            case CAST -> TEXT_COLOR;
            case THRESHOLD -> THRESHOLD_TEXT_COLOR;
            case MANA_LOW -> MANA_LOW_TEXT_COLOR;
            case LINEAR_BUILD_REMAINING -> TEXT_COLOR;
        };
    }

    private static int applyAlpha(int color, float alpha) {
        var normalizedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        var colorAlpha = (color >>> 24) & 0xFF;
        var tintedAlpha = Math.round(colorAlpha * normalizedAlpha) & 0xFF;
        return (tintedAlpha << 24) | (color & 0x00FFFFFF);
    }
}
