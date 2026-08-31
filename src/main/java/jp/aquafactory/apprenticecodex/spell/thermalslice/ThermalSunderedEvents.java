package jp.aquafactory.apprenticecodex.spell.thermalslice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ThermalSunderedEvents {
    private ThermalSunderedEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide
                || event.getNewDamage() <= 0.0F
                || !event.getSource().is(DamageTypes.ON_FIRE)) {
            return;
        }

        ThermalSunderedLogic.extendFromOnFireDamage(event.getEntity());
    }
}
