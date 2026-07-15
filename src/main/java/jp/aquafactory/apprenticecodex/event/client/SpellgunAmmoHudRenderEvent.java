package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SpellgunAmmoHudRenderEvent {
    private static final int CROSSHAIR_GAP = 18;
    private static final int PANEL_HEIGHT = 20;
    private static final int PANEL_PADDING_X = 4;
    private static final int ICON_SIZE = 16;
    private static final int INNER_GAP = 3;
    private static final int PANEL_BACKGROUND_COLOR = 0xA0181B1F;
    private static final int PANEL_BORDER_LIGHT_COLOR = 0xBFA8B0B8;
    private static final int PANEL_BORDER_DARK_COLOR = 0xCC0B0D10;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int EMPTY_TEXT_COLOR = 0xFF5555;
    private static final int TEXT_Y_OFFSET = 1;

    private SpellgunAmmoHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName()) || !ApprenticeCodexClientConfig.enableSpellgunAmmoHud()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        var guiGraphics = event.getGuiGraphics();
        var centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        var centerY = minecraft.getWindow().getGuiScaledHeight() / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);

        var mainHandAmmo = resolveAmmoDisplay(player.getMainHandItem(), player);
        if (mainHandAmmo != null) {
            renderAmmoPanel(guiGraphics, minecraft.font, mainHandAmmo, centerX + CROSSHAIR_GAP, centerY - PANEL_HEIGHT / 2);
        }

        var offHandAmmo = resolveAmmoDisplay(player.getOffhandItem(), player);
        if (offHandAmmo != null) {
            var panelWidth = getPanelWidth(minecraft.font, offHandAmmo);
            renderAmmoPanel(guiGraphics, minecraft.font, offHandAmmo, centerX - CROSSHAIR_GAP - panelWidth, centerY - PANEL_HEIGHT / 2);
        }

        guiGraphics.pose().popPose();
    }

    @Nullable
    private static AmmoHudEntry resolveAmmoDisplay(ItemStack weaponStack, Player player) {
        if (weaponStack.getItem() instanceof AbstractSpellGunItem spellGunItem) {
            var ammoItem = spellGunItem.getDisplayedAmmoItem(weaponStack);
            if (ammoItem == null) {
                return null;
            }

            return new AmmoHudEntry(
                    new ItemStack(ammoItem),
                    SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), ammoItem)
            );
        }

        if (weaponStack.getItem() instanceof MultipurposeStaffrifle staffrifle) {
            var ammoItem = staffrifle.getDisplayedAmmoItem(weaponStack);
            return new AmmoHudEntry(
                    new ItemStack(ammoItem),
                    SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), ammoItem)
            );
        }

        return null;
    }

    private static void renderAmmoPanel(GuiGraphics guiGraphics, Font font, AmmoHudEntry entry, int x, int y) {
        var panelWidth = getPanelWidth(font, entry);
        var panelRight = x + panelWidth;
        var panelBottom = y + PANEL_HEIGHT;
        var countText = Integer.toString(entry.count());
        var textColor = entry.count() > 0 ? TEXT_COLOR : EMPTY_TEXT_COLOR;
        var iconX = x + PANEL_PADDING_X;
        var textX = iconX + ICON_SIZE + INNER_GAP;
        var textY = y + (PANEL_HEIGHT - font.lineHeight) / 2 + TEXT_Y_OFFSET;

        // MobEffect欄の色合いだけを借りつつ、可変幅に耐える単純な矩形描画にする。
        guiGraphics.fill(x, y, panelRight, panelBottom, PANEL_BACKGROUND_COLOR);
        guiGraphics.fill(x, y, panelRight, y + 1, PANEL_BORDER_LIGHT_COLOR);
        guiGraphics.fill(x, y, x + 1, panelBottom, PANEL_BORDER_LIGHT_COLOR);
        guiGraphics.fill(x, panelBottom - 1, panelRight, panelBottom, PANEL_BORDER_DARK_COLOR);
        guiGraphics.fill(panelRight - 1, y, panelRight, panelBottom, PANEL_BORDER_DARK_COLOR);

        guiGraphics.renderItem(entry.iconStack(), iconX, y + (PANEL_HEIGHT - ICON_SIZE) / 2);
        guiGraphics.drawString(font, countText, textX, textY, textColor, true);
    }

    private static int getPanelWidth(Font font, AmmoHudEntry entry) {
        return PANEL_PADDING_X * 2 + ICON_SIZE + INNER_GAP + font.width(Integer.toString(entry.count()));
    }

    private record AmmoHudEntry(ItemStack iconStack, int count) {
    }
}
