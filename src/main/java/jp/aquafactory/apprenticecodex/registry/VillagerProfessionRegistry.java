package jp.aquafactory.apprenticecodex.registry;

import com.google.common.collect.ImmutableSet;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VillagerProfessionRegistry {
    public static final ResourceKey<VillagerProfession> ERRAND_MAGE_KEY = ResourceKey.create(
            Registries.VILLAGER_PROFESSION,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "errand_mage")
    );

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, ApprenticeCodex.MODID);

    public static final DeferredHolder<VillagerProfession, VillagerProfession> ERRAND_MAGE =
            VILLAGER_PROFESSIONS.register("errand_mage", () -> new VillagerProfession(
                    "errand_mage",
                    // Holder の key 判定にしておくと、将来の loader 差分があっても就職条件の意味が崩れにくい。
                    poiType -> poiType.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    poiType -> poiType.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundRegistry.VANILLA_USE_DESK.get()
            ));

    private VillagerProfessionRegistry() {
    }

    public static void register(IEventBus eventBus) {
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
