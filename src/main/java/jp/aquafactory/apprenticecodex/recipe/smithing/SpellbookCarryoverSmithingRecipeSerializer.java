package jp.aquafactory.apprenticecodex.recipe.smithing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class SpellbookCarryoverSmithingRecipeSerializer implements RecipeSerializer<SpellbookCarryoverSmithingRecipe> {
    private static final MapCodec<SpellbookCarryoverSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("template").forGetter(SpellbookCarryoverSmithingRecipe::getTemplate),
                    Ingredient.CODEC.fieldOf("base").forGetter(SpellbookCarryoverSmithingRecipe::getBase),
                    Ingredient.CODEC.fieldOf("addition").forGetter(SpellbookCarryoverSmithingRecipe::getAddition),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SpellbookCarryoverSmithingRecipe::getResultTemplate)
            ).apply(instance, SpellbookCarryoverSmithingRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, SpellbookCarryoverSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            SpellbookCarryoverSmithingRecipe::getTemplate,
            Ingredient.CONTENTS_STREAM_CODEC,
            SpellbookCarryoverSmithingRecipe::getBase,
            Ingredient.CONTENTS_STREAM_CODEC,
            SpellbookCarryoverSmithingRecipe::getAddition,
            ItemStack.STREAM_CODEC,
            SpellbookCarryoverSmithingRecipe::getResultTemplate,
            SpellbookCarryoverSmithingRecipe::new
    );

    @Override
    public MapCodec<SpellbookCarryoverSmithingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SpellbookCarryoverSmithingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
