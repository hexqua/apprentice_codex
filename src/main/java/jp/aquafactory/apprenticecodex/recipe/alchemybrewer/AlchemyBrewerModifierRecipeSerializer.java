package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public final class AlchemyBrewerModifierRecipeSerializer implements RecipeSerializer<AlchemyBrewerModifierRecipe> {
    private static final MapCodec<AlchemyBrewerModifierRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("input").forGetter(AlchemyBrewerModifierRecipe::input),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(AlchemyBrewerModifierRecipe::ingredient),
            ResourceLocation.CODEC.fieldOf("result").forGetter(AlchemyBrewerModifierRecipe::result),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(AlchemyBrewerModifierRecipe::priority)
    ).apply(instance, AlchemyBrewerModifierRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, AlchemyBrewerModifierRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AlchemyBrewerModifierRecipe::input,
            Ingredient.CONTENTS_STREAM_CODEC, AlchemyBrewerModifierRecipe::ingredient,
            ResourceLocation.STREAM_CODEC, AlchemyBrewerModifierRecipe::result,
            ByteBufCodecs.INT, AlchemyBrewerModifierRecipe::priority,
            AlchemyBrewerModifierRecipe::new
    );
    @Override public @NotNull MapCodec<AlchemyBrewerModifierRecipe> codec() { return CODEC; }
    @Override public @NotNull StreamCodec<RegistryFriendlyByteBuf, AlchemyBrewerModifierRecipe> streamCodec() { return STREAM_CODEC; }
}
