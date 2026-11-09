package jp.aquafactory.apprenticecodex.gametest.loot;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.getScrollSpell;

public final class LootTestSupport {
    private LootTestSupport() {}

    static LootParams createEmptyLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
    }

    public static LootContext createEmptyLootContext(GameTestHelper helper, long seed) {
        return new LootContext.Builder(createEmptyLootParams(helper))
                .withOptionalRandomSeed(seed)
                .create(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/random_applicable_enchantment"));
    }

    static List<AbstractSpell> getNonLootableApprenticeSpells() {
        return List.of(
                SpellRegistry.EXTRACT.get(),
                SpellRegistry.UNITE_LUNA.get(),
                SpellRegistry.ILLUMINATE_STELLAR.get(),
                SpellRegistry.MANIFESTATION_GRIMOIRE.get(),
                SpellRegistry.MANA_SLASH.get()
        );
    }

    static void assertLootTableGeneratesAllItems(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var remainingItems = expectedItems.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> remainingItems.remove(stack.getItem()));

        helper.assertTrue(remainingItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate expected items within " + attempts + " attempts: "
                        + remainingItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    static void assertLootTableGeneratesAnyItem(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var seenItems = new LinkedHashSet<Item>();
        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            if (expectedItems.contains(stack.getItem())) {
                seenItems.add(stack.getItem());
            }
        });

        helper.assertTrue(!seenItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate any of the expected items within " + attempts + " attempts: "
                        + expectedItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    static void assertLootTableNeverGeneratesBlockedSpells(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<AbstractSpell> blockedSpells
    ) {
        var blockedSpellIds = blockedSpells.stream()
                .map(AbstractSpell::getSpellResource)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var seenBlockedSpells = new LinkedHashSet<ResourceLocation>();
        var scrollSampleCount = new AtomicInteger();

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            var spell = getScrollSpell(stack);
            if (spell == null) {
                return;
            }

            scrollSampleCount.incrementAndGet();
            var spellId = spell.getSpellResource();
            if (spellId != null && blockedSpellIds.contains(spellId)) {
                seenBlockedSpells.add(spellId);
            }
        });

        helper.assertTrue(scrollSampleCount.get() > 0,
                "Loot table " + lootTableId + " did not generate any spell scrolls within " + attempts + " attempts");
        helper.assertTrue(seenBlockedSpells.isEmpty(),
                "Loot table " + lootTableId + " generated blocked apprentice scrolls: " + seenBlockedSpells);
    }

    static void sampleLootTable(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            Consumer<ItemStack> stackConsumer
    ) {
        var lootTable = helper.getLevel().getServer().getLootData().getLootTable(lootTableId);
        for (var i = 0; i < attempts; i++) {
            lootTable.getRandomItems(lootParams, stackConsumer);
        }
    }
}
