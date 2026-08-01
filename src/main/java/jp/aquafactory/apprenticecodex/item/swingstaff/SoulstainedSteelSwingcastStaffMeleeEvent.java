package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SoulstainedSteelSwingcastStaffMeleeEvent {
    private SoulstainedSteelSwingcastStaffMeleeEvent() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || event.getSource().getDirectEntity() != attacker
                || !(attacker.getMainHandItem().getItem() instanceof SoulstainedSteelSwingcastStaff)) {
            return;
        }

        var source = event.getSource();
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && !"player".equals(source.getMsgId())) {
            return;
        }

        MalumMnemonicBladeBridge.playMeleeHitEffect(attacker, event.getEntity());
    }
}
