package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleElementalDash;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

final class ManaDefenseImmunityResolver {
    private ManaDefenseImmunityResolver() {
    }

    static boolean cancelIfImmune(LivingIncomingDamageEvent event, ServerPlayer player) {
        // マナなどを消費する防御より先に、このMODが最終的に無効化するダメージを一箇所で解決する.
        return ManaShieldCharmLogic.cancelDuringVanillaStyleIFrame(event, player)
                || MirageAvoidanceEvents.cancelIncomingDamageIfInvulnerable(event)
                || MantleBlink.cancelIncomingDamageIfInvulnerable(event)
                || QuickBlinkRuntime.cancelIncomingDamageIfInvulnerable(event)
                || MantleElementalDash.cancelIncomingDamageIfInvulnerable(event);
    }
}
