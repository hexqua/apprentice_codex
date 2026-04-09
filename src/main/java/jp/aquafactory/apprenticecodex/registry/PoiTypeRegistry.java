package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class PoiTypeRegistry {
    public static final ResourceKey<PoiType> APPRENTICE_DESK_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_desk")
    );

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ApprenticeCodex.MODID);

    public static final DeferredHolder<PoiType, PoiType> APPRENTICE_DESK =
            POI_TYPES.register("apprentice_desk", () -> new PoiType(apprenticeDeskStates(), 1, 1));

    private PoiTypeRegistry() {
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }

    private static Set<net.minecraft.world.level.block.state.BlockState> apprenticeDeskStates() {
        return Set.copyOf(BlockRegistry.APPRENTICE_DESK.get().getStateDefinition().getPossibleStates());
    }
}
