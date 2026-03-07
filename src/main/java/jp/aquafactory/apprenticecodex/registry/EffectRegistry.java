package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.ArcaneCharge;
import jp.aquafactory.apprenticecodex.effect.PaletteReception;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.effect.SenseSensor;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ApprenticeCodex.MODID);

    public static final RegistryObject<MobEffect> ARCANE_CHARGE =
            EFFECTS.register("arcane_charge", ArcaneCharge::new);
    public static final RegistryObject<MobEffect> PALETTE_RECEPTION =
            EFFECTS.register("palette_reception", PaletteReception::new);
    public static final RegistryObject<MobEffect> PHALANX_STANCE =
            EFFECTS.register("phalanx_stance", PhalanxStance::new);
    public static final RegistryObject<MobEffect> SENSE_SENSOR =
            EFFECTS.register("sense_sensor", SenseSensor::new);
    public static final RegistryObject<MobEffect> THERMAL_PROCESSING =
            EFFECTS.register("thermal_processing", ThermalProcessing::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
