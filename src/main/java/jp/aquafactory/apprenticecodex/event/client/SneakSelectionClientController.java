package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.SneakSelectionState;
import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.SneakSelectionView;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmSneakSelectionPacket;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SneakSelectionClientController {
    private static final String EMPTY_SELECTION_LABEL_KEY = "ui.apprenticecodex.spell_selector.empty";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/gui/spell_selector.png"
    );
    private static final int TEXTURE_SIZE = 64;
    private static final int SLOT_WIDTH = 21;
    private static final int SLOT_HEIGHT = 22;
    private static final int SLOT_STEP = SLOT_WIDTH - 1;
    private static final int ICON_SIZE = 16;
    private static final int LABEL_OFFSET_FROM_CROSSHAIR = 12;
    private static final int SLOT_OFFSET_FROM_LABEL = 14;
    private static final int LABEL_COLOR = 0xFFFFFF;

    private static final int LEFT_FRAME_U = 0;
    private static final int MIDDLE_FRAME_U = 21;
    private static final int RIGHT_FRAME_U = 43;
    private static final int FRAME_V = 0;
    private static final int SELECTED_FRAME_U = 0;
    private static final int SELECTED_FRAME_V = 23;
    private static final int SELECTED_FRAME_SIZE = 24;

    @Nullable
    private static SneakSelectionState activeState;
    @Nullable
    private static ImmediateSneakSelectionUiItem activeItem;
    private static boolean wasSneakKeyDown;

    private SneakSelectionClientController() {
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
            var selection = resolveSelection(player, activeState.hand());
            if (minecraft.screen != null || ClientMagicData.isCasting() || !sneakKeyDown
                    || selection == null || selection.item() != activeItem) {
                clearState();
            } else {
                refreshActiveState(player, selection);
            }
        }

        if (activeState == null && !wasSneakKeyDown && sneakKeyDown
                && minecraft.screen == null && !ClientMagicData.isCasting()) {
            var selection = resolveOpeningSelection(player);
            if (selection != null) {
                openSelection(player, selection);
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
        if (event.getScrollDelta() == 0.0D || activeState.selectableViewCount() <= 1) {
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

        if (event.isUseItem()) {
            // 選択はホイール操作時に同期済みなので、使用入力は選択中の魔法へそのまま渡す。
            clearState();
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
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
        renderSelectionHud(
                event.getGuiGraphics(),
                minecraft.font,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                activeState
        );
    }

    private static void openSelection(Player player, ResolvedSelection selection) {
        var views = selection.item().getSneakSelectionViews(selection.stack());
        if (views.isEmpty()) {
            return;
        }
        activeItem = selection.item();
        activeState = SneakSelectionState.open(
                selection.hand(),
                views,
                selection.item().getSneakSelectionIndex(selection.stack())
        );
    }

    private static void refreshActiveState(Player player, ResolvedSelection selection) {
        if (activeState == null) {
            return;
        }
        var views = selection.item().getSneakSelectionViews(selection.stack());
        if (views.isEmpty()) {
            clearState();
            return;
        }
        // ItemStackを確定状態として参照し、UIだけが古いカーソルへ戻ることを防ぐ。
        activeState = activeState.refresh(
                views,
                selection.item().getSneakSelectionIndex(selection.stack())
        );
    }

    private static void moveSelection(int direction) {
        if (activeState == null || activeState.views().isEmpty()) {
            return;
        }
        var nextState = activeState.move(direction);
        if (nextState != activeState) {
            applySelection(nextState);
        }
    }

    private static void applySelection(SneakSelectionState nextState) {
        if (activeState == null || activeItem == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            clearState();
            return;
        }
        var selection = resolveSelection(player, activeState.hand());
        if (selection == null || selection.item() != activeItem
                || !activeItem.isSneakSelectionIndexSelectable(selection.stack(), nextState.selectedItemIndex())) {
            return;
        }

        // UI、ItemStack、通信へ同じ論理インデックスを渡し、右クリック直前でも選択を確定済みにする。
        activeState = nextState;
        var selectedIndex = nextState.selectedItemIndex();
        if (activeItem.getSneakSelectionIndex(selection.stack()) != selectedIndex) {
            activeItem.setSneakSelectionIndex(selection.stack(), selectedIndex);
            if (activeItem.getSneakSelectionIndex(selection.stack()) == selectedIndex) {
                Networks.sendToServer(new ClientConfirmSneakSelectionPacket(nextState.hand(), selectedIndex));
            }
        }
        refreshActiveState(player, selection);
    }

    @Nullable
    private static ResolvedSelection resolveOpeningSelection(Player player) {
        var mainHand = resolveSelection(player, InteractionHand.MAIN_HAND);
        if (mainHand != null) {
            return mainHand;
        }

        // UI対象のメインハンドが無効な場合だけ、物理オフハンド側へフォールバックする。
        var mainStack = player.getMainHandItem();
        if (mainStack.getItem() instanceof SneakSelectionUiItem mainItem
                && mainItem.isSneakSelectionUiEnabled(mainStack)) {
            return null;
        }
        return resolveSelection(player, InteractionHand.OFF_HAND);
    }

    @Nullable
    private static ResolvedSelection resolveSelection(Player player, InteractionHand hand) {
        var physicalStack = HandStackResolver.resolve(
                player,
                hand,
                HandStackResolver.OffhandResolution.PHYSICAL
        );
        if (!(physicalStack.getItem() instanceof ImmediateSneakSelectionUiItem item)) {
            return null;
        }
        var resolvedStack = item.resolveSneakSelectionStack(player, hand);
        if (resolvedStack.getItem() != item || !item.isSneakSelectionUiEnabled(resolvedStack)) {
            return null;
        }
        return new ResolvedSelection(hand, item, resolvedStack);
    }

    private static void renderSelectionHud(
            GuiGraphics gui,
            Font font,
            int screenWidth,
            int screenHeight,
            SneakSelectionState state
    ) {
        var views = state.views();
        var rowWidth = SLOT_WIDTH + Math.max(0, views.size() - 1) * SLOT_STEP;
        var startX = screenWidth / 2 - rowWidth / 2;
        var hasSelectableView = state.selectableViewCount() > 0;
        var label = hasSelectableView
                ? state.selectedView().displayName()
                : Component.translatable(EMPTY_SELECTION_LABEL_KEY).withStyle(ChatFormatting.RED);
        var labelX = screenWidth / 2 - font.width(label) / 2;
        var labelY = screenHeight / 2 + LABEL_OFFSET_FROM_CROSSHAIR;
        var rowY = labelY + SLOT_OFFSET_FROM_LABEL;

        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 300.0F);
        gui.drawString(font, label, labelX, labelY, LABEL_COLOR, true);
        for (var column = 0; column < views.size(); ++column) {
            renderSlot(gui, startX, rowY, column, views.size(), views.get(column));
        }
        if (hasSelectableView) {
            var selectedContent = resolveContentPosition(startX, rowY, state.selectedViewIndex(), views.size());
            gui.blit(TEXTURE, selectedContent.x() - 4, selectedContent.y() - 4,
                    SELECTED_FRAME_U, SELECTED_FRAME_V, SELECTED_FRAME_SIZE, SELECTED_FRAME_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE);
        }
        gui.pose().popPose();
    }

    private static void renderSlot(
            GuiGraphics gui,
            int rowX,
            int rowY,
            int column,
            int slotCount,
            SneakSelectionView view
    ) {
        var slotX = rowX + column * SLOT_STEP;
        var frameU = column == 0 ? LEFT_FRAME_U
                : column == slotCount - 1 ? RIGHT_FRAME_U : MIDDLE_FRAME_U;
        gui.blit(TEXTURE, slotX, rowY, frameU, FRAME_V,
                SLOT_WIDTH, SLOT_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        var content = resolveContentPosition(rowX, rowY, column, slotCount);
        if (view.icon() != null) {
            gui.blit(view.icon(), content.x(), content.y(), 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    private static ContentPosition resolveContentPosition(int rowX, int rowY, int column, int slotCount) {
        var slotX = rowX + column * SLOT_STEP;
        return new ContentPosition(
                slotX + (column == slotCount - 1 && column > 0 ? 2 : 3),
                rowY + 3
        );
    }

    private static void clearState() {
        activeState = null;
        activeItem = null;
    }

    private record ResolvedSelection(InteractionHand hand, ImmediateSneakSelectionUiItem item, ItemStack stack) {
    }

    private record ContentPosition(int x, int y) {
    }
}
