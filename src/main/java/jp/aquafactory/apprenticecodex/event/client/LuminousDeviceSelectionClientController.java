package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmLuminousDeviceSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class LuminousDeviceSelectionClientController {
    private static final ResourceLocation CLEAN_MODE_ICON = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/gui/luminous_device_clean.png"
    );

    @Nullable
    private static ActiveSelectionState activeState;
    private static boolean wasSneakKeyDown;

    private LuminousDeviceSelectionClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var sneakKeyDown = minecraft.screen == null && minecraft.options.keyShift.isDown();
        if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator()) {
            clearState();
            wasSneakKeyDown = sneakKeyDown;
            return;
        }

        if (activeState != null) {
            if (minecraft.screen != null || !isValidHeldDevice(player, activeState.hand())) {
                clearState();
            } else {
                refreshActiveState(player);
                if (!sneakKeyDown && activeState != null) {
                    confirmSelection(player);
                }
            }
        }

        if (activeState == null && !wasSneakKeyDown && sneakKeyDown
                && minecraft.screen == null && !player.isUsingItem()) {
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
        moveSelection(event.getScrollDelta() > 0.0D ? -1 : 1);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (activeState == null || Minecraft.getInstance().screen != null
                || (!event.isAttack() && !event.isUseItem())) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            clearState();
            return;
        }

        if (event.isUseItem()) {
            // 同じ接続上で選択パケットを先に送り、続く通常の右クリックパケットに確定後のNBTを使わせる。
            confirmSelection(player);
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        confirmSelection(player);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (activeState == null) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()
                || minecraft.screen != null) {
            return;
        }

        var hudViews = activeState.views().stream()
                .map(LuminousDeviceSelectionClientController::toHudView)
                .toList();
        DeferredSelectionHudRenderer.render(
                event.getGuiGraphics(),
                minecraft.font,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                hudViews,
                activeState.selectedIndex()
        );
    }

    private static void openSelection(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var views = LuminousDevice.getSelectionViews(player.getItemInHand(hand));
        if (views.isEmpty()) {
            return;
        }
        activeState = new ActiveSelectionState(hand, views, findInitialSelectionIndex(views));
    }

    private static DeferredSelectionHudRenderer.View toHudView(LuminousDevice.SelectionView view) {
        if (view.mode() == LuminousDevice.Mode.CLEAN) {
            return new DeferredSelectionHudRenderer.View(
                    view.displayName(),
                    ItemStack.EMPTY,
                    CLEAN_MODE_ICON,
                    view.badgeText(),
                    view.badgeColor(),
                    false,
                    0.0F
            );
        }
        return DeferredSelectionHudRenderer.View.forItem(
                view.displayName(),
                view.iconStack(),
                view.badgeText(),
                view.badgeColor()
        );
    }

    private static void refreshActiveState(net.minecraft.world.entity.player.Player player) {
        if (activeState == null) {
            return;
        }

        var refreshedViews = LuminousDevice.getSelectionViews(player.getItemInHand(activeState.hand()));
        if (refreshedViews.isEmpty()) {
            clearState();
            return;
        }

        var selectedIndex = indexOfSelection(refreshedViews, activeState.selectedView());
        if (selectedIndex < 0) {
            selectedIndex = findInitialSelectionIndex(refreshedViews);
        }
        activeState = new ActiveSelectionState(activeState.hand(), refreshedViews, selectedIndex);
    }

    private static void confirmSelection(net.minecraft.world.entity.player.Player player) {
        if (activeState == null) {
            return;
        }

        var hand = activeState.hand();
        var deviceStack = player.getItemInHand(hand);
        var selectedView = activeState.selectedView();
        var selectionApplied = selectedView.mode() == LuminousDevice.Mode.CLEAN
                ? LuminousDevice.setCleanMode(deviceStack)
                : LuminousDevice.setSelectedStack(deviceStack, selectedView.iconStack());
        if (selectionApplied) {
            Networks.sendToServer(new ClientConfirmLuminousDeviceSelectionPacket(
                    hand,
                    selectedView.mode(),
                    selectedView.iconStack()
            ));
        }
        clearState();
    }

    private static void moveSelection(int direction) {
        if (activeState == null || activeState.views().isEmpty()) {
            return;
        }
        var nextIndex = Math.floorMod(activeState.selectedIndex() + direction, activeState.views().size());
        activeState = new ActiveSelectionState(activeState.hand(), activeState.views(), nextIndex);
    }

    @Nullable
    private static InteractionHand resolveSelectionHand(net.minecraft.world.entity.player.Player player) {
        if (player.getMainHandItem().getItem() instanceof LuminousDevice) {
            return InteractionHand.MAIN_HAND;
        }
        if (SneakSelectionUiHandResolver.shouldSuppressOffhandSelection(player)) {
            return null;
        }
        if (player.getOffhandItem().getItem() instanceof LuminousDevice) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isValidHeldDevice(
            net.minecraft.world.entity.player.Player player,
            InteractionHand hand
    ) {
        return player.getItemInHand(hand).getItem() instanceof LuminousDevice;
    }

    private static int findInitialSelectionIndex(List<LuminousDevice.SelectionView> views) {
        for (int i = 0; i < views.size(); ++i) {
            if (views.get(i).currentSelection()) {
                return i;
            }
        }
        return 0;
    }

    private static int indexOfSelection(
            List<LuminousDevice.SelectionView> views,
            LuminousDevice.SelectionView selectedView
    ) {
        for (int i = 0; i < views.size(); ++i) {
            var view = views.get(i);
            if (view.mode() == selectedView.mode()
                    && (view.mode() == LuminousDevice.Mode.CLEAN
                    || ItemStack.isSameItemSameTags(view.iconStack(), selectedView.iconStack()))) {
                return i;
            }
        }
        return -1;
    }

    private static void clearState() {
        activeState = null;
    }

    private record ActiveSelectionState(
            InteractionHand hand,
            List<LuminousDevice.SelectionView> views,
            int selectedIndex
    ) {
        private ActiveSelectionState {
            views = List.copyOf(views);
        }

        private LuminousDevice.SelectionView selectedView() {
            return views.get(selectedIndex);
        }
    }
}
