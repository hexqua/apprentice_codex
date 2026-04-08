package jp.aquafactory.apprenticecodex.recipe.grindrunner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;
import java.util.Optional;

public final class GrindRunnerRecipeSerializer implements RecipeSerializer<GrindRunnerRecipe> {
    private static final String ALLOW_UNSTACKABLE_AND_TAGGED_INPUT = "allow_unstackable_and_tagged_input";
    private static boolean hasLoggedObsoleteInputFlagWarning = false;

    private static final MapCodec<GrindRunnerRecipe> CODEC = RecordCodecBuilder.<LegacyAwareSerializedRecipe>mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(LegacyAwareSerializedRecipe::ingredient),
                    ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(LegacyAwareSerializedRecipe::results),
                    Codec.BOOL.optionalFieldOf(ALLOW_UNSTACKABLE_AND_TAGGED_INPUT)
                            .forGetter(LegacyAwareSerializedRecipe::obsoleteInputFlag)
            ).apply(instance, LegacyAwareSerializedRecipe::new)
    ).xmap(
            serialized -> {
                if (serialized.obsoleteInputFlag().isPresent()) {
                    logObsoleteInputFlagWarningOnce();
                }
                return new GrindRunnerRecipe(serialized.ingredient(), serialized.results());
            },
            recipe -> new LegacyAwareSerializedRecipe(recipe.getIngredient(), recipe.getResultTemplates(), Optional.empty())
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, GrindRunnerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            GrindRunnerRecipe::getIngredient,
            ItemStack.LIST_STREAM_CODEC,
            GrindRunnerRecipe::getResultTemplates,
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

    private static void logObsoleteInputFlagWarningOnce() {
        if (hasLoggedObsoleteInputFlagWarning) {
            return;
        }

        hasLoggedObsoleteInputFlagWarning = true;
        ApprenticeCodex.LOGGER.warn(
                "GrindRunner recipe field '{}' is obsolete and ignored. Remove it from datapacks when convenient.",
                ALLOW_UNSTACKABLE_AND_TAGGED_INPUT
        );
    }

    private record LegacyAwareSerializedRecipe(
            Ingredient ingredient,
            List<ItemStack> results,
            Optional<Boolean> obsoleteInputFlag
    ) {
    }
}
