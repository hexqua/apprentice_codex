package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookSelectionState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmChargecastCatalystbookIndexPacket;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ChargecastCatalystbookSelectionClientController {
    private static final HandStackResolver.OffhandResolution OFFHAND_RESOLUTION =
            HandStackResolver.OffhandResolution.LOGICAL;
    private static final String EMPTY_SELECTION_LABEL_KEY =
            "ui.apprenticecodex.scrollcaster_gauntlet.select_ui.empty";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/gui/chargecast_catalystbook.png"
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
    private static ChargecastCatalystbookSelectionState activeState;
    private static boolean wasSneakKeyDown;

    private ChargecastCatalystbookSelectionClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var sneakKeyDown = minecraft.screen == null && minecraft.options.keyShift.isDown();
        if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator()) {
            clearState();
            wasSneakKeyDown = sneakKeyDown;
            return;
        }

        if (activeState != null) {
            var stack = resolveHeldStack(player, activeState.hand());
            if (minecraft.screen != null || ClientMagicData.isCasting()
                    || !isSelectableBook(stack) || !sneakKeyDown) {
                clearState();
            } else {
                refreshActiveState(player);
            }
        }

        if (activeState == null && !wasSneakKeyDown && sneakKeyDown
                && minecraft.screen == null && !ClientMagicData.isCasting()) {
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
        if (event.getScrollDeltaY() == 0.0D || activeState.selectableViewCount() <= 1) {
            return;
        }

        moveSelection(event.getScrollDeltaY() > 0.0D ? -1 : 1);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (activeState == null || Minecraft.getInstance().screen != null
                || (!event.isAttack() && !event.isUseItem())) {
            return;
        }

        if (event.isUseItem()) {
            // 選択はホイール操作時に同期済みなので、スニーク中の使用入力はそのまま詠唱へ渡す。
            clearState();
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
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
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.player.isSpectator()
                || minecraft.screen != null) {
            return;
        }
        renderSelectionHud(
                event.getGuiGraphics(),
                minecraft.font,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                minecraft.player,
                activeState
        );
    }

    private static void openSelection(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = resolveHeldStack(player, hand);
        if (!isSelectableBook(stack)) {
            return;
        }
        var views = ChargecastCatalystbook.getSelectionViews(stack);
        if (!views.isEmpty()) {
            activeState = ChargecastCatalystbookSelectionState.open(hand, views);
        }
    }

    private static void refreshActiveState(net.minecraft.world.entity.player.Player player) {
        if (activeState == null) {
            return;
        }
        var stack = resolveHeldStack(player, activeState.hand());
        var refreshedViews = ChargecastCatalystbook.getSelectionViews(stack);
        if (refreshedViews.isEmpty()) {
            clearState();
            return;
        }
        // ItemStackを確定状態として参照し、UIだけが古いカーソルへ戻ることを防ぐ。
        activeState = activeState.refresh(
                refreshedViews,
                ChargecastCatalystbook.getSelectedScrollIndex(stack)
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

    private static void applySelection(ChargecastCatalystbookSelectionState nextState) {
        if (activeState == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            clearState();
            return;
        }
        var stack = resolveHeldStack(player, activeState.hand());
        if (!isSelectableBook(stack)
                || !ChargecastCatalystbook.isSelectableScrollIndex(stack, nextState.selectedScrollIndex())) {
            return;
        }

        // UIカーソルを先に進め、ItemStackと通信へ同じ論理インデックスを渡す。
        // この順序を崩すと、即時反映後の再構築で変更前カーソルへ戻る。
        activeState = nextState;
        var selectedScrollIndex = nextState.selectedScrollIndex();
        if (ChargecastCatalystbook.getSelectedScrollIndex(stack) == selectedScrollIndex) {
            refreshActiveState(player);
            return;
        }

        // 1回の選択変更につき小さなC2Sパケット1件だけを送り、右クリック直前でも選択を確定済みにする。
        ChargecastCatalystbook.setSelectedScrollIndex(stack, selectedScrollIndex);
        if (ChargecastCatalystbook.getSelectedScrollIndex(stack) != selectedScrollIndex) {
            refreshActiveState(player);
            return;
        }
        Networks.sendToServer(new ClientConfirmChargecastCatalystbookIndexPacket(
                nextState.hand(),
                selectedScrollIndex
        ));
        refreshActiveState(player);
    }

    @Nullable
    private static InteractionHand resolveSelectionHand(net.minecraft.world.entity.player.Player player) {
        if (isSelectableBook(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (SneakSelectionUiHandResolver.shouldSuppressOffhandSelection(player)) {
            return null;
        }
        return isSelectableBook(resolveHeldStack(player, InteractionHand.OFF_HAND))
                ? InteractionHand.OFF_HAND
                : null;
    }

    private static boolean isSelectableBook(ItemStack stack) {
        return stack.getItem() instanceof ChargecastCatalystbook book
                && book.isSneakSelectionUiEnabled(stack);
    }

    private static ItemStack resolveHeldStack(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        // 右クリックへ到達できない論理オフハンド無効中は、UI とインデックス変更も同時に止める。
        return HandStackResolver.resolve(player, hand, OFFHAND_RESOLUTION);
    }

    private static void renderSelectionHud(
            GuiGraphics gui,
            Font font,
            int screenWidth,
            int screenHeight,
            net.minecraft.world.entity.player.Player player,
            ChargecastCatalystbookSelectionState state
    ) {
        var views = state.views();
        var rowWidth = SLOT_WIDTH + Math.max(0, views.size() - 1) * SLOT_STEP;
        var startX = screenWidth / 2 - rowWidth / 2;
        var label = ChargecastCatalystbookSelectionState.hasSpell(state.selectedView())
                ? state.selectedView().spellData().getSpell().getDisplayName(player)
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
        if (ChargecastCatalystbookSelectionState.hasSpell(state.selectedView())) {
            var selectedContent = resolveContentPosition(
                    startX, rowY, state.selectedViewIndex(), views.size()
            );
            gui.blit(TEXTURE, selectedContent.x() - 4, selectedContent.y() - 4,
                    SELECTED_FRAME_U, SELECTED_FRAME_V, SELECTED_FRAME_SIZE, SELECTED_FRAME_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE);
        }
        gui.pose().popPose();
    }

    private static void renderSlot(GuiGraphics gui, int rowX, int rowY, int column, int slotCount,
                                   ChargecastCatalystbook.ScrollSelectionView view) {
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
    }

    private record ContentPosition(int x, int y) {
    }
}
