package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
