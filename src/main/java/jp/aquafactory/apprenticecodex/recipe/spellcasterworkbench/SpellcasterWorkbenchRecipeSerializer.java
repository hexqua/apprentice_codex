package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;

public final class SpellcasterWorkbenchRecipeSerializer implements RecipeSerializer<SpellcasterWorkbenchRecipe> {
    private static final Codec<SpellcasterWorkbenchRecipe.SizedIngredient> SIZED_INGREDIENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(SpellcasterWorkbenchRecipe.SizedIngredient::ingredient),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(SpellcasterWorkbenchRecipe.SizedIngredient::count)
            ).apply(instance, SpellcasterWorkbenchRecipe.SizedIngredient::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, SpellcasterWorkbenchRecipe.SizedIngredient> SIZED_INGREDIENT_STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SpellcasterWorkbenchRecipe.SizedIngredient decode(RegistryFriendlyByteBuf buffer) {
                    return new SpellcasterWorkbenchRecipe.SizedIngredient(
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                            ByteBufCodecs.INT.decode(buffer)
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SpellcasterWorkbenchRecipe.SizedIngredient value) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, value.ingredient());
                    ByteBufCodecs.INT.encode(buffer, value.count());
                }
            };

    private static final MapCodec<SpellcasterWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SIZED_INGREDIENT_CODEC.listOf().fieldOf("ingredients").forGetter(SpellcasterWorkbenchRecipe::getSizedIngredients),
                    ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(SpellcasterWorkbenchRecipe::getResultTemplates),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(SpellcasterWorkbenchRecipe::getPriority)
            ).apply(instance, SpellcasterWorkbenchRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, SpellcasterWorkbenchRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SpellcasterWorkbenchRecipe decode(RegistryFriendlyByteBuf buffer) {
            var ingredientCount = buffer.readVarInt();
            var ingredients = new ArrayList<SpellcasterWorkbenchRecipe.SizedIngredient>(ingredientCount);
            for (var index = 0; index < ingredientCount; ++index) {
                ingredients.add(SIZED_INGREDIENT_STREAM_CODEC.decode(buffer));
            }

            var resultCount = buffer.readVarInt();
            var results = new ArrayList<ItemStack>(resultCount);
            for (var index = 0; index < resultCount; ++index) {
                results.add(ItemStack.STREAM_CODEC.decode(buffer));
            }

            return new SpellcasterWorkbenchRecipe(ingredients, results, ByteBufCodecs.INT.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SpellcasterWorkbenchRecipe recipe) {
            var ingredients = recipe.getSizedIngredients();
            buffer.writeVarInt(ingredients.size());
            for (var ingredient : ingredients) {
                SIZED_INGREDIENT_STREAM_CODEC.encode(buffer, ingredient);
            }

            var results = recipe.getResultTemplates();
            buffer.writeVarInt(results.size());
            for (var result : results) {
                ItemStack.STREAM_CODEC.encode(buffer, result);
            }

            ByteBufCodecs.INT.encode(buffer, recipe.getPriority());
        }
    };

    @Override
    public MapCodec<SpellcasterWorkbenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SpellcasterWorkbenchRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
