package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfigureSpellcasterAccessoryCaseMenuPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import java.util.ArrayList;

public final class SpellcasterAccessoryCaseScreen extends AbstractContainerScreen<SpellcasterAccessoryCaseMenu> {
    private static final int BASE_IMAGE_WIDTH = 176;
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation CURIOS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("curios", "textures/gui/curios/inventory.png");
    private static final ResourceLocation CURIOS_PANEL_SPRITE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_accessory_case_panel");
    private int appliedCuriosPanelWidth = -1;

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
    protected void init() {
        var maxVisibleCuriosColumns =
                ApprenticeCodexClientConfig.spellcasterAccessoryCaseMaxVisibleCuriosColumns();
        menu.configureMaxVisibleCuriosColumns(maxVisibleCuriosColumns);
        imageWidth = BASE_IMAGE_WIDTH + menu.getCuriosPanelWidth();
        super.init();
        appliedCuriosPanelWidth = -1;
        syncLayoutGeometry();
        Networks.sendToServer(new ClientConfigureSpellcasterAccessoryCaseMenuPacket(
                menu.containerId,
                maxVisibleCuriosColumns
        ));
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        syncLayoutGeometry();
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        var mainLeft = leftPos + menu.getCuriosPanelWidth();
        gui.blit(CONTAINER_TEXTURE, mainLeft, topPos, 0, 0, BASE_IMAGE_WIDTH, 71);
        gui.blit(CONTAINER_TEXTURE, mainLeft, topPos + 71, 0, 126, BASE_IMAGE_WIDTH, 96);
        renderCuriosPanel(gui);
        renderCuriosSlots(gui);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        if (hoveredSlot instanceof CurioSlot curioSlot && minecraft != null) {
            var displayStack = getDisplayStack(curioSlot);
            if (!displayStack.isEmpty()) {
                var tooltip = Screen.getTooltipFromItem(minecraft, displayStack);
                if (!curioSlot.isActiveState()) {
                    tooltip.add(Component.empty());
                    tooltip.add(Component.translatable("curios.tooltip.inactive").withStyle(ChatFormatting.RED));
                }
                gui.renderTooltip(font, tooltip, displayStack.getTooltipImage(), mouseX, mouseY);
                return;
            }

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
    protected void renderSlot(@NotNull GuiGraphics gui, @NotNull Slot slot) {
        if (!(slot instanceof CurioSlot curioSlot)) {
            super.renderSlot(gui, slot);
            return;
        }
        if (!menu.isCuriosPanelVisible()) {
            return;
        }

        // Curiosのslot extensionは、Geasのように実体を別データへ保存するアイテムの表示を差し替える。
        var displayStack = getDisplayStack(curioSlot);
        var x = slot.x;
        var y = slot.y;
        var highlightQuickCraft = false;
        var hideStack = slot == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
        var carriedStack = menu.getCarried();
        String countText = null;

        if (slot == clickedSlot && !draggingItem.isEmpty() && isSplittingStack && !displayStack.isEmpty()) {
            displayStack = displayStack.copyWithCount(displayStack.getCount() / 2);
        } else if (isQuickCrafting && quickCraftSlots.contains(slot) && !carriedStack.isEmpty()) {
            if (quickCraftSlots.size() == 1) {
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, carriedStack, true) && menu.canDragTo(slot)) {
                highlightQuickCraft = true;
                var maxStackSize = Math.min(carriedStack.getMaxStackSize(), slot.getMaxStackSize(carriedStack));
                var currentCount = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                var quickCraftCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                        quickCraftSlots,
                        quickCraftingType,
                        carriedStack
                ) + currentCount;
                if (quickCraftCount > maxStackSize) {
                    quickCraftCount = maxStackSize;
                    countText = ChatFormatting.YELLOW + Integer.toString(maxStackSize);
                }
                displayStack = carriedStack.copyWithCount(quickCraftCount);
            } else {
                quickCraftSlots.remove(slot);
                recalculateQuickCraftRemaining();
            }
        }

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 100.0F);
        if (displayStack.isEmpty() && slot.isActive() && minecraft != null) {
            Pair<ResourceLocation, ResourceLocation> noItemIcon = slot.getNoItemIcon();
            if (noItemIcon != null) {
                TextureAtlasSprite sprite = minecraft.getTextureAtlas(noItemIcon.getFirst()).apply(noItemIcon.getSecond());
                gui.blit(x, y, 0, 16, 16, sprite);
                hideStack = true;
            }
        }

        if (!hideStack) {
            if (highlightQuickCraft) {
                gui.fill(x, y, x + 16, y + 16, -2130706433);
            }
            renderSlotContents(gui, displayStack, slot, countText);
        }
        gui.pose().popPose();
    }

    @Override
    protected boolean hasClickedOutside(
            double mouseX,
            double mouseY,
            int guiLeft,
            int guiTop,
            int mouseButton
    ) {
        return mouseX < guiLeft
                || mouseY < guiTop
                || mouseX >= guiLeft + imageWidth
                || mouseY >= guiTop + imageHeight;
    }

    private void syncLayoutGeometry() {
        var panelWidth = menu.getCuriosPanelWidth();
        if (appliedCuriosPanelWidth == panelWidth) {
            return;
        }

        appliedCuriosPanelWidth = panelWidth;
        imageWidth = BASE_IMAGE_WIDTH + panelWidth;
        leftPos = (width - BASE_IMAGE_WIDTH) / 2 - panelWidth;
        titleLabelX = panelWidth + 8;
        inventoryLabelX = panelWidth + 8;
    }

    private void renderCuriosPanel(GuiGraphics gui) {
        if (!menu.isCuriosPanelVisible()) {
            return;
        }

        var panelWidth = 10 + menu.getVisibleCuriosColumnCount() * 18;
        var panelHeight = 10 + menu.getVisibleCuriosRowCount() * 18;
        gui.blitSprite(CURIOS_PANEL_SPRITE, leftPos + 1, topPos + 2, panelWidth, panelHeight);
    }

    private void renderCuriosSlots(GuiGraphics gui) {
        if (!menu.isCuriosPanelVisible()) {
            return;
        }
        for (var slot : menu.slots) {
            if (slot instanceof CurioSlot) {
                gui.blit(CURIOS_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 7, 7, 18, 18);
            }
        }
    }

    private static ItemStack getDisplayStack(CurioSlot slot) {
        return slot.getSlotExtension().getDisplayStack(slot.getSlotContext(), slot.getItem());
    }
}
