package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ParrycastBucklerAttackEvent {
    private ParrycastBucklerAttackEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled() || event.getAmount() <= 0.0F
                || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof ParrycastBuckler)) {
            return;
        }
        // 完全ブロックは ShieldBlockEvent 側で判定する。ここへ到達した攻撃は構えを貫通したため解除する。
        player.stopUsingItem();
    }
}
