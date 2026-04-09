package jp.aquafactory.apprenticecodex.worldgen;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ErrandMageVillageAddition {
    public static final int HOUSE_WEIGHT = 3;

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
            ApprenticeCodex.LOGGER.info("Errand Mage village house injection is disabled because HOUSE_WEIGHT <= 0");
            return;
        }

        var registryAccess = event.getServer().registryAccess();
        var templatePoolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
        var processorListRegistry = registryAccess.registryOrThrow(Registries.PROCESSOR_LIST);
        ApprenticeCodex.LOGGER.info("Injecting Errand Mage village houses with weight {} into {} village pools", HOUSE_WEIGHT, HOUSE_ADDITIONS.length);

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
        var beforeRawTemplates = accessor.apprenticecodex$getRawTemplates();
        int beforeExpandedTemplateCount = accessor.apprenticecodex$getTemplates().size();
        int beforeTotalWeight = getTotalWeight(beforeRawTemplates);
        int beforeMatchingEntryCount = countMatchingEntries(beforeRawTemplates, addition.structureId(), addition.processorListKey().location());

        for (int i = 0; i < weight; i++) {
            accessor.apprenticecodex$getTemplates().add(piece);
        }

        var rawTemplates = new ArrayList<>(beforeRawTemplates);
        rawTemplates.add(Pair.of((StructurePoolElement) piece, weight));
        accessor.apprenticecodex$setRawTemplates(rawTemplates);

        int afterTotalWeight = getTotalWeight(rawTemplates);
        int afterMatchingEntryCount = countMatchingEntries(rawTemplates, addition.structureId(), addition.processorListKey().location());
        ApprenticeCodex.LOGGER.info(
                "Errand Mage village house injected into pool {}: structure={} processor={} rawEntries {}->{} totalWeight {}->{} matchingEntries {}->{} expandedTemplates {}->{}",
                addition.poolId(),
                addition.structureId(),
                addition.processorListKey().location(),
                beforeRawTemplates.size(),
                rawTemplates.size(),
                beforeTotalWeight,
                afterTotalWeight,
                beforeMatchingEntryCount,
                afterMatchingEntryCount,
                beforeExpandedTemplateCount,
                accessor.apprenticecodex$getTemplates().size()
        );
    }

    private static int getTotalWeight(Iterable<Pair<StructurePoolElement, Integer>> rawTemplates) {
        int totalWeight = 0;
        for (var rawTemplate : rawTemplates) {
            totalWeight += rawTemplate.getSecond();
        }
        return totalWeight;
    }

    private static int countMatchingEntries(
            Iterable<Pair<StructurePoolElement, Integer>> rawTemplates,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId
    ) {
        int matchingEntryCount = 0;
        for (var rawTemplate : rawTemplates) {
            if (isMatchingSinglePoolElement(rawTemplate.getFirst(), expectedStructureId, expectedProcessorId)) {
                matchingEntryCount++;
            }
        }
        return matchingEntryCount;
    }

    private static boolean isMatchingSinglePoolElement(
            StructurePoolElement element,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId
    ) {
        if (!(element instanceof SinglePoolElement singlePoolElement)) {
            return false;
        }

        var accessor = (SinglePoolElementAccessor) singlePoolElement;
        var structureId = accessor.apprenticecodex$getTemplate().left().orElse(null);
        var processorId = accessor.apprenticecodex$getProcessors().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return expectedStructureId.equals(structureId) && expectedProcessorId.equals(processorId);
    }

    private record VillageHouseAddition(
            ResourceLocation poolId,
            ResourceLocation structureId,
            ResourceKey<StructureProcessorList> processorListKey
    ) {
    }
}
