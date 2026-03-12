package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellcasterAmmoPouchPickupEvent {
    private static final String OWNER_TAG = "Owner";

    private SpellcasterAmmoPouchPickupEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
        var player = event.getPlayer();
        var itemEntity = event.getItemEntity();
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

        // 装備ポーチぶんだけ先に差し引き、残りは通常の拾得処理へ流す。
        player.take(itemEntity, pickedUpCount);
        if (entityStack.isEmpty()) {
            itemEntity.discard();
            event.setCanPickup(TriState.FALSE);
        }
        player.awardStat(Stats.ITEM_PICKED_UP.get(pickedUpStack.getItem()), pickedUpCount);
    }

    private static boolean canBePickedUpBy(ItemEntity itemEntity, Player player) {
        // ItemEntity の所有者制限は公開 getter がないため、保存 NBT から Owner を読む。
        var itemEntityTag = itemEntity.saveWithoutId(new CompoundTag());
        return !itemEntityTag.hasUUID(OWNER_TAG) || itemEntityTag.getUUID(OWNER_TAG).equals(player.getUUID());
    }
}
