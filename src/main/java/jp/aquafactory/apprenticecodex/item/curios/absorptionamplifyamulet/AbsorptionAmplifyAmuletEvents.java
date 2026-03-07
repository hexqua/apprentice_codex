package jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AbsorptionAmplifyAmuletEvents {
    private AbsorptionAmplifyAmuletEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            AbsorptionAmplifyAmuletLogic.onPostDamage(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            AbsorptionAmplifyAmuletLogic.onDeath(player);
        }
    }
}
