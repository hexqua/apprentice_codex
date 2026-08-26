package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaShieldCharmEvents {
    private ManaShieldCharmEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            ManaShieldCharmLogic.onLivingAttack(event, player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            ManaShieldCharmLogic.onDeath(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCounterSpell(CounterSpellEvent event) {
        if (event.target instanceof ServerPlayer player && !player.level().isClientSide) {
            ManaShieldCharmLogic.onCounterSpell(event, player);
        }
    }
}
