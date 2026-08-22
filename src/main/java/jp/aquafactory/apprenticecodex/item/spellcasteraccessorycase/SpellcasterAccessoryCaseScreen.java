package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.ArrayList;

public final class SpellcasterAccessoryCaseScreen extends AbstractContainerScreen<SpellcasterAccessoryCaseMenu> {
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation CURIOS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("curios", "textures/gui/curios/inventory.png");

    public SpellcasterAccessoryCaseScreen(
            SpellcasterAccessoryCaseMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageHeight = 168;
        inventoryLabelY = 74;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(CONTAINER_TEXTURE, leftPos, topPos, 0, 0, imageWidth, 71);
        gui.blit(CONTAINER_TEXTURE, leftPos, topPos + 71, 0, 126, imageWidth, 96);
        renderCuriosSlots(gui);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        if (hoveredSlot instanceof CurioSlot curioSlot && hoveredSlot.getItem().isEmpty()) {
            var tooltip = new ArrayList<>(curioSlot.getSlotExtension().getSlotTooltip(
                    curioSlot.getSlotContext(),
                    net.minecraft.world.item.TooltipFlag.NORMAL
            ));
            if (tooltip.isEmpty()) {
                tooltip.add(Component.literal(curioSlot.getSlotName()));
            }
            if (!curioSlot.isActiveState()) {
                tooltip.add(Component.translatable("curios.tooltip.inactive").withStyle(ChatFormatting.RED));
            }
            gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(
            double mouseX,
            double mouseY,
            int guiLeft,
            int guiTop,
            int mouseButton
    ) {
        var panelLeft = guiLeft - menu.getCuriosPanelWidth();
        return mouseX < panelLeft
                || mouseY < guiTop
                || mouseX >= guiLeft + imageWidth
                || mouseY >= guiTop + imageHeight;
    }

    private void renderCuriosSlots(GuiGraphics gui) {
        for (var slot : menu.slots) {
            if (slot instanceof CurioSlot) {
                gui.blit(CURIOS_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 7, 7, 18, 18);
            }
        }
    }
}
