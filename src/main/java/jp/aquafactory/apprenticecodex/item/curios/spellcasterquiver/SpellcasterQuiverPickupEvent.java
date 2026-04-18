package jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver;

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
public final class SpellcasterQuiverPickupEvent {
    private static final String OWNER_TAG = "Owner";

    private SpellcasterQuiverPickupEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        var player = event.getEntity();
        var itemEntity = event.getItem();
        var entityStack = itemEntity.getItem();
        if (!SpellcasterQuiver.isEquippedBy(player) || !SpellcasterQuiver.accepts(entityStack)) {
            return;
        }

        if (!canBePickedUpBy(itemEntity, player)) {
            return;
        }

        var pickedUpStack = entityStack.copy();
        var pickedUpCount = SpellcasterQuiver.storeInEquippedQuivers(player, entityStack);
        if (pickedUpCount <= 0) {
            return;
        }

        // 矢筒へ入った分はここで拾得処理を完結させ、満杯時も残数を欠損させない。
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
