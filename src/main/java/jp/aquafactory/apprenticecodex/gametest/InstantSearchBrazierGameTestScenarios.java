package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconRefundManager;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
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
            helper.assertTrue(SearchBeaconRefundManager.hasPending(player),
                    "Using an Instant Search Brazier should persist its pending refund");

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
            helper.assertTrue(!SearchBeaconRefundManager.hasPending(player),
                    "Refunding an Instant Search Brazier should clear its persisted refund");
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
        var beacon = SearchBeaconSummoning.summonFromInstantBrazier(
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
        helper.assertTrue(!SearchBeaconRefundManager.hasPending(player),
                "Starting a search should consume the persisted brazier refund");
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

    static void pendingBrazierSurvivesSpellDataRoundTripAndRecoversOnce(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "instant_search_brazier_save_test"
        );
        player.setXRot(90.0F);
        var refundStack = new ItemStack(ItemRegistry.INSTANT_SEARCH_BRAZIER.get());
        refundStack.setHoverName(Component.literal("Persisted Test Brazier"));
        var beacon = SearchBeaconSummoning.summonFromInstantBrazier(
                helper.getLevel(), player, 500, 0, refundStack);
        helper.assertTrue(beacon != null, "Instant Search Brazier test should summon a Search Beacon");

        var spellData = Capabilities.getSpellDataOrNull(player);
        helper.assertTrue(spellData != null, "Test player should have CodexSpellData");
        var saved = spellData.saveAll();
        helper.assertTrue(SearchBeaconRefundManager.consume(player, beacon.getUUID()),
                "Test setup should clear the live pending refund before loading saved data");
        spellData.loadAll(saved);

        SearchBeaconRefundManager.recoverPending(player);
        helper.assertTrue(countBraziersInInventory(player) == 1,
                "Loading saved CodexSpellData should recover one Instant Search Brazier");
        var recoveredStack = player.getInventory().items.stream()
                .filter(stack -> stack.is(ItemRegistry.INSTANT_SEARCH_BRAZIER.get()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(recoveredStack.getHoverName().getString().equals("Persisted Test Brazier"),
                "Recovered Instant Search Brazier should keep its data components");
        helper.assertTrue(!SearchBeaconRefundManager.hasPending(player),
                "Recovering the saved brazier should consume its persisted refund");

        SearchBeaconRefundManager.recoverPending(player);
        beacon.tick();
        helper.assertTrue(countBraziersInInventory(player) == 1,
                "Repeated recovery and a stale Search Beacon must not duplicate the brazier");
        helper.assertTrue(beacon.isRemoved(),
                "A Search Beacon whose refund was already recovered should remove itself");
        helper.succeed();
    }

    static void unloadingBeaconReturnsBrazierToOwnerInventory(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "instant_search_brazier_unload_test"
        );
        player.setXRot(90.0F);
        var beacon = SearchBeaconSummoning.summonFromInstantBrazier(
                helper.getLevel(),
                player,
                500,
                0,
                new ItemStack(ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
        );
        helper.assertTrue(beacon != null, "Instant Search Brazier test should summon a Search Beacon");

        beacon.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(countBraziersInInventory(player) == 1,
                "Unloading a Search Beacon should return its brazier to the online owner");
        helper.assertTrue(!SearchBeaconRefundManager.hasPending(player),
                "Returning an unloaded Search Beacon should clear its persisted refund");
        helper.succeed();
    }

    static void cancelOnlyResetsCooldownForSpellSummonedBeacon(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "instant_search_brazier_cancel_cooldown_test"
        );
        player.setXRot(90.0F);
        player.setShiftKeyDown(true);
        var spell = SpellRegistry.SEARCH_BEACON.get();
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getPlayerCooldowns().addCooldown(spell, 1200, 1200);

        var itemBeacon = SearchBeaconSummoning.summonFromInstantBrazier(
                helper.getLevel(),
                player,
                500,
                0,
                new ItemStack(ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
        );
        helper.assertTrue(itemBeacon != null, "Instant Search Brazier test should summon a Search Beacon");
        itemBeacon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Cancelling an item-summoned Search Beacon must keep the spell cooldown");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0D))
                .forEach(ItemEntity::discard);

        var spellBeacon = SearchBeaconSummoning.summonFromSpell(helper.getLevel(), player, 500, 100);
        helper.assertTrue(spellBeacon != null, "Search Beacon spell test should summon a Search Beacon");
        spellBeacon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Cancelling a spell-summoned Search Beacon should reset its spell cooldown");
        helper.succeed();
    }

    private static int countBraziersInInventory(net.minecraft.server.level.ServerPlayer player) {
        return player.getInventory().items.stream()
                .filter(stack -> stack.is(ItemRegistry.INSTANT_SEARCH_BRAZIER.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static SearchBeaconEntity findSingleBeacon(GameTestHelper helper, AABB searchBox) {
        var beacons = helper.getLevel().getEntitiesOfClass(SearchBeaconEntity.class, searchBox);
        helper.assertTrue(beacons.size() == 1,
                "Expected exactly one Search Beacon but found " + beacons.size());
        return beacons.get(0);
    }
}
