package jp.aquafactory.apprenticecodex.worldgen;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ErrandMageVillageAddition {
    public static final int HOUSE_WEIGHT = 1;

    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(
            Registries.PROCESSOR_LIST,
            ResourceLocation.withDefaultNamespace("empty")
    );
    private static final ResourceKey<StructureProcessorList> MOSSIFY_10_PERCENT_PROCESSOR_LIST_KEY = ResourceKey.create(
            Registries.PROCESSOR_LIST,
            ResourceLocation.withDefaultNamespace("mossify_10_percent")
    );

    private static final VillageHouseAddition[] HOUSE_ADDITIONS = new VillageHouseAddition[]{
            new VillageHouseAddition(
                    ResourceLocation.withDefaultNamespace("village/plains/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    MOSSIFY_10_PERCENT_PROCESSOR_LIST_KEY
            ),
            new VillageHouseAddition(
                    ResourceLocation.withDefaultNamespace("village/desert/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    EMPTY_PROCESSOR_LIST_KEY
            ),
            new VillageHouseAddition(
                    ResourceLocation.withDefaultNamespace("village/savanna/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    EMPTY_PROCESSOR_LIST_KEY
            ),
            // 雪原とタイガは専用 NBT を増やさず、plain 家屋の見た目をそのまま使い回す。
            new VillageHouseAddition(
                    ResourceLocation.withDefaultNamespace("village/snowy/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    EMPTY_PROCESSOR_LIST_KEY
            ),
            new VillageHouseAddition(
                    ResourceLocation.withDefaultNamespace("village/taiga/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    MOSSIFY_10_PERCENT_PROCESSOR_LIST_KEY
            )
    };

    private ErrandMageVillageAddition() {
    }

    @SubscribeEvent
    public static void addErrandMageVillageHouse(final ServerAboutToStartEvent event) {
        if (HOUSE_WEIGHT <= 0) {
            return;
        }

        var registryAccess = event.getServer().registryAccess();
        var templatePoolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
        var processorListRegistry = registryAccess.registryOrThrow(Registries.PROCESSOR_LIST);

        for (var addition : HOUSE_ADDITIONS) {
            addBuildingToPool(templatePoolRegistry, processorListRegistry, addition, HOUSE_WEIGHT);
        }
    }

    private static void addBuildingToPool(
            Registry<StructureTemplatePool> templatePoolRegistry,
            Registry<StructureProcessorList> processorListRegistry,
            VillageHouseAddition addition,
            int weight
    ) {
        // 村 pool は他 mod も触りやすいため、JSON を丸ごと上書きせず起動時に不足分だけ差し込む。
        var pool = templatePoolRegistry.get(addition.poolId());
        if (pool == null) {
            ApprenticeCodex.LOGGER.warn("Errand Mage village house target pool is missing: {}", addition.poolId());
            return;
        }

        // Processor は static 定数を直接使うと world 起動後の holder identity と食い違うため、動的 registry から取り直す。
        Holder<StructureProcessorList> processors = processorListRegistry.getHolderOrThrow(addition.processorListKey());
        var piece = SinglePoolElement.legacy(addition.structureId().toString(), processors)
                .apply(StructureTemplatePool.Projection.RIGID);

        var accessor = (StructureTemplatePoolAccessor) pool;
        for (int i = 0; i < weight; i++) {
            accessor.apprenticecodex$getTemplates().add(piece);
        }

        var rawTemplates = new ArrayList<>(accessor.apprenticecodex$getRawTemplates());
        rawTemplates.add(Pair.of((StructurePoolElement) piece, weight));
        accessor.apprenticecodex$setRawTemplates(rawTemplates);
    }

    private record VillageHouseAddition(
            ResourceLocation poolId,
            ResourceLocation structureId,
            ResourceKey<StructureProcessorList> processorListKey
    ) {
    }
}
