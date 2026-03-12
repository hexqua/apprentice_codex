package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

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
public final class SpellcasterAmmoPouchPickupEvent {
    private static final String OWNER_TAG = "Owner";

    private SpellcasterAmmoPouchPickupEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        var player = event.getEntity();
        var itemEntity = event.getItem();
        var entityStack = itemEntity.getItem();
        if (!SpellcasterAmmoPouch.isEquippedBy(player) || !SpellcasterAmmoPouch.accepts(entityStack)) {
            return;
        }

        if (!canBePickedUpBy(itemEntity, player)) {
            return;
        }

        var pickedUpStack = entityStack.copy();
        var pickedUpCount = SpellcasterAmmoPouch.storeInEquippedPouches(player, entityStack);
        if (pickedUpCount <= 0) {
            return;
        }

        // ポーチ収納後の残数も含めてこちらで拾得処理を完結させ、満杯時でも収納成功分を失わせない。
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
        // ItemEntity の所有者制限は公開 getter がないため、保存 NBT から Owner を読む。
        var itemEntityTag = itemEntity.saveWithoutId(new CompoundTag());
        return !itemEntityTag.hasUUID(OWNER_TAG) || itemEntityTag.getUUID(OWNER_TAG).equals(player.getUUID());
    }
}
