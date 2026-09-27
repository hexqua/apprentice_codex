package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.StoredScrollCastingEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class StoredScrollClientEvents {
    private static final ItemStack[] SNAPSHOTS = {ItemStack.EMPTY, ItemStack.EMPTY};

    private StoredScrollClientEvents() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        boolean changed = false;
        for (var hand : InteractionHand.values()) {
            // 追加された手は選択同期の対象外にし、両手分のスナップショットだけを参照する。
            if (hand != InteractionHand.MAIN_HAND && hand != InteractionHand.OFF_HAND) continue;
            var held = player == null ? ItemStack.EMPTY : StoredScrollCastingEvents.resolveHeld(player, hand);
            var next = StoredScrollCastingEvents.isTarget(held) ? held.copy() : ItemStack.EMPTY;
            int index = hand == InteractionHand.MAIN_HAND ? 0 : 1;
            changed |= !ItemStack.isSameItemSameComponents(SNAPSHOTS[index], next);
            SNAPSHOTS[index] = next;
        }
        // 通知とインベントリ同期の到着順に依存せず、エンチャント変更も反映する。
        if (changed && player != null) ClientMagicData.updateSpellSelectionManager();
    }
}
