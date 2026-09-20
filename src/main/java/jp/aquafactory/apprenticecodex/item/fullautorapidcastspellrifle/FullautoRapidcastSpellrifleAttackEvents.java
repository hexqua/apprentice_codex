package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FullautoRapidcastSpellrifleAttackEvents {
    private FullautoRapidcastSpellrifleAttackEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle) {
            event.setCanceled(true);
        }
    }
}
