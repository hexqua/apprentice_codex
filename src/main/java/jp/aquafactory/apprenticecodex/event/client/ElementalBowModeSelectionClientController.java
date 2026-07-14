package jp.aquafactory.apprenticecodex.event.client;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmElementalBowModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowModeSelectionClientController {
    private static final int MAX_SLOTS_PER_ROW = 9;
    private static final int SLOT_OUTER_SIZE = 18;
    private static final int SLOT_STEP = SLOT_OUTER_SIZE - 1;
    private static final int SLOT_PADDING = 1;
    private static final int ICON_SIZE = 16;
    private static final int LABEL_OFFSET_FROM_CROSSHAIR = 12;
    private static final int SLOT_OFFSET_FROM_LABEL = 14;
    private static final int ROW_GAP = 2;
    private static final int SLOT_BACKGROUND_COLOR = 0xD0101010;
    private static final int SLOT_FILL_COLOR = 0xE0202327;
    private static final int SLOT_BORDER_COLOR = 0xFF6D7175;
    private static final int SLOT_SELECTED_BORDER_COLOR = 0xFFFFD95C;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int OVERHEAT_OVERLAY_COLOR = 0x80FFFFFF;

    @Nullable
    private static ActiveSelectionState activeState;
    private static boolean wasSneakKeyDown;

    private ElementalBowModeSelectionClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        var sneakKeyDown = minecraft.screen == null && minecraft.options.keyShift.isDown();
        if (player == null || level == null || !player.isAlive() || player.isSpectator()) {
            clearState();
            wasSneakKeyDown = sneakKeyDown;
            return;
        }

        if (activeState != null) {
            if (minecraft.screen != null || !isValidHeldBow(player, activeState.hand())) {
                clearState();
            } else {
                refreshActiveState(player);
                if (!sneakKeyDown && activeState != null) {
                    confirmSelection(player, false);
                }
            }
        }

        if (activeState == null && !wasSneakKeyDown && sneakKeyDown && minecraft.screen == null && !player.isUsingItem()) {
            var hand = resolveSelectionHand(player);
            if (hand != null) {
                openSelection(player, hand);
            }
        }

        wasSneakKeyDown = sneakKeyDown;
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (activeState == null || Minecraft.getInstance().screen != null) {
            return;
        }

        event.setCanceled(true);
        if (event.getScrollDelta() == 0.0D || activeState.views().size() <= 1) {
            return;
        }

        var direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        moveSelection(direction);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (activeState == null || Minecraft.getInstance().screen != null) {
            return;
        }

        if (!event.isAttack() && !event.isUseItem()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            clearState();
            return;
        }

        // 選択UI中は確定を優先し、攻撃/使用そのものは誤発火させない。
        event.setCanceled(true);
        event.setSwingHand(false);
        confirmSelection(player, event.isUseItem());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (activeState == null) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator() || minecraft.screen != null) {
            return;
        }

        renderSelectionHud(event.getGuiGraphics(), minecraft.font, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), activeState);
    }

    private static void openSelection(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var views = ElementalBow.getAvailableSelectionViews(player, stack);
        if (views.isEmpty()) {
            return;
        }

        activeState = new ActiveSelectionState(hand, views, findInitialSelectionIndex(views));
    }

    private static void refreshActiveState(net.minecraft.world.entity.player.Player player) {
        if (activeState == null) {
            return;
        }

        var stack = player.getItemInHand(activeState.hand());
        var refreshedViews = ElementalBow.getAvailableSelectionViews(player, stack);
        if (refreshedViews.isEmpty()) {
            clearState();
            return;
        }

        var selectedKey = activeState.selectedSelection();
        int selectedIndex = indexOfSelection(refreshedViews, selectedKey);
        if (selectedIndex < 0) {
            selectedIndex = findInitialSelectionIndex(refreshedViews);
        }
        activeState = new ActiveSelectionState(activeState.hand(), refreshedViews, selectedIndex);
    }

    private static void confirmSelection(net.minecraft.world.entity.player.Player player, boolean continueUse) {
        if (activeState == null) {
            return;
        }

        var selectedView = activeState.selectedView();
        Networks.sendToServer(new ClientConfirmElementalBowModePacket(
                activeState.hand(),
                selectedView.selection().shotMode(),
                selectedView.selection().selectionId(),
                continueUse
        ));
        clearState();
    }

    private static void moveSelection(int direction) {
        if (activeState == null || activeState.views().isEmpty()) {
            return;
        }

        var size = activeState.views().size();
        var nextIndex = Math.floorMod(activeState.selectedIndex() + direction, size);
        activeState = new ActiveSelectionState(activeState.hand(), activeState.views(), nextIndex);
    }

    @Nullable
    private static InteractionHand resolveSelectionHand(net.minecraft.world.entity.player.Player player) {
        if (player.getMainHandItem().getItem() instanceof ElementalBow) {
            return InteractionHand.MAIN_HAND;
        }
        if (SneakSelectionUiHandResolver.shouldSuppressOffhandSelection(player)) {
            return null;
        }
        if (player.getOffhandItem().getItem() instanceof ElementalBow) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isValidHeldBow(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        return player.getItemInHand(hand).getItem() instanceof ElementalBow;
    }

    private static int findInitialSelectionIndex(List<ElementalBow.ModeSelectionView> views) {
        for (int index = 0; index < views.size(); index++) {
            if (views.get(index).currentSelection()) {
                return index;
            }
        }
        return 0;
    }

    private static int indexOfSelection(List<ElementalBow.ModeSelectionView> views, ElementalBow.ModeSelectionKey selectionKey) {
        for (int index = 0; index < views.size(); index++) {
            var viewSelection = views.get(index).selection();
            if (viewSelection.shotMode().equals(selectionKey.shotMode())
                    && java.util.Objects.equals(viewSelection.selectionId(), selectionKey.selectionId())) {
                return index;
            }
        }
        return -1;
    }

    private static void renderSelectionHud(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, ActiveSelectionState state) {
        var views = state.views();
        int rowCount = Math.max(1, (views.size() + MAX_SLOTS_PER_ROW - 1) / MAX_SLOTS_PER_ROW);
        int maxRowSlots = rowCount > 1 ? MAX_SLOTS_PER_ROW : views.size();
        int maxRowWidth = computeRowWidth(maxRowSlots);
        int startX = screenWidth / 2 - maxRowWidth / 2;
        var crosshairY = screenHeight / 2;
        var label = state.selectedView().displayName();
        var labelX = screenWidth / 2 - font.width(label) / 2;
        var labelY = crosshairY + LABEL_OFFSET_FROM_CROSSHAIR;
        var firstRowY = labelY + SLOT_OFFSET_FROM_LABEL;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        guiGraphics.drawString(font, label, labelX, labelY, LABEL_COLOR, true);

        for (var row = 0; row < rowCount; row++) {
            var rowStartIndex = row * MAX_SLOTS_PER_ROW;
            var rowSlotCount = Math.min(MAX_SLOTS_PER_ROW, views.size() - rowStartIndex);
            var rowY = firstRowY + row * (SLOT_OUTER_SIZE + ROW_GAP);

            for (var column = 0; column < rowSlotCount; column++) {
                var index = rowStartIndex + column;
                renderSlotContent(guiGraphics, font, startX, rowY, column, views.get(index));
            }

            for (var column = 0; column < rowSlotCount; column++) {
                var index = rowStartIndex + column;
                if (index != state.selectedIndex()) {
                    renderSlotBorder(guiGraphics, startX, rowY, column, SLOT_BORDER_COLOR);
                }
            }

            var selectedColumn = state.selectedIndex() - rowStartIndex;
            if (selectedColumn >= 0 && selectedColumn < rowSlotCount) {
                renderSlotBorder(guiGraphics, startX, rowY, selectedColumn, SLOT_SELECTED_BORDER_COLOR);
            }
        }

        guiGraphics.pose().popPose();
    }

    private static int computeRowWidth(int slotCount) {
        if (slotCount <= 0) {
            return 0;
        }
        return SLOT_OUTER_SIZE + (slotCount - 1) * SLOT_STEP;
    }

    private static void renderSlotContent(GuiGraphics guiGraphics, Font font, int rowX, int rowY, int column,
                                          ElementalBow.ModeSelectionView view) {
        int slotX = rowX + column * SLOT_STEP;
        int itemX = slotX + SLOT_PADDING;
        int itemY = rowY + SLOT_PADDING;

        guiGraphics.fill(slotX, rowY, slotX + SLOT_OUTER_SIZE, rowY + SLOT_OUTER_SIZE, SLOT_BACKGROUND_COLOR);
        guiGraphics.fill(slotX + 1, rowY + 1, slotX + SLOT_OUTER_SIZE - 1, rowY + SLOT_OUTER_SIZE - 1, SLOT_FILL_COLOR);

        if (view.iconKind() == ElementalBow.SelectionIconKind.SPELL) {
            if (view.spellIcon() != null) {
                guiGraphics.blit(view.spellIcon(), itemX, itemY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            renderSpellOverheatOverlay(guiGraphics, itemX, itemY, view);
        } else {
            guiGraphics.renderItem(view.iconStack(), itemX, itemY);
        }

        if (view.badgeText() != null && !view.badgeText().isEmpty()) {
            var badgeX = slotX + SLOT_OUTER_SIZE - font.width(view.badgeText()) - 1;
            var badgeY = rowY + SLOT_OUTER_SIZE - font.lineHeight;
            // renderItemDecorations と同じ系統の経路で描き、個数表示を常にアイコン前面へ出す。
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(
                    view.badgeText(),
                    badgeX,
                    badgeY,
                    view.badgeColor(),
                    true,
                    guiGraphics.pose().last().pose(),
                    buffers,
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );
            guiGraphics.pose().popPose();
            guiGraphics.flush();
        }
    }

    private static void renderSpellOverheatOverlay(GuiGraphics guiGraphics, int itemX, int itemY, ElementalBow.ModeSelectionView view) {
        if (!view.overheatActive() || view.overheatFillRatio() <= 0.0F) {
            return;
        }

        // バニラのクールダウン表示に寄せて、回復待ちの残量を半透明白で下から覆う。
        int overlayHeight = Math.max(1, Math.round(ICON_SIZE * view.overheatFillRatio()));
        int overlayTop = itemY + ICON_SIZE - overlayHeight;
        guiGraphics.fill(itemX, overlayTop, itemX + ICON_SIZE, itemY + ICON_SIZE, OVERHEAT_OVERLAY_COLOR);
    }

    private static void renderSlotBorder(GuiGraphics guiGraphics, int rowX, int rowY, int column, int borderColor) {
        int slotX = rowX + column * SLOT_STEP;
        guiGraphics.fill(slotX, rowY, slotX + SLOT_OUTER_SIZE, rowY + 1, borderColor);
        guiGraphics.fill(slotX, rowY + SLOT_OUTER_SIZE - 1, slotX + SLOT_OUTER_SIZE, rowY + SLOT_OUTER_SIZE, borderColor);
        guiGraphics.fill(slotX, rowY, slotX + 1, rowY + SLOT_OUTER_SIZE, borderColor);
        guiGraphics.fill(slotX + SLOT_OUTER_SIZE - 1, rowY, slotX + SLOT_OUTER_SIZE, rowY + SLOT_OUTER_SIZE, borderColor);
    }

    private static void clearState() {
        activeState = null;
    }

    private record ActiveSelectionState(
            InteractionHand hand,
            List<ElementalBow.ModeSelectionView> views,
            int selectedIndex
    ) {
        private ActiveSelectionState {
            views = List.copyOf(views);
        }

        private ElementalBow.ModeSelectionView selectedView() {
            return views.get(selectedIndex);
        }

        private ElementalBow.ModeSelectionKey selectedSelection() {
            return selectedView().selection();
        }
    }
}
