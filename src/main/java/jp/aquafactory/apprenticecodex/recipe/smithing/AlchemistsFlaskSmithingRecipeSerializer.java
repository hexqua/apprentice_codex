package jp.aquafactory.apprenticecodex.recipe.smithing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class AlchemistsFlaskSmithingRecipeSerializer implements RecipeSerializer<AlchemistsFlaskSmithingRecipe> {
    private static final MapCodec<AlchemistsFlaskSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("template").forGetter(AlchemistsFlaskSmithingRecipe::getTemplate),
                    Ingredient.CODEC.fieldOf("base").forGetter(AlchemistsFlaskSmithingRecipe::getBase),
                    Ingredient.CODEC.fieldOf("addition").forGetter(AlchemistsFlaskSmithingRecipe::getAddition),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlchemistsFlaskSmithingRecipe::getResultTemplate)
            ).apply(instance, AlchemistsFlaskSmithingRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, AlchemistsFlaskSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            AlchemistsFlaskSmithingRecipe::getTemplate,
            Ingredient.CONTENTS_STREAM_CODEC,
            AlchemistsFlaskSmithingRecipe::getBase,
            Ingredient.CONTENTS_STREAM_CODEC,
            AlchemistsFlaskSmithingRecipe::getAddition,
            ItemStack.STREAM_CODEC,
            AlchemistsFlaskSmithingRecipe::getResultTemplate,
            AlchemistsFlaskSmithingRecipe::new
    );

    @Override
    public MapCodec<AlchemistsFlaskSmithingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AlchemistsFlaskSmithingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
