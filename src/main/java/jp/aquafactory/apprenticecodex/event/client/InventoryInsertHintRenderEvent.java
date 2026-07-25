package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.InventoryInsertTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class InventoryInsertHintRenderEvent {
    private static final int ITEM_INSERT_COLOR = 0xFFFF55;
    private static final int MANA_INSERT_COLOR = 0x55FFFF;

    private InventoryInsertHintRenderEvent() {
    }

    @SubscribeEvent
    public static void onContainerScreenForeground(ContainerScreenEvent.Render.Foreground event) {
        var minecraft = Minecraft.getInstance();
        var screen = event.getContainerScreen();
        if (isUnsupportedCreativeScreen(screen) || minecraft.player == null) {
            return;
        }

        var carriedStack = screen.getMenu().getCarried();
        if (carriedStack.isEmpty()) {
            return;
        }

        for (var slot : screen.getMenu().slots) {
            var hint = resolveHint(slot, carriedStack, minecraft.player);
            if (hint == InventoryInsertTarget.InsertHint.NONE) {
                continue;
            }

            // Foreground はスロット相対座標なので、Forge 1.20.1 側では GUI 原点の加算を行わない。
            var poseStack = event.getGuiGraphics().pose();
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 300.0F);
            event.getGuiGraphics().drawString(
                    minecraft.font,
                    "+",
                    slot.x + 10,
                    slot.y + 1,
                    colorFor(hint),
                    true
            );
            poseStack.popPose();
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || isUnsupportedCreativeScreen(screen)
                || minecraft.player == null) {
            return;
        }

        var carriedStack = screen.getMenu().getCarried();
        var hoveredSlot = screen.getSlotUnderMouse();
        if (carriedStack.isEmpty() || hoveredSlot == null) {
            return;
        }

        var hint = resolveHint(hoveredSlot, carriedStack, minecraft.player);
        if (hint == InventoryInsertTarget.InsertHint.NONE) {
            return;
        }

        var translationKey = hint == InventoryInsertTarget.InsertHint.MANA
                ? "ui.apprenticecodex.inventory_insert.refill_mana"
                : "ui.apprenticecodex.inventory_insert.store";
        var formatting = hint == InventoryInsertTarget.InsertHint.MANA
                ? ChatFormatting.AQUA
                : ChatFormatting.YELLOW;
        event.getGuiGraphics().renderComponentTooltip(
                minecraft.font,
                List.of(Component.translatable(translationKey).withStyle(formatting)),
                event.getMouseX(),
                event.getMouseY()
        );
    }

    private static InventoryInsertTarget.InsertHint resolveHint(
            Slot slot,
            ItemStack incomingStack,
            net.minecraft.world.entity.player.Player player
    ) {
        var storageStack = slot.getItem();
        if (!(storageStack.getItem() instanceof InventoryInsertTarget target)
                || !InventoryInsertTarget.canModifyStorageSlot(storageStack, slot, player)) {
            return InventoryInsertTarget.InsertHint.NONE;
        }
        return target.getInventoryInsertHint(storageStack, incomingStack, player);
    }

    private static boolean isUnsupportedCreativeScreen(AbstractContainerScreen<?> screen) {
        // アイテム一覧は複製用の仮スロットだが、インベントリタブは実際の player.inventoryMenu を操作する。
        return screen instanceof CreativeModeInventoryScreen creativeScreen
                && !creativeScreen.isInventoryOpen();
    }

    private static int colorFor(InventoryInsertTarget.InsertHint hint) {
        return hint == InventoryInsertTarget.InsertHint.MANA
                ? MANA_INSERT_COLOR
                : ITEM_INSERT_COLOR;
    }
}
