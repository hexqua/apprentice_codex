package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.ArcaneCharge;
import jp.aquafactory.apprenticecodex.effect.PaletteReception;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, ApprenticeCodex.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_CHARGE =
            EFFECTS.register("arcane_charge", ArcaneCharge::new);
    public static final DeferredHolder<MobEffect, MobEffect> PALETTE_RECEPTION =
            EFFECTS.register("palette_reception", PaletteReception::new);
    public static final DeferredHolder<MobEffect, MobEffect> PHALANX_STANCE =
            EFFECTS.register("phalanx_stance", PhalanxStance::new);
    public static final DeferredHolder<MobEffect, MobEffect> THERMAL_PROCESSING =
            EFFECTS.register("thermal_processing", ThermalProcessing::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}

