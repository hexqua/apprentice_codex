package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AtelierStationScreen extends AbstractContainerScreen<AtelierStationMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/atelier_station.png");
    private static final Component FLASK_LABEL =
            Component.translatable("container.apprenticecodex.atelier_station.flask");
    private static final Component EMPTY_FILTER_TITLE =
            Component.translatable("container.apprenticecodex.atelier_station.filter.empty");
    private static final Component EMPTY_FILTER_HINT =
            Component.translatable(
                    "container.apprenticecodex.atelier_station.filter.empty_hint",
                    Component.translatable("item.apprenticecodex.spellcasters_flask"))
                    .withStyle(ChatFormatting.GRAY);
    private static final Component FILTER_CLEAR_HINT =
            Component.translatable("container.apprenticecodex.atelier_station.filter.clear_hint")
                    .withStyle(ChatFormatting.GRAY);
    private static final Component FILTER_REPLACE_HINT =
            Component.translatable("container.apprenticecodex.atelier_station.filter.replace_hint")
                    .withStyle(ChatFormatting.GRAY);
    private static final int FILTER_SLOT_SIZE = 16;

    public AtelierStationScreen(AtelierStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        gui.drawString(font, FLASK_LABEL, 43, 41, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        var filterSlot = findHoveredFilterSlot(mouseX, mouseY);
        if (filterSlot >= 0) {
            gui.renderTooltip(
                    font,
                    createFilterTooltip(filterSlot).stream().map(Component::getVisualOrderText).toList(),
                    mouseX,
                    mouseY
            );
            return;
        }

        super.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var filterSlot = findHoveredFilterSlot(mouseX, mouseY);
        if (filterSlot < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return true;
        }

        if (button != 0) {
            return true;
        }

        var buttonId = hasShiftDown()
                ? AtelierStationMenu.encodeFilterClearButtonId(filterSlot)
                : AtelierStationMenu.encodeFilterSetButtonId(filterSlot);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        return true;
    }

    private int findHoveredFilterSlot(double mouseX, double mouseY) {
        for (var slot = 0; slot < AtelierStationBlockEntity.FILTER_SLOT_COUNT; ++slot) {
            var slotX = leftPos + AtelierStationMenu.FILTER_SLOT_X + slot * AtelierStationMenu.FILTER_SLOT_SPACING;
            var slotY = topPos + AtelierStationMenu.FILTER_SLOT_Y;
            if (mouseX >= slotX && mouseX < slotX + FILTER_SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + FILTER_SLOT_SIZE) {
                return slot;
            }
        }

        return -1;
    }

    private List<Component> createFilterTooltip(int slot) {
        var filterItem = menu.getFilterItem(slot);
        if (filterItem.isEmpty()) {
            return List.of(EMPTY_FILTER_TITLE, EMPTY_FILTER_HINT);
        }

        return List.of(filterItem.getHoverName(), FILTER_CLEAR_HINT, FILTER_REPLACE_HINT);
    }
}
