package jp.aquafactory.apprenticecodex.block.spellcalibrationbench;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SpellCalibrationBenchScreen extends AbstractContainerScreen<SpellCalibrationBenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/spell_calibration_bench.png");
    private static final int DISABLED_SLOT_U = 176;
    private static final int DISABLED_SLOT_V = 0;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int SCROLL_COLUMNS = 5;
    private static final Component SCROLL_LABEL =
            Component.translatable("container.apprenticecodex.spell_calibration_bench.scroll_label");

    public SpellCalibrationBenchScreen(SpellCalibrationBenchMenu menu, Inventory inventory, Component title) {
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
        renderDisabledSlotOverlays(gui);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        gui.drawString(font, SCROLL_LABEL, 79, 23, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        var disabledScrollSlot = findHoveredDisabledScrollSlot(mouseX, mouseY);
        if (disabledScrollSlot >= 0) {
            var tooltip = menu.getScrollItem(disabledScrollSlot).isEmpty()
                    ? Component.translatable(
                            "container.apprenticecodex.spell_calibration_bench.tooltip.unable_scroll_slot.empty",
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()).getHoverName()
                    )
                    : Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.unable_scroll_slot.scroll");
            gui.renderTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        super.renderTooltip(gui, mouseX, mouseY);
    }

    private void renderDisabledSlotOverlays(GuiGraphics gui) {
        if (!menu.hasGauntlet()) {
            for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
                renderDisabledSlotOverlay(
                        gui,
                        SpellCalibrationBenchMenu.ADJUSTMENT_SLOT_X + slot * SLOT_SPACING,
                        SpellCalibrationBenchMenu.ADJUSTMENT_SLOT_Y
                );
            }
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (menu.isScrollSlotEnabled(slot)) {
                continue;
            }
            renderDisabledSlotOverlay(
                    gui,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            );
        }
    }

    private void renderDisabledSlotOverlay(GuiGraphics gui, int x, int y) {
        gui.blit(TEXTURE, leftPos + x, topPos + y, DISABLED_SLOT_U, DISABLED_SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }

    private int findHoveredDisabledScrollSlot(int mouseX, int mouseY) {
        if (!menu.hasGauntlet()) {
            return -1;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (menu.isScrollSlotEnabled(slot)) {
                continue;
            }

            var slotX = leftPos + SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING;
            var slotY = topPos + SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }
}
