package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.RandomSpellImbueLootFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class LootFunctionRegistry {
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTION_TYPES =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, ApprenticeCodex.MODID);

    public static final RegistryObject<LootItemFunctionType> RANDOM_SPELL_IMBUE = LOOT_FUNCTION_TYPES.register(
                    "random_spell_imbue",
                    () -> new LootItemFunctionType(new RandomSpellImbueLootFunction.Serializer())
            );

    private LootFunctionRegistry() {
    }

    public static void register(IEventBus bus) {
        LOOT_FUNCTION_TYPES.register(bus);
    }
}
