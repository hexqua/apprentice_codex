package jp.aquafactory.apprenticecodex.spell.thermalslice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ThermalSunderedEvents {
    private ThermalSunderedEvents() {
    }

    // Forge の最終ダメージイベントで、先行ハンドラーによる無効化を反映する。
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide
                || event.getAmount() <= 0.0F
                || !event.getSource().is(DamageTypes.ON_FIRE)) {
            return;
        }

        ThermalSunderedLogic.extendFromSuccessfulFireDamage(event.getEntity());
    }
}
