package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class LuminousDevicePickupEvent {
    private LuminousDevicePickupEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
        var player = event.getPlayer();
        var itemEntity = event.getItemEntity();
        var entityStack = itemEntity.getItem();
        if (!LuminousDevice.accepts(entityStack) || !canBePickedUpBy(itemEntity, player)) {
            return;
        }

        var pickedUpStack = entityStack.copy();
        var pickedUpCount = LuminousDevice.storePickedUpStackInInventoryDevices(player, entityStack);
        if (pickedUpCount <= 0) {
            return;
        }

        // デバイスへ入った分はここで拾得処理を完結させ、容量超過分は通常インベントリへ戻す。
        if (!entityStack.isEmpty()) {
            player.getInventory().add(entityStack);
        }

        pickedUpCount = pickedUpStack.getCount() - entityStack.getCount();
        if (pickedUpCount <= 0) {
            return;
        }

        pickedUpStack.setCount(pickedUpCount);
        EventHooks.fireItemPickupPost(itemEntity, player, pickedUpStack);
        player.take(itemEntity, pickedUpCount);
        if (entityStack.isEmpty()) {
            itemEntity.discard();
            entityStack.setCount(pickedUpCount);
        }
        player.awardStat(Stats.ITEM_PICKED_UP.get(pickedUpStack.getItem()), pickedUpCount);
        player.onItemPickup(itemEntity);
        event.setCanPickup(TriState.FALSE);
    }

    private static boolean canBePickedUpBy(ItemEntity itemEntity, Player player) {
        return itemEntity.getOwner() == null || itemEntity.getOwner().equals(player.getUUID());
    }
}
