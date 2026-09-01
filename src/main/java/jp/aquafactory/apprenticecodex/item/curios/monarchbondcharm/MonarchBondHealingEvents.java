package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MonarchBondHealingEvents {
    private MonarchBondHealingEvents() {
    }

    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        if (!(event.getTargetEntity() instanceof ServerPlayer wearer)
                || !MonarchBondCharm.isEquippedBy(wearer)) {
            return;
        }

        MonarchBondHealing.distributeOverflow(wearer, event.getHealAmount());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellLifesteal(LivingDamageEvent.Post event) {
        if (!(event.getSource() instanceof SpellDamageSource spellDamageSource)
                || spellDamageSource.getLifestealPercent() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer wearer)
                || !MonarchBondCharm.isEquippedBy(wearer)) {
            return;
        }

        // Iron's本体は通常優先度でこの直後に回復するため、回復前の体力を使える最高優先度で余剰だけ予約する。
        MonarchBondHealing.distributeOverflow(
                wearer,
                spellDamageSource.getLifestealPercent() * event.getNewDamage()
        );
    }

    @SubscribeEvent
    public static void onGreaterHealCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer wearer)
                || !io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get().getSpellId()
                .equals(event.getSpellId())
                || !MonarchBondCharm.isEquippedBy(wearer)) {
            return;
        }

        MonarchBondHealing.healAll(wearer);
    }
}
