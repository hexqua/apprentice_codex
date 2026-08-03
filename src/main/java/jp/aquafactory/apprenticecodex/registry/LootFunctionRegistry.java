package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.RandomSpellImbueLootFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LootFunctionRegistry {
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTION_TYPES =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, ApprenticeCodex.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RandomSpellImbueLootFunction>>
            RANDOM_SPELL_IMBUE = LOOT_FUNCTION_TYPES.register(
                    "random_spell_imbue",
                    () -> new LootItemFunctionType<>(RandomSpellImbueLootFunction.CODEC)
            );

    private LootFunctionRegistry() {
    }

    public static void register(IEventBus bus) {
        LOOT_FUNCTION_TYPES.register(bus);
    }
}
