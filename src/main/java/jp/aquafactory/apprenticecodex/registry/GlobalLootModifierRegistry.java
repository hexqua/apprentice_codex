package jp.aquafactory.apprenticecodex.registry;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.AddIsekaiTravelGuidebookToBonusChestModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GlobalLootModifierRegistry {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ApprenticeCodex.MODID);

    static {
        GLOBAL_LOOT_MODIFIER_CODECS.register("add_isekai_travel_guidebook_to_bonus_chest",
                () -> AddIsekaiTravelGuidebookToBonusChestModifier.CODEC);
    }

    private GlobalLootModifierRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        GLOBAL_LOOT_MODIFIER_CODECS.register(modEventBus);
    }
}
