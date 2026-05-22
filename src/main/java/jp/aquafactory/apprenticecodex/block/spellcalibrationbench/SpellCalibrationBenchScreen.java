package jp.aquafactory.apprenticecodex.block.spellcalibrationbench;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private static final Component SLOT_UPGRADE_GROUP =
            Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_slot_upgrades");

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
                            SLOT_UPGRADE_GROUP
                    )
                    : Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.unable_scroll_slot.scroll");
            gui.renderTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        if (findHoveredEmptyAdjustmentSlot(mouseX, mouseY) >= 0) {
            gui.renderComponentTooltip(font, createAdjustmentItemHintTooltip(), mouseX, mouseY);
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

    private int findHoveredEmptyAdjustmentSlot(int mouseX, int mouseY) {
        if (!menu.hasGauntlet()) {
            return -1;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (!menu.getAdjustmentItem(slot).isEmpty()) {
                continue;
            }

            var slotX = leftPos + SpellCalibrationBenchMenu.ADJUSTMENT_SLOT_X + slot * SLOT_SPACING;
            var slotY = topPos + SpellCalibrationBenchMenu.ADJUSTMENT_SLOT_Y;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    private List<Component> createAdjustmentItemHintTooltip() {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_title"));
        lines.add(SLOT_UPGRADE_GROUP);
        appendTaggedItemHintLines(
                lines,
                TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
        );
        lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_enchantment_books"));
        appendTaggedItemHintLines(
                lines,
                TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS,
                new ItemStack(Items.ENCHANTED_BOOK)
        );
        lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
        return List.copyOf(lines);
    }

    private static void appendTaggedItemHintLines(List<Component> lines, TagKey<Item> tag, ItemStack fallbackStack) {
        var stacks = ForgeRegistries.ITEMS.getValues().stream()
                .map(ItemStack::new)
                .filter(stack -> stack.is(tag))
                .sorted(Comparator.comparing(stack -> stack.getHoverName().getString()))
                .toList();

        if (stacks.isEmpty()) {
            lines.add(createSpecificItemHint(fallbackStack));
            return;
        }

        for (var stack : stacks) {
            lines.add(createSpecificItemHint(stack));
        }
    }

    private static Component createSpecificItemHint(ItemStack stack) {
        return Component.translatable(
                "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_specific_item",
                stack.getHoverName()
        );
    }
}
