package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmScrollcasterGauntletIndexPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ScrollcasterGauntletSelectionClientController {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    private static final String EMPTY_SELECTION_LABEL_KEY =
            "ui.apprenticecodex.scrollcaster_gauntlet.select_ui.empty";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/scrollcaster_gauntlet.png");
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
    private static ActiveSelectionState activeState;
    private static boolean wasSneakKeyDown;

    private ScrollcasterGauntletSelectionClientController() {
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
            if (minecraft.screen != null || ClientMagicData.isCasting() || !isValidHeldGauntlet(player, activeState.hand())) {
                clearState();
            } else {
                refreshActiveState(player);
                if (!sneakKeyDown && activeState != null) {
                    confirmSelection();
                }
            }
        }

        if (activeState == null
                && !wasSneakKeyDown
                && sneakKeyDown
                && minecraft.screen == null
                && !ClientMagicData.isCasting()) {
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
        if (event.getScrollDelta() == 0.0D || activeState.selectableViewCount() <= 1) {
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

        event.setCanceled(true);
        event.setSwingHand(false);
        confirmSelection();
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

        renderSelectionHud(
                event.getGuiGraphics(),
                minecraft.font,
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),
                activeState
        );
    }

    private static void openSelection(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        var stack = resolveHeldGauntletStack(player, hand);
        var views = ScrollcasterGauntlet.getSelectionViews(stack);
        if (views.isEmpty()) {
            return;
        }

        activeState = new ActiveSelectionState(hand, views, findInitialViewIndex(views));
    }

    private static void refreshActiveState(net.minecraft.world.entity.player.Player player) {
        if (activeState == null) {
            return;
        }

        var selectedScrollIndex = activeState.selectedView().scrollIndex();
        var refreshedViews = ScrollcasterGauntlet.getSelectionViews(resolveHeldGauntletStack(player, activeState.hand()));
        if (refreshedViews.isEmpty()) {
            clearState();
            return;
        }

        var selectedViewIndex = indexOfScroll(refreshedViews, selectedScrollIndex);
        if (selectedViewIndex < 0) {
            selectedViewIndex = findInitialViewIndex(refreshedViews);
        }
        activeState = new ActiveSelectionState(activeState.hand(), refreshedViews, selectedViewIndex);
    }

    private static void confirmSelection() {
        if (activeState == null) {
            return;
        }

        var selectedView = activeState.selectedView();
        if (!selectedView.hasSpell()) {
            clearState();
            return;
        }

        Networks.sendToServer(new ClientConfirmScrollcasterGauntletIndexPacket(
                activeState.hand(),
                selectedView.scrollIndex()
        ));
        clearState();
    }

    private static void moveSelection(int direction) {
        if (activeState == null || activeState.views().isEmpty()) {
            return;
        }

        var size = activeState.views().size();
        for (var offset = 1; offset <= size; ++offset) {
            var nextIndex = Math.floorMod(activeState.selectedViewIndex() + direction * offset, size);
            if (activeState.views().get(nextIndex).hasSpell()) {
                activeState = new ActiveSelectionState(activeState.hand(), activeState.views(), nextIndex);
                return;
            }
        }
    }

    @Nullable
    private static InteractionHand resolveSelectionHand(net.minecraft.world.entity.player.Player player) {
        if (player.getMainHandItem().getItem() instanceof ScrollcasterGauntlet) {
            return InteractionHand.MAIN_HAND;
        }
        if (SneakSelectionUiHandResolver.shouldSuppressOffhandSelection(player)) {
            return null;
        }
        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)
                && BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return InteractionHand.OFF_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof ScrollcasterGauntlet) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isValidHeldGauntlet(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        return resolveHeldGauntletStack(player, hand).getItem() instanceof ScrollcasterGauntlet;
    }

    private static net.minecraft.world.item.ItemStack resolveHeldGauntletStack(
            net.minecraft.world.entity.player.Player player,
            InteractionHand hand
    ) {
        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return BetterCombatScrollcasterGauntletCompat.getResolvedHeldStack(player, hand);
        }
        return player.getItemInHand(hand);
    }

    private static int findInitialViewIndex(List<ScrollcasterGauntlet.ScrollSelectionView> views) {
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).currentSelection() && views.get(index).hasSpell()) {
                return index;
            }
        }
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).hasSpell()) {
                return index;
            }
        }
        return 0;
    }

    private static int indexOfScroll(List<ScrollcasterGauntlet.ScrollSelectionView> views, int scrollIndex) {
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).scrollIndex() == scrollIndex && views.get(index).hasSpell()) {
                return index;
            }
        }
        return -1;
    }

    private static void renderSelectionHud(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight,
                                           ActiveSelectionState state) {
        var views = state.views();
        var rowWidth = computeRowWidth(views.size());
        var startX = screenWidth / 2 - rowWidth / 2;
        var crosshairY = screenHeight / 2;
        var hasSelectableView = state.selectableViewCount() > 0;
        var label = hasSelectableView
                ? state.selectedView().displayName()
                : Component.translatable(EMPTY_SELECTION_LABEL_KEY).withStyle(ChatFormatting.RED);
        var labelX = screenWidth / 2 - font.width(label) / 2;
        var labelY = crosshairY + LABEL_OFFSET_FROM_CROSSHAIR;
        var rowY = labelY + SLOT_OFFSET_FROM_LABEL;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        guiGraphics.drawString(font, label, labelX, labelY, LABEL_COLOR, true);

        for (var column = 0; column < views.size(); ++column) {
            renderSlot(guiGraphics, startX, rowY, column, views.size(), views.get(column));
        }

        if (hasSelectableView) {
            var selectedColumn = state.selectedViewIndex();
            var selectedContent = resolveContentPosition(startX, rowY, selectedColumn, views.size());
            renderSelectedFrame(guiGraphics, selectedContent.x() - 4, selectedContent.y() - 4);
        }
        guiGraphics.pose().popPose();
    }

    private static int computeRowWidth(int slotCount) {
        return slotCount <= 0 ? 0 : SLOT_WIDTH + (slotCount - 1) * SLOT_STEP;
    }

    private static void renderSlot(GuiGraphics guiGraphics, int rowX, int rowY, int column, int slotCount,
                                   ScrollcasterGauntlet.ScrollSelectionView view) {
        var slotX = rowX + column * SLOT_STEP;
        var frameU = resolveFrameU(column, slotCount);
        guiGraphics.blit(TEXTURE, slotX, rowY, frameU, FRAME_V, SLOT_WIDTH, SLOT_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        var content = resolveContentPosition(rowX, rowY, column, slotCount);
        if (view.spellIcon() != null) {
            guiGraphics.blit(view.spellIcon(), content.x(), content.y(), 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    private static int resolveFrameU(int column, int slotCount) {
        if (column == 0) {
            return LEFT_FRAME_U;
        }
        if (column == slotCount - 1) {
            return RIGHT_FRAME_U;
        }
        return MIDDLE_FRAME_U;
    }

    private static ContentPosition resolveContentPosition(int rowX, int rowY, int column, int slotCount) {
        var slotX = rowX + column * SLOT_STEP;
        if (column == 0) {
            return new ContentPosition(slotX + 3, rowY + 3);
        }
        if (column < slotCount - 1) {
            return new ContentPosition(slotX + 3, rowY + 3);
        }
        return new ContentPosition(slotX + 2, rowY + 3);
    }

    private static void renderSelectedFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(
                TEXTURE,
                x,
                y,
                SELECTED_FRAME_U,
                SELECTED_FRAME_V,
                SELECTED_FRAME_SIZE,
                SELECTED_FRAME_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }

    private static void clearState() {
        activeState = null;
    }

    private record ActiveSelectionState(
            InteractionHand hand,
            List<ScrollcasterGauntlet.ScrollSelectionView> views,
            int selectedViewIndex
    ) {
        private ActiveSelectionState {
            views = List.copyOf(views);
        }

        private ScrollcasterGauntlet.ScrollSelectionView selectedView() {
            return views.get(selectedViewIndex);
        }

        private int selectableViewCount() {
            var count = 0;
            for (var view : views) {
                if (view.hasSpell()) {
                    ++count;
                }
            }
            return count;
        }
    }

    private record ContentPosition(int x, int y) {
    }
}
