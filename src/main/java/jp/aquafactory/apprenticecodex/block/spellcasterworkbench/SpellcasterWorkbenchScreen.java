package jp.aquafactory.apprenticecodex.block.spellcasterworkbench;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SpellcasterWorkbenchScreen extends AbstractContainerScreen<SpellcasterWorkbenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/spellcaster_workbench.png");
    private static final Component CANT_REMOVE_DEFAULT_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.cant_remove_default");
    private static final Component CANT_REMOVE_NOT_ALLOW_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.cant_remove_not_allow");
    private static final Component CANT_REMOVE_NOT_EXIST_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.cant_remove_not_exist");
    private static final Component WARNING_REMOVE_NOT_ALLOW_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.warning_remove_not_allow")
                    .withStyle(ChatFormatting.RED);
    private static final Component CANT_IMBUE_UNSUPPORTED_EQUIPMENT_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.cant_imbue_unsupported_equipment");
    private static final Component WARNING_MAX_SLOT_REACHED_TOOLTIP =
            Component.translatable("ui.apprenticecodex.spellcaster_workbench.warning_max_slot_reached");
    private static final int ICON_GRID_X = 121;
    private static final int ICON_GRID_Y = 16;
    private static final int ICON_COLUMNS = 2;
    private static final int ICON_ROWS = 3;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 18;
    private static final int SCROLL_BAR_X = 156;
    private static final int SCROLL_BAR_Y = 15;
    private static final int SCROLL_BAR_WIDTH = 12;
    private static final int SCROLL_BAR_HEIGHT = 15;
    private static final int SCROLL_TRACK_HEIGHT = 54;
    private static final int RESULT_SLOT_X = 81;
    private static final int RESULT_SLOT_Y = 33;
    private static final int SLOT_SIZE = 16;
    private static final int BLOCKED_RESULT_U = 177;
    private static final int BLOCKED_RESULT_V = 16;
    private static final int BLOCKED_RESULT_SIZE = 15;

    private float scrollOffset;
    private boolean isScrolling;
    private int startIndex;

    public SpellcasterWorkbenchScreen(SpellcasterWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
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
        gui.blit(
                TEXTURE,
                leftPos + SCROLL_BAR_X,
                topPos + SCROLL_BAR_Y + (int) (41.0F * scrollOffset),
                176 + (isScrollBarActive() ? 0 : 12),
                0,
                SCROLL_BAR_WIDTH,
                SCROLL_BAR_HEIGHT
        );
        renderIconButtons(gui, mouseX, mouseY);
        renderIconItems(gui);
        renderBlockedResultOverlay(gui);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        var icons = getSelectableIcons();
        var lastVisibleIconIndex = Math.min(startIndex + ICON_COLUMNS * ICON_ROWS, icons.size());
        for (var index = startIndex; index < lastVisibleIconIndex; ++index) {
            var visibleIndex = index - startIndex;
            var iconX = leftPos + ICON_GRID_X + visibleIndex % ICON_COLUMNS * ICON_WIDTH;
            var iconY = topPos + ICON_GRID_Y + visibleIndex / ICON_COLUMNS * ICON_HEIGHT;
            if (mouseX >= iconX && mouseX < iconX + ICON_WIDTH && mouseY >= iconY && mouseY < iconY + ICON_WIDTH) {
                gui.renderTooltip(font, icons.get(index), mouseX, mouseY);
                return;
            }
        }

        if (isHoveringResultSlot(mouseX, mouseY)) {
            if (menu.isBlockedByDefaultSpellExtraction()) {
                gui.renderTooltip(font, CANT_REMOVE_DEFAULT_TOOLTIP, mouseX, mouseY);
                return;
            }

            if (menu.isBlockedByUnsupportedSpellExtraction()) {
                gui.renderTooltip(font, CANT_REMOVE_NOT_ALLOW_TOOLTIP, mouseX, mouseY);
                return;
            }

            if (menu.isBlockedByMissingSpellExtraction()) {
                gui.renderTooltip(font, CANT_REMOVE_NOT_EXIST_TOOLTIP, mouseX, mouseY);
                return;
            }

            if (menu.isWarnedByUnsupportedEmptySpellExtraction()) {
                gui.renderTooltip(font, WARNING_REMOVE_NOT_ALLOW_TOOLTIP, mouseX, mouseY);
                return;
            }

            if (menu.isBlockedByUnsupportedWorkbenchImbue()) {
                gui.renderTooltip(font, CANT_IMBUE_UNSUPPORTED_EQUIPMENT_TOOLTIP, mouseX, mouseY);
                return;
            }

            if (menu.isBlockedByArchivistsGrimoireMaxSlotReached()) {
                gui.renderTooltip(font, WARNING_MAX_SLOT_REACHED_TOOLTIP, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        isScrolling = false;
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        var icons = getSelectableIcons();
        var lastVisibleIconIndex = Math.min(startIndex + ICON_COLUMNS * ICON_ROWS, icons.size());
        for (var index = startIndex; index < lastVisibleIconIndex; ++index) {
            var visibleIndex = index - startIndex;
            var buttonX = leftPos + ICON_GRID_X + visibleIndex % ICON_COLUMNS * ICON_WIDTH;
            var buttonY = topPos + ICON_GRID_Y + visibleIndex / ICON_COLUMNS * ICON_HEIGHT;
            if (mouseX >= buttonX && mouseX < buttonX + ICON_WIDTH
                    && mouseY >= buttonY - 1 && mouseY < buttonY - 1 + ICON_HEIGHT) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        SpellcasterWorkbenchMenu.encodeRecipeButtonId(index, hasShiftDown())
                );
                return true;
            }
        }

        if (isScrollBarActive()
                && mouseX >= leftPos + SCROLL_BAR_X
                && mouseX < leftPos + SCROLL_BAR_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_Y
                && mouseY < topPos + SCROLL_BAR_Y + SCROLL_TRACK_HEIGHT) {
            isScrolling = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isScrolling || !isScrollBarActive()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        var scrollTop = topPos + SCROLL_BAR_Y;
        var scrollBottom = scrollTop + SCROLL_TRACK_HEIGHT;
        scrollOffset = ((float) mouseY - (float) scrollTop - 7.5F) / ((float) (scrollBottom - scrollTop) - (float) SCROLL_BAR_HEIGHT);
        scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
        startIndex = (int) ((double) (scrollOffset * (float) getOffscreenRows()) + 0.5D) * ICON_COLUMNS;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isScrollBarActive()) {
            return super.mouseScrolled(mouseX, mouseY, scrollY);
        }

        var offscreenRows = getOffscreenRows();
        scrollOffset = Mth.clamp(scrollOffset - (float) scrollY / (float) offscreenRows, 0.0F, 1.0F);
        startIndex = (int) ((double) (scrollOffset * (float) offscreenRows) + 0.5D) * ICON_COLUMNS;
        return true;
    }

    private void renderIconButtons(GuiGraphics gui, int mouseX, int mouseY) {
        var icons = getSelectableIcons();
        var lastVisibleIconIndex = Math.min(startIndex + ICON_COLUMNS * ICON_ROWS, icons.size());
        for (var index = startIndex; index < lastVisibleIconIndex; ++index) {
            var visibleIndex = index - startIndex;
            var buttonX = leftPos + ICON_GRID_X + visibleIndex % ICON_COLUMNS * ICON_WIDTH;
            var buttonY = topPos + ICON_GRID_Y + visibleIndex / ICON_COLUMNS * ICON_HEIGHT;
            var textureY = imageHeight;
            if (mouseX >= buttonX && mouseX < buttonX + ICON_WIDTH
                    && mouseY >= buttonY - 1 && mouseY < buttonY - 1 + ICON_HEIGHT) {
                textureY += 36;
            }

            gui.blit(TEXTURE, buttonX, buttonY - 1, 0, textureY, ICON_WIDTH, ICON_HEIGHT);
        }
    }

    private void renderIconItems(GuiGraphics gui) {
        var icons = getSelectableIcons();
        var lastVisibleIconIndex = Math.min(startIndex + ICON_COLUMNS * ICON_ROWS, icons.size());
        for (var index = startIndex; index < lastVisibleIconIndex; ++index) {
            var visibleIndex = index - startIndex;
            var iconX = leftPos + ICON_GRID_X + visibleIndex % ICON_COLUMNS * ICON_WIDTH;
            var iconY = topPos + ICON_GRID_Y + visibleIndex / ICON_COLUMNS * ICON_HEIGHT;
            gui.renderItem(icons.get(index), iconX, iconY);
        }
    }

    private void renderBlockedResultOverlay(GuiGraphics gui) {
        if (!menu.isResultBlocked()) {
            return;
        }

        var resultX = leftPos + RESULT_SLOT_X;
        var resultY = topPos + RESULT_SLOT_Y;
        gui.blit(
                TEXTURE,
                resultX,
                resultY,
                BLOCKED_RESULT_U,
                BLOCKED_RESULT_V,
                BLOCKED_RESULT_SIZE,
                BLOCKED_RESULT_SIZE
        );
    }

    private boolean isHoveringResultSlot(int mouseX, int mouseY) {
        var resultX = leftPos + RESULT_SLOT_X;
        var resultY = topPos + RESULT_SLOT_Y;
        return mouseX >= resultX
                && mouseX < resultX + SLOT_SIZE
                && mouseY >= resultY
                && mouseY < resultY + SLOT_SIZE;
    }

    private boolean isScrollBarActive() {
        return getSelectableIcons().size() > ICON_COLUMNS * ICON_ROWS;
    }

    private int getOffscreenRows() {
        return Math.max((getSelectableIcons().size() + ICON_COLUMNS - 1) / ICON_COLUMNS - ICON_ROWS, 0);
    }

    private List<ItemStack> getSelectableIcons() {
        return menu.getSelectableIcons();
    }
}
