package jp.aquafactory.apprenticecodex.registry;

import com.mojang.serialization.MapCodec;
import io.redspace.ironsjewelry.core.actions.IAction;
import io.redspace.ironsjewelry.registry.IronsJewelryRegistries;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.gems.AdvanceSpellCooldownsAction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GemsActionRegistry {
    private static final DeferredRegister<MapCodec<? extends IAction>> ACTIONS =
            DeferredRegister.create(IronsJewelryRegistries.ACTION_REGISTRY, ApprenticeCodex.MODID);

    static {
        ACTIONS.register("advance_spell_cooldowns", () -> AdvanceSpellCooldownsAction.CODEC);
    }

    private GemsActionRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        ACTIONS.register(modEventBus);
    }
}
