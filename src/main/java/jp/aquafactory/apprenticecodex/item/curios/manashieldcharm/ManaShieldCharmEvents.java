package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearDamageLogic;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaShieldCharmEvents {
    private ManaShieldCharmEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            if (ManaDefenseImmunityResolver.cancelIfImmune(event, player)) {
                return;
            }
            // 同一イベント内でGearを先に処理し、残ったダメージだけをMana Shieldへ渡す。
            ManaManeuverGearDamageLogic.reduceFallDamage(event, player);
            if (!event.isCanceled() && event.getAmount() > 0.0F) {
                ManaShieldCharmLogic.onLivingAttack(event, player);
            }
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
