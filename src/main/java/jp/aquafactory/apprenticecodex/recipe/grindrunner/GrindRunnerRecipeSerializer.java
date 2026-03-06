package jp.aquafactory.apprenticecodex.recipe.grindrunner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class GrindRunnerRecipeSerializer implements RecipeSerializer<GrindRunnerRecipe> {
    private static final String ALLOW_UNSTACKABLE_AND_TAGGED_INPUT = "allow_unstackable_and_tagged_input";

    private static final MapCodec<GrindRunnerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(GrindRunnerRecipe::getIngredient),
                    ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(GrindRunnerRecipe::getResultTemplates),
                    Codec.BOOL.optionalFieldOf(ALLOW_UNSTACKABLE_AND_TAGGED_INPUT, false)
                            .forGetter(GrindRunnerRecipe::allowsUnstackableAndTaggedInput)
            ).apply(instance, GrindRunnerRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, GrindRunnerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            GrindRunnerRecipe::getIngredient,
            ItemStack.LIST_STREAM_CODEC,
            GrindRunnerRecipe::getResultTemplates,
            ByteBufCodecs.BOOL,
            GrindRunnerRecipe::allowsUnstackableAndTaggedInput,
            GrindRunnerRecipe::new
    );

    @Override
    public MapCodec<GrindRunnerRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GrindRunnerRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
