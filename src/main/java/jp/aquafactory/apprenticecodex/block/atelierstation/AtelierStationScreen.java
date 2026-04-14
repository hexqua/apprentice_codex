package jp.aquafactory.apprenticecodex.block.atelierstation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
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
    private static final int FLUID_AREA_X = 12;
    private static final int FLUID_AREA_Y = 20;
    private static final int FLUID_AREA_WIDTH = 8;
    private static final int FLUID_AREA_HEIGHT = 48;
    private static final int FLUID_TEXTURE_TILE_SIZE = 16;

    public AtelierStationScreen(AtelierStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderStoredFluids(gui);
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

        if (isHoveringStoredFluidArea(mouseX, mouseY)) {
            gui.renderTooltip(
                    font,
                    createStoredFluidTooltip().stream().map(Component::getVisualOrderText).toList(),
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

    private void renderStoredFluids(GuiGraphics gui) {
        var entries = menu.getStoredFluidsForDisplay();
        if (entries.isEmpty()) {
            return;
        }

        var entryHeights = calculateEntryHeights(entries);
        var fluidX = leftPos + FLUID_AREA_X;
        var fluidBottomY = topPos + FLUID_AREA_Y + FLUID_AREA_HEIGHT;
        for (var index = 0; index < entries.size(); ++index) {
            var entry = entries.get(index);
            var entryHeight = entryHeights[index];
            if (entryHeight <= 0) {
                continue;
            }

            fluidBottomY -= entryHeight;
            renderStoredFluidEntry(gui, entry, fluidX, fluidBottomY, entryHeight);
        }
    }

    private int[] calculateEntryHeights(List<AtelierStationBlockEntity.StoredPotionEntry> entries) {
        var heights = new int[entries.size()];
        var totalHeight = 0;

        for (var index = 0; index < entries.size(); ++index) {
            var amountMb = entries.get(index).amountMb();
            if (amountMb <= 0) {
                continue;
            }

            var proportionalHeight = (int) Math.floor(
                    (double) amountMb * FLUID_AREA_HEIGHT / AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT
            );
            heights[index] = Math.max(1, proportionalHeight);
            totalHeight += heights[index];
        }

        if (menu.getStoredFluidAmount() >= AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT
                && totalHeight < FLUID_AREA_HEIGHT
                && !entries.isEmpty()) {
            heights[entries.size() - 1] += FLUID_AREA_HEIGHT - totalHeight;
        }

        return heights;
    }

    private void renderStoredFluidEntry(GuiGraphics gui, AtelierStationBlockEntity.StoredPotionEntry entry, int x, int y,
                                        int height) {
        var tintColor = SpellcastersFlask.getStoredItemTintColorForDisplay(entry.representativeItem());
        var sprite = resolveFluidSprite(entry);
        if (sprite == null) {
            gui.fill(x, y, x + FLUID_AREA_WIDTH, y + height, tintColor);
            return;
        }

        var alpha = ((tintColor >>> 24) & 0xFF) / 255.0f;
        var red = ((tintColor >>> 16) & 0xFF) / 255.0f;
        var green = ((tintColor >>> 8) & 0xFF) / 255.0f;
        var blue = (tintColor & 0xFF) / 255.0f;
        var renderedHeight = 0;
        while (renderedHeight < height) {
            var tileHeight = Math.min(FLUID_TEXTURE_TILE_SIZE, height - renderedHeight);
            var drawY = y + height - renderedHeight - tileHeight;
            gui.blit(x, drawY, 0, FLUID_AREA_WIDTH, tileHeight, sprite, red, green, blue, alpha);
            renderedHeight += tileHeight;
        }
    }

    private TextureAtlasSprite resolveFluidSprite(AtelierStationBlockEntity.StoredPotionEntry entry) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }

        var fluidStack = SpellcastersFlask.createFluidForStoredItem(minecraft.level, entry.representativeItem(),
                AtelierStationBlockEntity.MILLIBUCKETS_PER_USE);
        if (fluidStack == null || fluidStack.isEmpty()) {
            return null;
        }

        var stillTexture = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getStillTexture(fluidStack);
        if (stillTexture == null) {
            return null;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    private boolean isHoveringStoredFluidArea(double mouseX, double mouseY) {
        var areaX = leftPos + FLUID_AREA_X;
        var areaY = topPos + FLUID_AREA_Y;
        return mouseX >= areaX && mouseX < areaX + FLUID_AREA_WIDTH
                && mouseY >= areaY && mouseY < areaY + FLUID_AREA_HEIGHT;
    }

    private List<Component> createStoredFluidTooltip() {
        var entries = menu.getStoredFluidsForDisplay();
        var tooltip = new java.util.ArrayList<Component>(entries.size() + 1);
        tooltip.add(createStoredFluidSummaryLine());
        tooltip.add(Component.empty());
        for (var entry : entries) {
            tooltip.add(createStoredFluidEntryLine(entry));
        }
        return tooltip;
    }

    private Component createStoredFluidSummaryLine() {
        var currentUses = menu.getStoredFluidAmount() / AtelierStationBlockEntity.MILLIBUCKETS_PER_USE;
        var maxUses = AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT / AtelierStationBlockEntity.MILLIBUCKETS_PER_USE;
        return Component.translatable("container.apprenticecodex.atelier_station.fluid.summary").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" "))
                .append(Component.translatable("container.apprenticecodex.atelier_station.fluid.summary_count", currentUses, maxUses).withStyle(ChatFormatting.GRAY));
    }

    private Component createStoredFluidEntryLine(AtelierStationBlockEntity.StoredPotionEntry entry) {
        var uses = entry.amountMb() / AtelierStationBlockEntity.MILLIBUCKETS_PER_USE;
        return Component.literal("- ").withStyle(ChatFormatting.YELLOW)
                .append(entry.representativeItem().getHoverName().copy().withStyle(ChatFormatting.BLUE))
                .append(Component.translatable("container.apprenticecodex.atelier_station.fluid.count", uses).withStyle(ChatFormatting.GRAY));
    }
}
