package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmElementalBowModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowModeSelectionClientController {
    @Nullable
    private static ActiveSelectionState activeState;
    private static boolean wasSneakKeyDown;

    private ElementalBowModeSelectionClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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
        if (event.getScrollDeltaY() == 0.0D || activeState.views().size() <= 1) {
            return;
        }

        var direction = event.getScrollDeltaY() > 0.0D ? -1 : 1;
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
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (activeState == null) {
            return;
        }
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator() || minecraft.screen != null) {
            return;
        }

        renderSelectionHud(
                event.getGuiGraphics(),
                minecraft.font,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                activeState
        );
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
        var hudViews = state.views().stream()
                .map(view -> new DeferredSelectionHudRenderer.View(
                        view.displayName(),
                        view.iconStack(),
                        view.iconKind() == ElementalBow.SelectionIconKind.SPELL ? view.spellIcon() : null,
                        view.badgeText(),
                        view.badgeColor(),
                        view.overheatActive(),
                        view.overheatFillRatio()
                ))
                .toList();
        DeferredSelectionHudRenderer.render(
                guiGraphics,
                font,
                screenWidth,
                screenHeight,
                hudViews,
                state.selectedIndex()
        );
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
