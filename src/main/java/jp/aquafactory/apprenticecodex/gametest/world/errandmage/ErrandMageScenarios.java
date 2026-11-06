package jp.aquafactory.apprenticecodex.gametest.world.errandmage;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.ErrandMageVillagerTradesEvent;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeStack;
import jp.aquafactory.apprenticecodex.loot.RandomSpellImbueHelper;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

public final class ErrandMageScenarios {
    private ErrandMageScenarios() {}

    static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        var deskPos = new BlockPos(1, 1, 0);
        var absoluteDeskPos = helper.absolutePos(deskPos);
        helper.setBlock(deskPos, BlockRegistry.APPRENTICE_DESK.get());
        helper.getLevel().setDayTime(2000L);

        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        villager.setVillagerData(new VillagerData(villager.getVillagerData().getType(), VillagerProfession.NONE, 1));
        villager.refreshBrain(helper.getLevel());
        villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(helper.getLevel().dimension(), absoluteDeskPos));

        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isAlive(), "Villager died before claiming the Apprentice Desk");
            helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfessionRegistry.ERRAND_MAGE.get(),
                    "Villager did not become Errand Mage: " + villager.getVillagerData().getProfession());
            var jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
            helper.assertTrue(jobSite != null, "Villager did not store a job site");
            helper.assertTrue(jobSite != null && absoluteDeskPos.equals(jobSite.pos()),
                    "Villager claimed unexpected job site: " + jobSite);
        });
    }

    static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var templatePoolRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);

            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/plains/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/desert/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/savanna/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/snowy/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/snowy/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    3
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/taiga/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/taiga/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    3
            );
        });
    }

    static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var structureTemplateManager = helper.getLevel().getStructureManager();
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/plains/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/desert/villagers"),
                    new BlockPos(2, 1, 4)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/savanna/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/snowy/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/snowy/villagers"),
                    new BlockPos(3, 1, 5)
            );
            assertVillageHouseTemplateLoadsWithRequiredBlocks(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/taiga/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/taiga/villagers"),
                    new BlockPos(3, 1, 5)
            );

            assertErrandMageHouseLootTableExists(helper);
        });
    }

    static void randomSpellImbueCreatesLockedLevelOneWand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wand = new ItemStack(ItemRegistry.WOODEN_WAND.get());
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    wand,
                    List.of(spell.getSpellResource()),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(1L)
            );

            helper.assertTrue(result.is(ItemRegistry.WOODEN_WAND.get()),
                    "Random spell imbue unexpectedly used its fallback: " + result);
            helper.assertTrue(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue did not create a spell container");
            var spellContainer = ISpellContainer.get(result);
            helper.assertTrue(spellContainer != null,
                    "Random spell imbue did not expose its spell container");
            if (spellContainer == null) {
                return;
            }
            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellContainer.getMaxSpellCount() == 1
                            && spellContainer.getActiveSpellCount() == 1
                            && !spellContainer.isSpellWheel()
                            && !spellContainer.mustEquip(),
                    "Random spell imbue created an unexpected spell container: " + spellContainer);
            helper.assertTrue(spellData.getSpell() == spell
                            && spellData.getLevel() == 1
                            && spellData.isLocked(),
                    "Random spell imbue did not create a locked level one spell: " + spellData);
        });
    }

    static void randomSpellImbueUsesOnlyConfiguredEnabledSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = SpellRegistry.HEAL_SPELL.get();
            var allowedSpells = List.of(magicMissile, heal);
            var spellIds = allowedSpells.stream().map(AbstractSpell::getSpellResource).toList();
            var random = RandomSource.create(2L);

            for (var attempt = 0; attempt < 32; attempt++) {
                var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                        new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                        spellIds,
                        ItemRegistry.CRUDE_INK.get(),
                        random
                );
                helper.assertTrue(result.is(ItemRegistry.WOODEN_WAND.get()),
                        "Random spell imbue unexpectedly used its fallback: " + result);
                var spellContainer = ISpellContainer.get(result);
                var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
                helper.assertTrue(spellData != SpellData.EMPTY && allowedSpells.contains(spellData.getSpell()),
                        "Random spell imbue selected a spell outside its configured candidates: " + spellData);
                helper.assertTrue(spellData.getSpell().isEnabled(),
                        "Random spell imbue selected a disabled spell: " + spellData);
            }
        });
    }

    static void randomSpellImbueFallsBackWhenNoSpellIsEligible(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                    List.of(),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(3L)
            );

            helper.assertTrue(result.is(ItemRegistry.CRUDE_INK.get()),
                    "Random spell imbue did not use Crude Ink for an empty candidate list: " + result);
            helper.assertFalse(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue copied a spell container to its fallback item");
        });
    }

    static void randomSpellImbueFallsBackForUnknownSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var result = RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                    new ItemStack(ItemRegistry.WOODEN_WAND.get()),
                    List.of(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "missing_spell")),
                    ItemRegistry.CRUDE_INK.get(),
                    RandomSource.create(4L)
            );

            helper.assertTrue(result.is(ItemRegistry.CRUDE_INK.get()),
                    "Random spell imbue did not use Crude Ink for an unknown spell id: " + result);
            helper.assertFalse(ISpellContainer.isSpellContainer(result),
                    "Random spell imbue copied a spell container to its fallback item for an unknown spell id");
        });
    }

    static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var taggedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedScroll, tag -> tag.putString("apprenticecodex_test", "tagged"));
            var taggedScrollCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedScrollCost, tag -> tag.putString("apprenticecodex_test", "cost"));
            var taggedScrollItemCost = new ItemCost(
                    taggedScrollCost.getItemHolder(),
                    taggedScrollCost.getCount(),
                    DataComponentPredicate.allOf(taggedScrollCost.getComponents())
            );
            var scrollOffer = new MerchantOffer(
                    taggedScrollItemCost,
                    Optional.of(new ItemCost(Items.EMERALD, 16)),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    5,
                    0.05F
            );
            helper.assertTrue(scrollOffer.satisfiedBy(taggedScroll, new ItemStack(Items.EMERALD, 16)),
                    "Tagged scroll should satisfy the errand mage ink trade even when the saved cost stack has tags");

            var trades = createEmptyVillagerTrades();
            ErrandMageVillagerTradesEvent.onVillagerTrades(
                    new VillagerTradesEvent(
                            trades,
                            VillagerProfessionRegistry.ERRAND_MAGE.get(),
                            helper.getLevel().registryAccess()
                    )
            );
            var tarnishedCrownOffer = createOffers(trades.get(4), 0L).stream()
                    .filter(offer -> offer.getBaseCostA().is(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(tarnishedCrownOffer != null, "Errand Mage level 4 should buy Tarnished Crowns");
            helper.assertTrue(ErrandMageTradeManager.shouldIgnorePaymentTags(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()),
                    "Errand Mage Tarnished Crown trade should ignore payment tags");
            var taggedTarnishedCrown = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedTarnishedCrown, tag -> tag.putString("apprenticecodex_test", "tagged"));
            helper.assertTrue(tarnishedCrownOffer.satisfiedBy(taggedTarnishedCrown, ItemStack.EMPTY),
                    "Tagged Tarnished Crown should satisfy the current errand mage buy trade");

            var healingPotion = PotionContentsHelper.createPotionStack(Items.POTION, Potions.HEALING.value());
            var regenerationPotion = PotionContentsHelper.createPotionStack(Items.POTION, Potions.REGENERATION.value());
            var healingPotionDefinition = new ErrandMageTradeStack(
                    BuiltInRegistries.ITEM.getKey(Items.POTION),
                    1,
                    Optional.of(BuiltInRegistries.POTION.getKey(Potions.HEALING.value())),
                    false
            );
            var healingPotionCost = ErrandMageTradeHelper.createPaymentStack(healingPotionDefinition, healingPotion);
            helper.assertTrue(healingPotionCost.test(healingPotion),
                    "Payment stack should satisfy its own potion component");
            helper.assertFalse(healingPotionCost.test(regenerationPotion),
                    "Payment stack should not accept a different potion component");
        });
    }

    static void errandMageComfortBerriesPaymentsSurviveSerialization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var comfortBerries = new ItemStack(ItemRegistry.COMFORT_BERRIES.get(), 22);
            var definition = new ErrandMageTradeStack(
                    ItemRegistry.COMFORT_BERRIES.getId(),
                    22,
                    Optional.empty(),
                    false
            );
            var currentCost = ErrandMageTradeHelper.createPaymentStack(definition, comfortBerries);
            var syncedCurrentCost = roundTripItemCostThroughNetwork(
                    currentCost,
                    helper.getLevel().registryAccess()
            );
            helper.assertTrue(syncedCurrentCost.test(comfortBerries),
                    "Current Comfort Berries payment should survive merchant offer sync");

            var legacyCost = new ItemCost(
                    comfortBerries.getItemHolder(),
                    comfortBerries.getCount(),
                    DataComponentPredicate.allOf(comfortBerries.getComponents()),
                    comfortBerries.copy()
            );
            var syncedLegacyCost = roundTripItemCostThroughNetwork(
                    legacyCost,
                    helper.getLevel().registryAccess()
            );
            helper.assertFalse(syncedLegacyCost.components().test(comfortBerries),
                    "Legacy Comfort Berries FOOD predicate should reproduce the sync mismatch");
            helper.assertTrue(syncedLegacyCost.test(comfortBerries),
                    "Legacy synced Comfort Berries payment should be migrated");

            var savedLegacyCost = roundTripItemCostThroughNbt(
                    legacyCost,
                    helper.getLevel().registryAccess()
            );
            helper.assertTrue(savedLegacyCost.test(comfortBerries),
                    "Legacy saved Comfort Berries payment should be migrated");

            var alteredBerries = comfortBerries.copy();
            alteredBerries.set(DataComponents.RARITY, Rarity.EPIC);
            helper.assertFalse(savedLegacyCost.test(alteredBerries),
                    "Comfort Berries migration should preserve non-FOOD component requirements");

            var customPackBerries = comfortBerries.copyWithCount(21);
            var customPackLegacyCost = new ItemCost(
                    customPackBerries.getItemHolder(),
                    customPackBerries.getCount(),
                    DataComponentPredicate.allOf(customPackBerries.getComponents()),
                    customPackBerries.copy()
            );
            var syncedCustomPackLegacyCost = roundTripItemCostThroughNetwork(
                    customPackLegacyCost,
                    helper.getLevel().registryAccess()
            );
            helper.assertFalse(syncedCustomPackLegacyCost.test(customPackBerries),
                    "Legacy FOOD migration should not rewrite custom Comfort Berries payment counts");

            var comfortSandwich = new ItemStack(ItemRegistry.COMFORT_SANDWICH.get());
            var legacySandwichCost = new ItemCost(
                    comfortSandwich.getItemHolder(),
                    comfortSandwich.getCount(),
                    DataComponentPredicate.allOf(comfortSandwich.getComponents()),
                    comfortSandwich.copy()
            );
            var syncedLegacySandwichCost = roundTripItemCostThroughNetwork(
                    legacySandwichCost,
                    helper.getLevel().registryAccess()
            );
            helper.assertFalse(syncedLegacySandwichCost.test(comfortSandwich),
                    "Legacy FOOD migration should remain limited to default Comfort Berries trades");
        });
    }

    static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var trades = createEmptyVillagerTrades();
            ErrandMageVillagerTradesEvent.onVillagerTrades(
                    new VillagerTradesEvent(
                            trades,
                            VillagerProfessionRegistry.ERRAND_MAGE.get(),
                            helper.getLevel().registryAccess()
                    )
            );

            var level1Offers = createOffers(trades.get(1), 0L);
            helper.assertFalse(hasBaseCostItem(level1Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.FROZEN_BONE_SHARD.get()),
                    "Errand Mage level 1 trades should not buy Frozen Bone Shards");
            assertContainsOffer(helper, level1Offers,
                    new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 32),
                    ItemStack.EMPTY,
                    new ItemStack(Items.EMERALD),
                    16,
                    "Errand Mage level 1 trades should buy Rapid Spellcaster Rounds");

            var level2Listings = trades.get(2);
            var exclusiveItems = new LinkedHashSet<Item>();
            var level2Offers = createOffers(level2Listings, 0L);
            helper.assertTrue(countBaseCostItems(level2Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get(),
                            io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_PEARL.get()) == 1,
                    "Errand Mage level 2 trades should choose either Shriving Stone or Divine Pearl");
            var exclusiveRandom = RandomSource.create(0L);
            var exclusiveListing = level2Listings.get(0);
            for (var attempt = 0; attempt < 64 && exclusiveItems.size() < 2; attempt++) {
                var offer = exclusiveListing.getOffer(null, exclusiveRandom);
                helper.assertTrue(offer != null, "Errand Mage level 2 exclusive trade should create an offer");
                exclusiveItems.add(offer.getBaseCostA().getItem());
            }
            helper.assertTrue(exclusiveItems.size() == 2,
                    "Errand Mage level 2 exclusive trade should be able to choose both configured items");
            assertContainsOffer(helper, level2Offers,
                    new ItemStack(Items.EMERALD),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 8),
                    12,
                    "Errand Mage level 2 trades should always sell Basic Spellcaster Rounds");

            assertContainsOffer(helper, createOffers(trades.get(4), 0L),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()),
                    ItemStack.EMPTY,
                    new ItemStack(Items.EMERALD, 2),
                    16,
                    "Errand Mage level 4 trades should buy Tarnished Crowns for two Emeralds");
            assertContainsOffer(helper, createOffers(trades.get(4), 0L),
                    new ItemStack(Items.EMERALD, 32),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()),
                    12,
                    "Errand Mage level 4 trades should sell Spellstained Arcane Ingots");
            assertContainsOffer(helper, createOffers(trades.get(5), 0L),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    new ItemStack(Items.EMERALD, 16),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    "Errand Mage level 5 trades should sell Common Ink for Scrolls");
            assertContainsOffer(helper, createOffers(trades.get(5), 0L),
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.WRITABLE_BOOK),
                    new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()),
                    3,
                    "Errand Mage level 5 trades should sell Isekai Travel Guidebooks");
        });
    }

    static void assertVillageHousePoolContains(
            GameTestHelper helper,
            Registry<StructureTemplatePool> templatePoolRegistry,
            ResourceLocation poolId,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId,
            int expectedWeight
    ) {
        var pool = templatePoolRegistry.get(poolId);
        helper.assertTrue(pool != null, "Missing village house pool: " + poolId);

        var rawTemplates = ((StructureTemplatePoolAccessor) pool).apprenticecodex$getRawTemplates();
        var matchingRawEntries = rawTemplates.stream()
                .filter(pair -> isMatchingSinglePoolElement(pair.getFirst(), expectedStructureId, expectedProcessorId))
                .toList();

        helper.assertTrue(matchingRawEntries.size() == 1,
                "Expected exactly one Errand Mage entry in " + poolId + " but found " + matchingRawEntries.size());
        helper.assertTrue(
                !matchingRawEntries.isEmpty() && matchingRawEntries.get(0).getSecond() == expectedWeight,
                "Unexpected Errand Mage weight in " + poolId + ": " + (matchingRawEntries.isEmpty() ? "missing" : matchingRawEntries.get(0).getSecond())
        );

        long expandedMatchCount = pool.getShuffledTemplates(RandomSource.create(0L)).stream()
                .filter(element -> isMatchingSinglePoolElement(element, expectedStructureId, expectedProcessorId))
                .count();
        helper.assertTrue(expandedMatchCount == expectedWeight,
                "Expanded template count mismatch in " + poolId + ": " + expandedMatchCount);
    }

    static boolean isMatchingSinglePoolElement(
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

    static void assertVillageHouseTemplateLoadsWithRequiredBlocks(
            GameTestHelper helper,
            StructureTemplateManager structureTemplateManager,
            ResourceLocation structureId,
            ResourceLocation expectedVillagerPool,
            BlockPos expectedLootChestPos
    ) {
        var template = structureTemplateManager.get(structureId).orElse(null);
        helper.assertTrue(template != null, "Missing village house template: " + structureId);

        var jigsawBlocks = template != null
                ? template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true)
                : List.<StructureTemplate.StructureBlockInfo>of();
        helper.assertTrue(jigsawBlocks.size() == 2,
                "Unexpected jigsaw count for " + structureId + ": " + jigsawBlocks.size());

        boolean hasBottom = false;
        boolean hasEntrance = false;
        for (var jigsawBlock : jigsawBlocks) {
            var nbt = jigsawBlock.nbt();
            helper.assertTrue(nbt != null, "Village house jigsaw is missing NBT: " + structureId + " at " + jigsawBlock.pos());
            if (nbt == null) {
                continue;
            }

            var name = ResourceLocation.tryParse(nbt.getString("name"));
            var target = ResourceLocation.tryParse(nbt.getString("target"));
            var pool = ResourceLocation.tryParse(nbt.getString("pool"));
            if (ResourceLocation.withDefaultNamespace("bottom").equals(name)
                    && ResourceLocation.withDefaultNamespace("bottom").equals(target)
                    && expectedVillagerPool.equals(pool)) {
                hasBottom = true;
            }
            if (ResourceLocation.withDefaultNamespace("building_entrance").equals(name)
                    && ResourceLocation.withDefaultNamespace("building_entrance").equals(target)
                    && ResourceLocation.withDefaultNamespace("empty").equals(pool)) {
                hasEntrance = true;
            }
        }

        helper.assertTrue(hasBottom, "Village house is missing villager spawn jigsaw: " + structureId);
        helper.assertTrue(hasEntrance, "Village house is missing building entrance jigsaw: " + structureId);

        var lootChests = template != null
                ? template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST, true)
                : List.<StructureTemplate.StructureBlockInfo>of();
        helper.assertTrue(lootChests.size() == 1,
                "Unexpected loot chest count for " + structureId + ": " + lootChests.size());
        if (lootChests.size() != 1) {
            return;
        }

        var lootChest = lootChests.getFirst();
        helper.assertTrue(expectedLootChestPos.equals(lootChest.pos()),
                "Unexpected loot chest position for " + structureId + ": " + lootChest.pos());
        var chestNbt = lootChest.nbt();
        helper.assertTrue(chestNbt != null,
                "Village house loot chest is missing NBT: " + structureId + " at " + lootChest.pos());
        if (chestNbt != null) {
            helper.assertTrue("minecraft:chest".equals(chestNbt.getString("id")),
                    "Village house loot chest has an unexpected block entity id: " + structureId);
            helper.assertTrue("apprenticecodex:chests/errand_mage_house".equals(chestNbt.getString("LootTable")),
                    "Village house loot chest has an unexpected loot table: " + structureId);
        }
    }

    public static LootParams createChestLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 1, 0)))
                .create(LootContextParamSets.CHEST);
    }

    static void assertErrandMageHouseLootTableExists(GameTestHelper helper) {
        var lootTableId = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "chests/errand_mage_house"
        );
        var lootTable = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, lootTableId)
        );
        helper.assertTrue(lootTable != LootTable.EMPTY,
                "Errand Mage house loot table is missing: " + lootTableId);
    }

    static Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>> createEmptyVillagerTrades() {
        var trades = new Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>>();
        for (var level = 1; level <= 5; level++) {
            trades.put(level, new ArrayList<>());
        }
        return trades;
    }

    static List<MerchantOffer> createOffers(List<VillagerTrades.ItemListing> listings, long randomSeed) {
        var random = RandomSource.create(randomSeed);
        var offers = new ArrayList<MerchantOffer>();
        for (var listing : listings) {
            var offer = listing.getOffer(null, random);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    static void assertContainsOffer(GameTestHelper helper, List<MerchantOffer> offers,
                                            ItemStack costA, ItemStack costB, ItemStack result,
                                            int maxUses, String message) {
        helper.assertTrue(offers.stream().anyMatch(offer -> offerMatches(offer, costA, costB, result, maxUses)), message);
    }

    static boolean offerMatches(MerchantOffer offer, ItemStack costA, ItemStack costB, ItemStack result, int maxUses) {
        return stackMatches(offer.getBaseCostA(), costA)
                && stackMatches(offer.getCostB(), costB)
                && stackMatches(offer.getResult(), result)
                && offer.getMaxUses() == maxUses;
    }

    static boolean stackMatches(ItemStack actual, ItemStack expected) {
        if (expected.isEmpty()) {
            return actual.isEmpty();
        }

        return actual.is(expected.getItem()) && actual.getCount() == expected.getCount();
    }

    static boolean hasBaseCostItem(List<MerchantOffer> offers, Item item) {
        return offers.stream().anyMatch(offer -> offer.getBaseCostA().is(item));
    }

    static int countBaseCostItems(List<MerchantOffer> offers, Item... items) {
        var count = 0;
        for (var offer : offers) {
            for (var item : items) {
                if (offer.getBaseCostA().is(item)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    static ItemCost roundTripItemCostThroughNetwork(ItemCost cost, RegistryAccess registryAccess) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            ItemCost.STREAM_CODEC.encode(buffer, cost);
            return ItemCost.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    static ItemCost roundTripItemCostThroughNbt(ItemCost cost, RegistryAccess registryAccess) {
        var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        var encoded = ItemCost.CODEC.encodeStart(ops, cost).getOrThrow();
        return ItemCost.CODEC.parse(ops, encoded).getOrThrow();
    }
}
