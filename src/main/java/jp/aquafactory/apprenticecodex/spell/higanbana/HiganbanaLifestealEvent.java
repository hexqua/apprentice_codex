package jp.aquafactory.apprenticecodex.spell.higanbana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class HiganbanaLifestealEvent {
    private HiganbanaLifestealEvent() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!event.getSource().is(DamageTypes.HIGANBANA)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        var dealtDamage = event.getNewDamage();
        if (dealtDamage <= 0f) {
            return;
        }

        attacker.heal(dealtDamage * 0.5f);
    }
}

