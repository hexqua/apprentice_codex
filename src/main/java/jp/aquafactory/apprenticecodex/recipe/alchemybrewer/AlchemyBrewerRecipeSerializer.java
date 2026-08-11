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

public final class AlchemyBrewerRecipeSerializer implements RecipeSerializer<AlchemyBrewerRecipe> {
    private static final MapCodec<AlchemyBrewerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("base").forGetter(AlchemyBrewerRecipe::base),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(AlchemyBrewerRecipe::ingredient),
            ResourceLocation.CODEC.fieldOf("result").forGetter(AlchemyBrewerRecipe::result),
            Codec.INT.fieldOf("fluid_amount_mb").forGetter(AlchemyBrewerRecipe::fluidAmountMb),
            Codec.intRange(1, AlchemyBrewerRecipe.MAX_PROCESSING_TIME_TICKS)
                    .fieldOf("processing_time_ticks").forGetter(AlchemyBrewerRecipe::processingTimeTicks),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(AlchemyBrewerRecipe::priority)
    ).apply(instance, AlchemyBrewerRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, AlchemyBrewerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AlchemyBrewerRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, AlchemyBrewerRecipe::ingredient,
            ResourceLocation.STREAM_CODEC, AlchemyBrewerRecipe::result,
            ByteBufCodecs.INT, AlchemyBrewerRecipe::fluidAmountMb,
            ByteBufCodecs.INT, AlchemyBrewerRecipe::processingTimeTicks,
            ByteBufCodecs.INT, AlchemyBrewerRecipe::priority,
            AlchemyBrewerRecipe::new
    );
    @Override public @NotNull MapCodec<AlchemyBrewerRecipe> codec() { return CODEC; }
    @Override public @NotNull StreamCodec<RegistryFriendlyByteBuf, AlchemyBrewerRecipe> streamCodec() { return STREAM_CODEC; }
}
