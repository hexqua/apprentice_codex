package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.loot.RandomSpellImbueLootFunction;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.function.BiConsumer;

public final class ChestLootTableGenerator implements LootTableSubProvider {
    private static final ResourceKey<LootTable> ERRAND_MAGE_HOUSE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "chests/errand_mage_house")
    );

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(ERRAND_MAGE_HOUSE, createTable(List.of(
                new PoolDefinition(
                        new IntRange(3, 5),
                        1.0F,
                        List.of(
                                entry(Items.BOOK, 10, 1, 3),
                                entry(Items.BREAD, 5, 1, 4),
                                entry(ItemRegistry.COMFORT_BERRIES.get(), 5, 1, 5),
                                entry(ItemRegistry.COMFORT_SANDWICH.get(), 1, 1, 4),
                                entry(ItemRegistry.CRUDE_INK.get(), 1, 1, 1)
                        )
                ),
                new PoolDefinition(
                        new IntRange(2, 2),
                        0.5F,
                        List.of(
                                entry(
                                        ItemRegistry.WOODEN_WAND.get(),
                                        1,
                                        1,
                                        1,
                                        RandomSpellImbueLootFunction.builder(
                                                woodenWandSpellIds(),
                                                ItemRegistry.CRUDE_INK.get()
                                        )
                                ),
                                entry(ItemRegistry.ARCANE_CINDER.get(), 1, 1, 1)
                        )
                )
        )));
    }

    private static LootTable.Builder createTable(List<PoolDefinition> pools) {
        var table = LootTable.lootTable();
        pools.forEach(pool -> table.withPool(pool.build()));
        return table;
    }

    private static ItemEntryDefinition entry(ItemLike item, int weight, int minCount, int maxCount) {
        return new ItemEntryDefinition(item, weight, new IntRange(minCount, maxCount), List.of());
    }

    private static ItemEntryDefinition entry(
            ItemLike item,
            int weight,
            int minCount,
            int maxCount,
            LootItemFunction.Builder... functions
    ) {
        return new ItemEntryDefinition(item, weight, new IntRange(minCount, maxCount), List.of(functions));
    }

    private static EmptyEntryDefinition emptyEntry(int weight) {
        return new EmptyEntryDefinition(weight);
    }

    private static List<ResourceLocation> woodenWandSpellIds() {
        return List.of(
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get().getSpellResource(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get().getSpellResource(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.RECALL_SPELL.get().getSpellResource(),
                io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get().getSpellResource(),
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK.get().getSpellResource(),
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource()
        );
    }

    private record IntRange(int min, int max) {
        private IntRange {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid loot range: " + min + "-" + max);
            }
        }

        NumberProvider toNumberProvider() {
            return min == max
                    ? ConstantValue.exactly(min)
                    : UniformGenerator.between(min, max);
        }

        boolean isOne() {
            return min == 1 && max == 1;
        }
    }

    private sealed interface EntryDefinition permits ItemEntryDefinition, EmptyEntryDefinition {
        LootPoolSingletonContainer.Builder<?> build();
    }

    private record ItemEntryDefinition(
            ItemLike item,
            int weight,
            IntRange count,
            List<LootItemFunction.Builder> functions
    ) implements EntryDefinition {
        private ItemEntryDefinition {
            if (weight < 1) {
                throw new IllegalArgumentException("Loot weight must be positive: " + weight);
            }
            functions = List.copyOf(functions);
        }

        @Override
        public LootPoolSingletonContainer.Builder<?> build() {
            var builder = LootItem.lootTableItem(item).setWeight(weight);
            if (!count.isOne()) {
                builder.apply(SetItemCountFunction.setCount(count.toNumberProvider()));
            }
            functions.forEach(builder::apply);
            return builder;
        }
    }

    private record EmptyEntryDefinition(int weight) implements EntryDefinition {
        private EmptyEntryDefinition {
            if (weight < 1) {
                throw new IllegalArgumentException("Loot weight must be positive: " + weight);
            }
        }

        @Override
        public LootPoolSingletonContainer.Builder<?> build() {
            return EmptyLootItem.emptyItem().setWeight(weight);
        }
    }

    private record PoolDefinition(IntRange rolls, float chance, List<EntryDefinition> entries) {
        private PoolDefinition {
            if (chance <= 0.0F || chance > 1.0F) {
                throw new IllegalArgumentException("Loot pool chance must be in (0, 1]: " + chance);
            }
            entries = List.copyOf(entries);
        }

        LootPool.Builder build() {
            var builder = LootPool.lootPool().setRolls(rolls.toNumberProvider());
            if (chance < 1.0F) {
                builder.when(LootItemRandomChanceCondition.randomChance(chance));
            }
            entries.forEach(entry -> builder.add(entry.build()));
            return builder;
        }
    }
}
