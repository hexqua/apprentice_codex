package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.ArcaneCharge;
import jp.aquafactory.apprenticecodex.effect.CastingMobility;
import jp.aquafactory.apprenticecodex.effect.CraftsmansDelightMobility;
import jp.aquafactory.apprenticecodex.effect.Intelligence;
import jp.aquafactory.apprenticecodex.effect.LongStrideMobility;
import jp.aquafactory.apprenticecodex.effect.ManaRegeneration;
import jp.aquafactory.apprenticecodex.effect.PaletteReception;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.effect.SenseSensor;
import jp.aquafactory.apprenticecodex.effect.SpectralWingEffect;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ApprenticeCodex.MODID);

    public static final RegistryObject<MobEffect> ARCANE_CHARGE =
            EFFECTS.register("arcane_charge", ArcaneCharge::new);
    public static final RegistryObject<MobEffect> CASTING_MOBILITY =
            EFFECTS.register("casting_mobility", CastingMobility::new);
    public static final RegistryObject<MobEffect> CRAFTSMANS_DELIGHT_MOBILITY =
            EFFECTS.register("craftsmans_delight_mobility", CraftsmansDelightMobility::new);
    public static final RegistryObject<MobEffect> INTELLIGENCE =
            EFFECTS.register("intelligence", Intelligence::new);
    public static final RegistryObject<MobEffect> LONG_STRIDE_MOBILITY =
            EFFECTS.register("long_stride_mobility", LongStrideMobility::new);
    public static final RegistryObject<MobEffect> MANA_REGENERATION =
            EFFECTS.register("mana_regeneration", ManaRegeneration::new);
    public static final RegistryObject<MobEffect> PALETTE_RECEPTION =
            EFFECTS.register("palette_reception", PaletteReception::new);
    public static final RegistryObject<MobEffect> PHALANX_STANCE =
            EFFECTS.register("phalanx_stance", PhalanxStance::new);
    public static final RegistryObject<MobEffect> SENSE_SENSOR =
            EFFECTS.register("sense_sensor", SenseSensor::new);
    public static final RegistryObject<MobEffect> SPECTRAL_WING =
            EFFECTS.register("spectral_wing", SpectralWingEffect::new);
    public static final RegistryObject<MobEffect> THERMAL_PROCESSING =
            EFFECTS.register("thermal_processing", ThermalProcessing::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
        eventBus.addListener(EffectRegistry::registerDynamicEffects);
    }

    private static void registerDynamicEffects(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.MOB_EFFECTS, helper -> {
            for (var definition : SchoolAffinityRegistry.getDefinitions()) {
                helper.register(definition.effectId(), definition.effect());
            }
        });
    }
}
