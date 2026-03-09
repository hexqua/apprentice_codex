package jp.aquafactory.apprenticecodex.recipe.essencesmoker;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.ItemStack;

public final class EssenceSmokerRecipeSerializer implements RecipeSerializer<EssenceSmokerRecipe> {
    private static final MapCodec<EssenceSmokerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("catalyst").forGetter(EssenceSmokerRecipe::getCatalyst),
                    Ingredient.CODEC.fieldOf("material").forGetter(EssenceSmokerRecipe::getMaterial),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(EssenceSmokerRecipe::getResultTemplate)
            ).apply(instance, EssenceSmokerRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, EssenceSmokerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            EssenceSmokerRecipe::getCatalyst,
            Ingredient.CONTENTS_STREAM_CODEC,
            EssenceSmokerRecipe::getMaterial,
            ItemStack.STREAM_CODEC,
            EssenceSmokerRecipe::getResultTemplate,
            EssenceSmokerRecipe::new
    );

    @Override
    public MapCodec<EssenceSmokerRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EssenceSmokerRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
