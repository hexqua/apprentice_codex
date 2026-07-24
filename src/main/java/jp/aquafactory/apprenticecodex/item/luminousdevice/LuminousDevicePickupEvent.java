package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class LuminousDevicePickupEvent {
    private static final String OWNER_TAG = "Owner";

    private LuminousDevicePickupEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        var player = event.getEntity();
        var itemEntity = event.getItem();
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
        net.minecraftforge.event.ForgeEventFactory.firePlayerItemPickupEvent(player, itemEntity, pickedUpStack);
        player.take(itemEntity, pickedUpCount);
        if (entityStack.isEmpty()) {
            itemEntity.discard();
            entityStack.setCount(pickedUpCount);
        }
        player.awardStat(Stats.ITEM_PICKED_UP.get(pickedUpStack.getItem()), pickedUpCount);
        player.onItemPickup(itemEntity);
        event.setCanceled(true);
    }

    private static boolean canBePickedUpBy(ItemEntity itemEntity, Player player) {
        var itemEntityTag = itemEntity.saveWithoutId(new CompoundTag());
        return !itemEntityTag.hasUUID(OWNER_TAG) || itemEntityTag.getUUID(OWNER_TAG).equals(player.getUUID());
    }
}
