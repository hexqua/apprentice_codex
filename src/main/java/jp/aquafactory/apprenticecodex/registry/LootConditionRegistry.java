package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.ApprenticeCurioLootChanceCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LootConditionRegistry {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, ApprenticeCodex.MODID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> APPRENTICE_CURIO_LOOT_CHANCE =
            LOOT_CONDITION_TYPES.register(
                    "apprentice_curio_loot_chance",
                    () -> new LootItemConditionType(ApprenticeCurioLootChanceCondition.CODEC)
            );

    private LootConditionRegistry() {
    }

    public static void register(IEventBus bus) {
        LOOT_CONDITION_TYPES.register(bus);
    }
}
