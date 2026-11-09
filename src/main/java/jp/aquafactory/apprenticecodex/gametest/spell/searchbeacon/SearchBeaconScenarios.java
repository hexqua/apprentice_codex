package jp.aquafactory.apprenticecodex.gametest.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SearchBeaconScenarios {
    private SearchBeaconScenarios() {}

    static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var unknownMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city"),
                    1L
            );
            var searchedMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "village_plains"),
                    2L
            );
            var unknownResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            unknownMarker,
                            BlockPos.ZERO,
                            SearchBeaconState.StructureKnowledge.UNKNOWN,
                            4.0
                    )
            ));
            var knownOnlyResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            searchedMarker,
                            new BlockPos(8, 0, 0),
                            SearchBeaconState.StructureKnowledge.SEARCHED,
                            64.0
                    )
            ));

            helper.assertFalse(
                    SearchBeaconSearchService.shouldRefundOfferedItems(unknownResult),
                    "SearchBeacon should not refund items when it found an unknown structure"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(knownOnlyResult),
                    "SearchBeacon should refund items when it only found known structures"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(new SearchBeaconSearchService.SearchResult(List.of())),
                    "SearchBeacon should refund items when it found nothing"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(null),
                    "SearchBeacon should refund items before any result is available"
            );
        });
    }

    public static void assertSearchBeaconTarget(GameTestHelper helper, Item item, String expectedTarget) {
        var definition = SearchBeaconTargetManager.getDefinition(new ItemStack(item));
        helper.assertTrue(definition != null, "SearchBeacon target missing for " + BuiltInRegistries.ITEM.getKey(item));
        helper.assertTrue(
                definition != null
                        && definition.targets().contains(new SearchBeaconTargetList.TargetReference(false, ResourceLocation.parse(expectedTarget))),
                "SearchBeacon target mismatch for " + BuiltInRegistries.ITEM.getKey(item)
        );
    }
}
