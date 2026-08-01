package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class InstantSearchBrazierGameTestScenarios {
    private InstantSearchBrazierGameTestScenarios() {
    }

    static void useCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig
                .useInstantSearchBrazierInitialRangeOverrideForGameTest(777)) {
            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 2, 0),
                    "instant_search_brazier_use_test"
            );
            var heldStack = new ItemStack(ItemRegistry.INSTANT_SEARCH_BRAZIER.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
            player.setXRot(90.0F);

            var useResult = heldStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Instant Search Brazier use should succeed");
            helper.assertTrue(heldStack.getCount() == 1,
                    "Instant Search Brazier should consume exactly one item on successful use");

            var beacon = findSingleBeacon(helper, player.getBoundingBox().inflate(16.0D));
            helper.assertTrue(beacon.getInitialRange() == 777,
                    "Instant Search Brazier should use the configured initial range");
            helper.assertTrue(beacon.getAdditionalRangePerItem() == 0,
                    "Instant Search Brazier should keep additional range fixed at zero");

            heldStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(heldStack.getCount() == 1,
                    "Using a second Instant Search Brazier should not consume it while one is active");

            var dropPos = beacon.blockPosition();
            beacon.discard();
            var refundedCount = helper.getLevel().getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(dropPos).inflate(2.0D),
                            item -> item.getItem().is(ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
                    ).stream()
                    .mapToInt(item -> item.getItem().getCount())
                    .sum();
            helper.assertTrue(refundedCount == 1,
                    "Removing an unstarted item-summoned Search Beacon should refund one brazier");
        }
        helper.succeed();
    }

    static void searchStartStopsBrazierRefundAndRejectsAdditionalOffer(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "instant_search_brazier_search_test"
        );
        player.setXRot(90.0F);
        var beacon = SearchBeaconSummoning.summon(
                helper.getLevel(),
                player,
                500,
                0,
                new ItemStack(ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
        );
        helper.assertTrue(beacon != null, "Instant Search Brazier test should summon a Search Beacon");

        var offeredItems = new ItemEntity(
                helper.getLevel(),
                beacon.getX(),
                beacon.getY(),
                beacon.getZ(),
                new ItemStack(Items.EMERALD, 2)
        );
        offeredItems.setNoGravity(true);
        helper.getLevel().addFreshEntity(offeredItems);
        for (int i = 0; i < 4; i++) {
            beacon.tick();
        }

        helper.assertTrue(beacon.getOfferedItem().getCount() == 1,
                "A zero-additional-range Search Beacon should accept only one offered item");
        helper.assertTrue(offeredItems.getItem().getCount() == 1,
                "A zero-additional-range Search Beacon should leave additional offered items untouched");

        beacon.mobInteract(player, InteractionHand.MAIN_HAND);
        var dropPos = beacon.blockPosition();
        beacon.discard();
        var refundedCount = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(dropPos).inflate(2.0D),
                        item -> item.getItem().is(ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
                ).stream()
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        helper.assertTrue(refundedCount == 0,
                "Removing a Search Beacon after search start should not refund the brazier");
        helper.succeed();
    }

    private static SearchBeaconEntity findSingleBeacon(GameTestHelper helper, AABB searchBox) {
        var beacons = helper.getLevel().getEntitiesOfClass(SearchBeaconEntity.class, searchBox);
        helper.assertTrue(beacons.size() == 1,
                "Expected exactly one Search Beacon but found " + beacons.size());
        return beacons.getFirst();
    }
}
