package jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AbsorptionAmplifyAmuletEvents {
    private AbsorptionAmplifyAmuletEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
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
