package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientOpenStorageStabilizerEnderChestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class StorageStabilizerClientEvents {
    private StorageStabilizerClientEvents() {
    }

    @SubscribeEvent
    public static void onCreativeInventoryRightClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)
                || event.getButton() != 1
                || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        var slot = screen.getSlotUnderMouse();
        if (player == null
                || slot == null
                || slot.container != player.getInventory()
                || !(slot.getItem().getItem() instanceof StorageStabilizer)) {
            return;
        }

        // Creative画面はplayer inventoryのクリックをclient内で完結させるため、server側で再検証して開く。
        Networks.sendToServer(new ClientOpenStorageStabilizerEnderChestPacket(slot.getSlotIndex()));
        event.setCanceled(true);
    }
}
