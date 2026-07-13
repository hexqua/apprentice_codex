package jp.aquafactory.apprenticecodex.block.spellcalibrationbench;

import com.mojang.blaze3d.systems.RenderSystem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
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
    private static final int RESTRICTED_SLOT_U = 192;
    private static final int RESTRICTED_SLOT_V = 0;
    private static final int MISMATCH_SLOT_U = 208;
    private static final int MISMATCH_SLOT_V = 0;
    private static final int BLOCKED_SLOT_U = 177;
    private static final int BLOCKED_SLOT_V = 16;
    private static final int BLOCKED_SLOT_SIZE = 15;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int SCROLL_COLUMNS = 5;
    private static final Component DEFAULT_SPELL_HINT =
            Component.translatable("ui.apprenticecodex.spell_calibration_bench.hint_default_spell");
    private static final Component CANT_REMOVE_NOT_ALLOW =
            Component.translatable("ui.apprenticecodex.spell_calibration_bench.cant_remove_not_allow");
    private static final Component CANT_IMBUE_UNSUPPORTED_EQUIPMENT =
            Component.translatable("ui.apprenticecodex.spell_calibration_bench.cant_imbue_unsupported_equipment");
    private static final Component WARNING_RESTRICT_IMBUE_CONDITION =
            Component.translatable("ui.apprenticecodex.spell_calibration_bench.warning_restrict_imbue_condition");
    private static final Component WARNING_MISMATCH_CAST_CONDITION =
            Component.translatable("ui.apprenticecodex.spell_calibration_bench.warning_mismatch_cast_condition");
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
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderRestrictedSlotWarnings(gui);
        renderMismatchCastConditionWarnings(gui);
        renderLockedPreviewScrolls(gui);
        renderDisabledSlotOverlays(gui);
        renderUnsupportedImbueOverlays(gui);
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

        var unsupportedSlot = findHoveredUnsupportedImbueSlot(mouseX, mouseY);
        if (unsupportedSlot >= 0) {
            var tooltip = menu.hasTargetSpellAt(unsupportedSlot)
                    ? CANT_REMOVE_NOT_ALLOW
                    : CANT_IMBUE_UNSUPPORTED_EQUIPMENT;
            gui.renderTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        if (findHoveredLockedPreviewScrollSlot(mouseX, mouseY) >= 0) {
            gui.renderTooltip(font, DEFAULT_SPELL_HINT, mouseX, mouseY);
            return;
        }

        var restrictedSlot = findHoveredRestrictedSlot(mouseX, mouseY);
        if (restrictedSlot >= 0) {
            var tooltip = new ArrayList<Component>();
            tooltip.add(WARNING_RESTRICT_IMBUE_CONDITION);
            tooltip.addAll(menu.getImbueRestrictionTooltipLines());
            gui.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        if (findHoveredMismatchedCastConditionSlot(mouseX, mouseY) >= 0) {
            gui.renderTooltip(font, WARNING_MISMATCH_CAST_CONDITION, mouseX, mouseY);
            return;
        }

        if (findHoveredEmptyAdjustmentSlot(mouseX, mouseY) >= 0) {
            gui.renderComponentTooltip(font, createAdjustmentItemHintTooltip(), mouseX, mouseY);
            return;
        }

        super.renderTooltip(gui, mouseX, mouseY);
    }

    private void renderDisabledSlotOverlays(GuiGraphics gui) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (!menu.isAdjustmentSlotEnabled(slot)) {
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

    private void renderRestrictedSlotWarnings(GuiGraphics gui) {
        if (menu.getImbueRestrictionTooltipLines().isEmpty()) {
            return;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!isRestrictedWarningSlot(slot)) {
                continue;
            }
            renderRestrictedSlotWarning(
                    gui,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            );
        }
    }

    private void renderRestrictedSlotWarning(GuiGraphics gui, int x, int y) {
        gui.blit(TEXTURE, leftPos + x, topPos + y, RESTRICTED_SLOT_U, RESTRICTED_SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }

    private void renderMismatchCastConditionWarnings(GuiGraphics gui) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!menu.shouldRenderMismatchCastConditionWarning(slot)) {
                continue;
            }
            renderMismatchCastConditionWarning(
                    gui,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            );
        }
    }

    private void renderMismatchCastConditionWarning(GuiGraphics gui, int x, int y) {
        gui.blit(TEXTURE, leftPos + x, topPos + y, MISMATCH_SLOT_U, MISMATCH_SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }

    private void renderUnsupportedImbueOverlays(GuiGraphics gui) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!menu.shouldRenderUnsupportedImbueOverlay(slot)) {
                continue;
            }
            renderUnsupportedImbueOverlay(
                    gui,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            );
        }
    }

    private void renderUnsupportedImbueOverlay(GuiGraphics gui, int x, int y) {
        gui.blit(TEXTURE, leftPos + x, topPos + y, BLOCKED_SLOT_U, BLOCKED_SLOT_V, BLOCKED_SLOT_SIZE, BLOCKED_SLOT_SIZE);
    }

    private void renderLockedPreviewScrolls(GuiGraphics gui) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.45F);
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            var previewStack = menu.getLockedPreviewScrollItem(slot);
            if (previewStack.isEmpty()) {
                continue;
            }

            gui.renderItem(
                    previewStack,
                    leftPos + SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING,
                    topPos + SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING
            );
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int findHoveredDisabledScrollSlot(int mouseX, int mouseY) {
        if (!menu.hasStoredCalibrationTarget()) {
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

    private int findHoveredUnsupportedImbueSlot(int mouseX, int mouseY) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!menu.shouldRenderUnsupportedImbueOverlay(slot) || !isHoveringScrollSlot(slot, mouseX, mouseY)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    private int findHoveredLockedPreviewScrollSlot(int mouseX, int mouseY) {
        if (!menu.getImbueRestrictionTooltipLines().isEmpty()) {
            return -1;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (menu.getLockedPreviewScrollItem(slot).isEmpty() || !isHoveringScrollSlot(slot, mouseX, mouseY)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    private int findHoveredRestrictedSlot(int mouseX, int mouseY) {
        if (menu.getImbueRestrictionTooltipLines().isEmpty()) {
            return -1;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!isRestrictedWarningSlot(slot) || !isHoveringScrollSlot(slot, mouseX, mouseY)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    private int findHoveredMismatchedCastConditionSlot(int mouseX, int mouseY) {
        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!menu.shouldRenderMismatchCastConditionWarning(slot) || !isHoveringScrollSlot(slot, mouseX, mouseY)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    private boolean isRestrictedWarningSlot(int slot) {
        return menu.isScrollSlotEnabled(slot)
                && menu.getScrollItem(slot).isEmpty();
    }

    private boolean isHoveringScrollSlot(int slot, int mouseX, int mouseY) {
        var slotX = leftPos + SpellCalibrationBenchMenu.SCROLL_SLOT_X + slot % SCROLL_COLUMNS * SLOT_SPACING;
        var slotY = topPos + SpellCalibrationBenchMenu.SCROLL_SLOT_Y + slot / SCROLL_COLUMNS * SLOT_SPACING;
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private int findHoveredEmptyAdjustmentSlot(int mouseX, int mouseY) {
        if (!menu.hasStoredCalibrationTarget()) {
            return -1;
        }

        for (var slot = 0; slot < ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (!menu.isAdjustmentSlotEnabled(slot) || !menu.getAdjustmentItem(slot).isEmpty()) {
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
        if (menu.hasMagiAgentSuit()) {
            lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
            return List.copyOf(lines);
        }
        if (menu.hasMithrilFreecastStaff()) {
            lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
            appendSilverRingHint(lines);
            return List.copyOf(lines);
        }
        if (menu.hasAutocastAmulet()) {
            appendSlotUpgradeHints(lines);
            appendSilverRingHint(lines);
            appendWisdomShardHint(lines);
            return List.copyOf(lines);
        }
        if (menu.hasBulwarkGreatshield()) {
            lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
            appendWisdomShardHint(lines);
            return List.copyOf(lines);
        }
        if (menu.hasParrycastBuckler()) {
            lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
            appendSilverRingHint(lines);
            appendWisdomShardHint(lines);
            return List.copyOf(lines);
        }
        if (menu.hasReflectcastShield()) {
            appendSilverRingHint(lines);
            appendWisdomShardHint(lines);
            return List.copyOf(lines);
        }
        appendSlotUpgradeHints(lines);
        if (menu.hasGauntlet()) {
            lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_enchantment_books"));
            appendTaggedItemHintLines(
                    lines,
                    TagRegistry.Items.SCROLLCASTER_GAUNTLET_ENCHANTMENT_BOOKS,
                    new ItemStack(Items.ENCHANTED_BOOK)
            );
            lines.add(Component.translatable(
                    "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_single_specific_item",
                    new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()).getHoverName()
            ));
            appendSilverRingHint(lines);
        }
        lines.add(Component.translatable("container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_runes"));
        if (menu.hasRevolvercastStaff()) {
            lines.add(Component.translatable(
                    "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_single_specific_item",
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()).getHoverName()
            ));
            appendSilverRingHint(lines);
        }
        return List.copyOf(lines);
    }

    private static void appendSlotUpgradeHints(List<Component> lines) {
        lines.add(SLOT_UPGRADE_GROUP);
        appendTaggedItemHintLines(
                lines,
                TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
        );
    }

    private static void appendSilverRingHint(List<Component> lines) {
        lines.add(Component.translatable(
                "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_single_specific_item",
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()).getHoverName()
        ));
    }

    private static void appendWisdomShardHint(List<Component> lines) {
        lines.add(Component.translatable(
                "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_single_specific_item",
                new ItemStack(ItemRegistry.WISDOM_SHARD.get()).getHoverName()
        ));
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
