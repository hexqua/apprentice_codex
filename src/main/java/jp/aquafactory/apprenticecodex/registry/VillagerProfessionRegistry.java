package jp.aquafactory.apprenticecodex.registry;

import com.google.common.collect.ImmutableSet;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VillagerProfessionRegistry {
    public static final ResourceKey<VillagerProfession> ERRAND_MAGE_KEY = ResourceKey.create(
            Registries.VILLAGER_PROFESSION,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "errand_mage")
    );

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, ApprenticeCodex.MODID);

    public static final RegistryObject<VillagerProfession> ERRAND_MAGE =
            VILLAGER_PROFESSIONS.register("errand_mage", () -> new VillagerProfession(
                    "errand_mage",
                    // Holder の key 判定にしておくと、Forge の POI holder 差し替えや将来の loader 差分を踏んでも意味が崩れにくい。
                    poiType -> poiType.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    poiType -> poiType.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.ENCHANTMENT_TABLE_USE
            ));

    private VillagerProfessionRegistry() {
    }

    public static void register(IEventBus eventBus) {
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
