package jp.aquafactory.apprenticecodex.spell.thermalslice;

import jp.aquafactory.apprenticecodex.effect.ThermalSundered;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class ThermalSunderedLogic {
    private ThermalSunderedLogic() {
    }

    public static void applyFromThermalSlice(LivingEntity target, int amplifier) {
        if (target.hasEffect(MobEffects.FIRE_RESISTANCE) || target.fireImmune()) {
            return;
        }

        updateEffect(target, ThermalSundered.INITIAL_DURATION_TICKS, amplifier);
    }

    public static void extendFromOnFireDamage(LivingEntity target) {
        var effect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.THERMAL_SUNDERED.get());
        var current = target.getEffect(effect);
        if (current == null) {
            return;
        }

        updateEffect(target, ThermalSundered.ON_FIRE_EXTENDED_DURATION_TICKS, current.getAmplifier());
    }

    private static void updateEffect(LivingEntity target, int minimumDuration, int requestedAmplifier) {
        var effect = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.THERMAL_SUNDERED.get());
        var current = target.getEffect(effect);
        var amplifier = ThermalSundered.clampAmplifier(requestedAmplifier);
        var duration = minimumDuration;
        if (current != null) {
            amplifier = Math.max(amplifier, ThermalSundered.clampAmplifier(current.getAmplifier()));
            duration = Math.max(duration, current.getDuration());
        }

        target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }
}
