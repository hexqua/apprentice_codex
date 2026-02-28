package jp.aquafactory.apprenticecodex.spell.higanbana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class HiganbanaLifestealEvent {
    private HiganbanaLifestealEvent() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!event.getSource().is(DamageTypes.HIGANBANA)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        var dealtDamage = event.getAmount();
        if (dealtDamage <= 0f) {
            return;
        }

        attacker.heal(dealtDamage);
    }
}
