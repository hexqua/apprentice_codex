package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.ApprenticeCurioLootChanceCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class LootConditionRegistry {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, ApprenticeCodex.MODID);

    public static final RegistryObject<LootItemConditionType> APPRENTICE_CURIO_LOOT_CHANCE =
            LOOT_CONDITION_TYPES.register(
                    "apprentice_curio_loot_chance",
                    () -> new LootItemConditionType(new ApprenticeCurioLootChanceCondition.Serializer())
            );

    private LootConditionRegistry() {
    }

    public static void register(IEventBus bus) {
        LOOT_CONDITION_TYPES.register(bus);
    }
}
