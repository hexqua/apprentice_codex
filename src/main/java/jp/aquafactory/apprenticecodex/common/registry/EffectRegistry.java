package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.effect.ArcaneCharge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, ApprenticeCodex.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_CHARGE =
            EFFECTS.register("arcane_charge", ArcaneCharge::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
