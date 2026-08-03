package jp.aquafactory.apprenticecodex.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import jp.aquafactory.apprenticecodex.registry.LootFunctionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
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
    private final List<ResourceLocation> spellIds;
    private final Item fallbackItem;

    public RandomSpellImbueLootFunction(
            LootItemCondition[] conditions,
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
    public @NotNull LootItemFunctionType getType() {
        return LootFunctionRegistry.RANDOM_SPELL_IMBUE.get();
    }

    public static final class Serializer extends LootItemConditionalFunction.Serializer<RandomSpellImbueLootFunction> {
        @Override
        public void serialize(
                JsonObject json,
                RandomSpellImbueLootFunction function,
                JsonSerializationContext context
        ) {
            super.serialize(json, function, context);
            var spells = new JsonArray();
            function.spellIds.forEach(spellId -> spells.add(spellId.toString()));
            json.add("spells", spells);
            json.addProperty("fallback_item", BuiltInRegistries.ITEM.getKey(function.fallbackItem).toString());
        }

        @Override
        public @NotNull RandomSpellImbueLootFunction deserialize(
                JsonObject json,
                JsonDeserializationContext context,
                LootItemCondition[] conditions
        ) {
            var spells = GsonHelper.getAsJsonArray(json, "spells").asList().stream()
                    .map(element -> ResourceLocation.parse(GsonHelper.convertToString(element, "spell")))
                    .toList();
            var fallbackItemId = ResourceLocation.parse(GsonHelper.getAsString(json, "fallback_item"));
            var fallbackItem = BuiltInRegistries.ITEM.getOptional(fallbackItemId)
                    .orElseThrow(() -> new JsonSyntaxException("Unknown fallback item '" + fallbackItemId + "'"));
            return new RandomSpellImbueLootFunction(conditions, spells, fallbackItem);
        }
    }
}
