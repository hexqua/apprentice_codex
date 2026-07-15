package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ParrycastBucklerAttackEvent {
    private ParrycastBucklerAttackEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F
                || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof ParrycastBuckler)) {
            return;
        }
        // IncomingDamage はバニラの盾判定より前なので、盾判定後に実ダメージが残った場合だけ構えを解除する。
        player.stopUsingItem();
    }
}
