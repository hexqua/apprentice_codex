package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.ArcaneCharge;
import jp.aquafactory.apprenticecodex.effect.Intelligence;
import jp.aquafactory.apprenticecodex.effect.LongStrideMobility;
import jp.aquafactory.apprenticecodex.effect.PaletteReception;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.effect.SenseSensor;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ApprenticeCodex.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_CHARGE =
            EFFECTS.register("arcane_charge", ArcaneCharge::new);
    public static final DeferredHolder<MobEffect, MobEffect> INTELLIGENCE =
            EFFECTS.register("intelligence", Intelligence::new);
    public static final DeferredHolder<MobEffect, MobEffect> LONG_STRIDE_MOBILITY =
            EFFECTS.register("long_stride_mobility", LongStrideMobility::new);
    public static final DeferredHolder<MobEffect, MobEffect> PALETTE_RECEPTION =
            EFFECTS.register("palette_reception", PaletteReception::new);
    public static final DeferredHolder<MobEffect, MobEffect> PHALANX_STANCE =
            EFFECTS.register("phalanx_stance", PhalanxStance::new);
    public static final DeferredHolder<MobEffect, MobEffect> SENSE_SENSOR =
            EFFECTS.register("sense_sensor", SenseSensor::new);
    public static final DeferredHolder<MobEffect, MobEffect> THERMAL_PROCESSING =
            EFFECTS.register("thermal_processing", ThermalProcessing::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
        eventBus.addListener(EffectRegistry::registerDynamicEffects);
    }

    private static void registerDynamicEffects(RegisterEvent event) {
        event.register(Registries.MOB_EFFECT, helper -> {
            for (var definition : SchoolAffinityRegistry.getDefinitions()) {
                helper.register(definition.effectId(), definition.effect());
            }
        });
    }
}

