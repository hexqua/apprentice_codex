package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
