package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.ArcaneCharge;
import jp.aquafactory.apprenticecodex.effect.CastingMobility;
import jp.aquafactory.apprenticecodex.effect.CraftsmansDelightMobility;
import jp.aquafactory.apprenticecodex.effect.EchoSpell;
import jp.aquafactory.apprenticecodex.effect.FrostTrapped;
import jp.aquafactory.apprenticecodex.effect.Intelligence;
import jp.aquafactory.apprenticecodex.effect.LongStrideMobility;
import jp.aquafactory.apprenticecodex.effect.ManaRegeneration;
import jp.aquafactory.apprenticecodex.effect.MistFormEffect;
import jp.aquafactory.apprenticecodex.effect.PaletteReception;
import jp.aquafactory.apprenticecodex.effect.PenetratedArmor;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.effect.SenseSensor;
import jp.aquafactory.apprenticecodex.effect.SpectralWingEffect;
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
    public static final DeferredHolder<MobEffect, MobEffect> CASTING_MOBILITY =
            EFFECTS.register("casting_mobility", CastingMobility::new);
    public static final DeferredHolder<MobEffect, MobEffect> CRAFTSMANS_DELIGHT_MOBILITY =
            EFFECTS.register("craftsmans_delight_mobility", CraftsmansDelightMobility::new);
    public static final DeferredHolder<MobEffect, MobEffect> INTELLIGENCE =
            EFFECTS.register("intelligence", Intelligence::new);
    public static final DeferredHolder<MobEffect, MobEffect> LONG_STRIDE_MOBILITY =
            EFFECTS.register("long_stride_mobility", LongStrideMobility::new);
    public static final DeferredHolder<MobEffect, MobEffect> MANA_REGENERATION =
            EFFECTS.register("mana_regeneration", ManaRegeneration::new);
    public static final DeferredHolder<MobEffect, MobEffect> MIST_FORM =
            EFFECTS.register("mist_form", MistFormEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PALETTE_RECEPTION =
            EFFECTS.register("palette_reception", PaletteReception::new);
    public static final DeferredHolder<MobEffect, MobEffect> ECHO_SPELL =
            EFFECTS.register("echo_spell", EchoSpell::new);
    public static final DeferredHolder<MobEffect, MobEffect> PHALANX_STANCE =
            EFFECTS.register("phalanx_stance", PhalanxStance::new);
    public static final DeferredHolder<MobEffect, MobEffect> SENSE_SENSOR =
            EFFECTS.register("sense_sensor", SenseSensor::new);
    public static final DeferredHolder<MobEffect, MobEffect> SPECTRAL_WING =
            EFFECTS.register("spectral_wing", SpectralWingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> THERMAL_PROCESSING =
            EFFECTS.register("thermal_processing", ThermalProcessing::new);
    public static final DeferredHolder<MobEffect, MobEffect> PENETRATED_ARMOR =
            EFFECTS.register("penetrated_armor", PenetratedArmor::new);
    public static final DeferredHolder<MobEffect, MobEffect> FROST_TRAPPED =
            EFFECTS.register("frost_trapped", FrostTrapped::new);

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

