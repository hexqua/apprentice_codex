package jp.aquafactory.apprenticecodex.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.LootFunctionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RandomSpellImbueLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomSpellImbueLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(instance.group(
                    ResourceLocation.CODEC.listOf().fieldOf("spells")
                            .forGetter(RandomSpellImbueLootFunction::spellIds),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("fallback_item")
                            .forGetter(RandomSpellImbueLootFunction::fallbackItem)
            )).apply(instance, RandomSpellImbueLootFunction::new)
    );

    private final List<ResourceLocation> spellIds;
    private final Item fallbackItem;

    public RandomSpellImbueLootFunction(
            List<LootItemCondition> conditions,
            List<ResourceLocation> spellIds,
            Item fallbackItem
    ) {
        super(conditions);
        this.spellIds = List.copyOf(spellIds);
        this.fallbackItem = fallbackItem;
    }

    public static LootItemFunction.Builder builder(List<ResourceLocation> spellIds, Item fallbackItem) {
        return simpleBuilder(conditions -> new RandomSpellImbueLootFunction(conditions, spellIds, fallbackItem));
    }

    @Override
    protected @NotNull ItemStack run(@NotNull ItemStack stack, @NotNull LootContext context) {
        return RandomSpellImbueHelper.imbueRandomEnabledSpellOrFallback(
                stack,
                spellIds,
                fallbackItem,
                context.getRandom()
        );
    }

    @Override
    public @NotNull LootItemFunctionType<RandomSpellImbueLootFunction> getType() {
        return LootFunctionRegistry.RANDOM_SPELL_IMBUE.get();
    }

    private List<ResourceLocation> spellIds() {
        return spellIds;
    }

    private Item fallbackItem() {
        return fallbackItem;
    }
}
