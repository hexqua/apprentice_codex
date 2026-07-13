package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ParrycastBucklerAttackEvent {
    private ParrycastBucklerAttackEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled() || event.getAmount() <= 0.0F
                || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof ParrycastBuckler)) {
            return;
        }
        // 完全ブロックは ShieldBlockEvent 側で判定する。ここへ到達した攻撃は構えを貫通したため解除する。
        player.stopUsingItem();
    }
}
