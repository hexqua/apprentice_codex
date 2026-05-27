package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PhalanxCounterSpellEvent {
    private static final double FRONT_DOT_THRESHOLD = 0.25;

    private PhalanxCounterSpellEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCounterSpell(CounterSpellEvent event) {
        if (!(event.target instanceof ServerPlayer target)) {
            return;
        }

        var stance = target.getEffect(EffectRegistry.PHALANX_STANCE.get());
        if (stance == null || stance.getAmplifier() < PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER) {
            return;
        }

        if (!isFromFront(target, event.caster)) {
            return;
        }

        event.setCanceled(true);
        PhalanxGuardSuccessFlashEvent.triggerGuardSuccess(target);
    }

    private static boolean isFromFront(ServerPlayer target, Entity caster) {
        if (caster == null || caster == target) {
            return false;
        }

        var forward = horizontal(target.getLookAngle());
        var incoming = horizontal(caster.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter()));
        return isUsableDirection(forward)
                && isUsableDirection(incoming)
                && forward.normalize().dot(incoming.normalize()) >= FRONT_DOT_THRESHOLD;
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0, vector.z);
    }

    private static boolean isUsableDirection(Vec3 vector) {
        return vector.lengthSqr() > 1.0e-6;
    }
}
