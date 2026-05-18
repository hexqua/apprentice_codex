package jp.aquafactory.apprenticecodex.worldgen;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
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
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ErrandMageVillageAddition {
    private ErrandMageVillageAddition() {
    }

    @SubscribeEvent
    public static void addErrandMageVillageHouse(final ServerAboutToStartEvent event) {
        if (!ApprenticeCodexServerConfig.enableErrandMageVillageHouseInjection()) {
            ApprenticeCodex.LOGGER.info("Errand Mage village house injection is disabled by server config");
            return;
        }

        var additions = ErrandMageVillageHouseManager.definitions();
        if (additions.isEmpty()) {
            ApprenticeCodex.LOGGER.info("Errand Mage village house injection skipped because no definitions were loaded");
            return;
        }

        var registryAccess = event.getServer().registryAccess();
        var templatePoolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
        var processorListRegistry = registryAccess.registryOrThrow(Registries.PROCESSOR_LIST);
        ApprenticeCodex.LOGGER.info("Injecting {} Errand Mage village house definitions", additions.size());

        for (var addition : additions) {
            addBuildingToPool(templatePoolRegistry, processorListRegistry, addition);
        }
    }

    private static void addBuildingToPool(
            Registry<StructureTemplatePool> templatePoolRegistry,
            Registry<StructureProcessorList> processorListRegistry,
            ErrandMageVillageHouseDefinition addition
    ) {
        // 村 pool は他 mod も触りやすいため、JSON を丸ごと上書きせず起動時に不足分だけ差し込む。
        var pool = templatePoolRegistry.get(addition.pool());
        if (pool == null) {
            ApprenticeCodex.LOGGER.warn("Errand Mage village house target pool is missing: {}", addition.pool());
            return;
        }

        // Processor は static 定数を直接使うと world 起動後の holder identity と食い違うため、動的 registry から取り直す。
        var processorKey = ResourceKey.create(Registries.PROCESSOR_LIST, addition.processor());
        var processors = processorListRegistry.getHolder(processorKey);
        if (processors.isEmpty()) {
            ApprenticeCodex.LOGGER.warn("Errand Mage village house processor list is missing: {}", addition.processor());
            return;
        }

        Holder<StructureProcessorList> processorHolder = processors.get();
        var piece = SinglePoolElement.legacy(addition.structure().toString(), processorHolder)
                .apply(StructureTemplatePool.Projection.RIGID);

        var accessor = (StructureTemplatePoolAccessor) pool;
        var beforeRawTemplates = accessor.apprenticecodex$getRawTemplates();
        int beforeExpandedTemplateCount = accessor.apprenticecodex$getTemplates().size();
        int beforeTotalWeight = getTotalWeight(beforeRawTemplates);
        int beforeMatchingEntryCount = countMatchingEntries(beforeRawTemplates, addition.structure(), addition.processor());

        for (int i = 0; i < addition.weight(); i++) {
            accessor.apprenticecodex$getTemplates().add(piece);
        }

        var rawTemplates = new ArrayList<>(beforeRawTemplates);
        rawTemplates.add(Pair.of(piece, addition.weight()));
        accessor.apprenticecodex$setRawTemplates(rawTemplates);

        int afterTotalWeight = getTotalWeight(rawTemplates);
        int afterMatchingEntryCount = countMatchingEntries(rawTemplates, addition.structure(), addition.processor());
        ApprenticeCodex.LOGGER.info(
                "Errand Mage village house injected into pool {}: structure={} processor={} rawEntries {}->{} totalWeight {}->{} matchingEntries {}->{} expandedTemplates {}->{}",
                addition.pool(),
                addition.structure(),
                addition.processor(),
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
                .map(ResourceKey::location)
                .orElse(null);
        return expectedStructureId.equals(structureId) && expectedProcessorId.equals(processorId);
    }

}
