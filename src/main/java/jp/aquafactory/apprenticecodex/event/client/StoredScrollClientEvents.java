package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.StoredScrollCastingEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class StoredScrollClientEvents {
    private static final ItemStack[] SNAPSHOTS = {ItemStack.EMPTY, ItemStack.EMPTY};

    private StoredScrollClientEvents() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = Minecraft.getInstance().player;
        boolean changed = false;
        for (var hand : InteractionHand.values()) {
            var held = player == null ? ItemStack.EMPTY : StoredScrollCastingEvents.resolveHeld(player, hand);
            var next = StoredScrollCastingEvents.isTarget(held) ? held.copy() : ItemStack.EMPTY;
            int index = hand.ordinal();
            changed |= !ItemStack.isSameItemSameTags(SNAPSHOTS[index], next);
            SNAPSHOTS[index] = next;
        }
        // 通知とインベントリ同期の到着順に依存せず、エンチャント変更も反映する。
        if (changed && player != null) ClientMagicData.updateSpellSelectionManager();
    }
}
