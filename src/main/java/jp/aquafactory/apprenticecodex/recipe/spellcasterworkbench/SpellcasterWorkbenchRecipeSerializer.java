package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceUpgrade;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.Optional;

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

    private static final Codec<Operation> OPERATION_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(Operation::type),
                    ResourceLocation.CODEC.fieldOf("feature").forGetter(Operation::feature),
                    ResourceLocation.CODEC.optionalFieldOf("required_spell").forGetter(Operation::requiredSpell),
                    Codec.INT.optionalFieldOf("minimum_spell_level", 1).forGetter(Operation::minimumSpellLevel)
            ).apply(instance, Operation::new)
    ).validate(operation -> {
        if (!"add_luminous_device_upgrade".equals(operation.type())) {
            return DataResult.error(() -> "Unknown SpellcasterWorkbench operation: " + operation.type());
        }
        if (LuminousDeviceUpgrade.byId(operation.feature()) == null) {
            return DataResult.error(() -> "Unknown Luminous Device upgrade: " + operation.feature());
        }
        return DataResult.success(operation);
    });

    private static final MapCodec<SpellcasterWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SIZED_INGREDIENT_CODEC.listOf().fieldOf("ingredients").forGetter(SpellcasterWorkbenchRecipe::getSizedIngredients),
                    ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(SpellcasterWorkbenchRecipe::getResultTemplates),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(SpellcasterWorkbenchRecipe::getPriority),
                    OPERATION_CODEC.optionalFieldOf("operation").forGetter(SpellcasterWorkbenchRecipeSerializer::getOperation)
            ).apply(instance, SpellcasterWorkbenchRecipeSerializer::createRecipe)
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

            var priority = ByteBufCodecs.INT.decode(buffer);
            var upgrade = buffer.readBoolean()
                    ? LuminousDeviceUpgrade.byId(buffer.readResourceLocation())
                    : null;
            var requiredSpell = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            var minimumSpellLevel = buffer.readVarInt();
            return new SpellcasterWorkbenchRecipe(
                    ingredients,
                    results,
                    priority,
                    upgrade,
                    requiredSpell,
                    minimumSpellLevel
            );
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
            var upgrade = recipe.getLuminousDeviceUpgrade();
            buffer.writeBoolean(upgrade != null);
            if (upgrade != null) {
                buffer.writeResourceLocation(upgrade.id());
            }
            buffer.writeBoolean(recipe.getRequiredSpell() != null);
            if (recipe.getRequiredSpell() != null) {
                buffer.writeResourceLocation(recipe.getRequiredSpell());
            }
            buffer.writeVarInt(recipe.getMinimumSpellLevel());
        }
    };

    private static SpellcasterWorkbenchRecipe createRecipe(
            java.util.List<SpellcasterWorkbenchRecipe.SizedIngredient> ingredients,
            java.util.List<ItemStack> results,
            int priority,
            Optional<Operation> operation
    ) {
        var value = operation.orElse(null);
        return new SpellcasterWorkbenchRecipe(
                ingredients,
                results,
                priority,
                value == null ? null : LuminousDeviceUpgrade.byId(value.feature()),
                value == null ? null : value.requiredSpell().orElse(null),
                value == null ? 1 : value.minimumSpellLevel()
        );
    }

    private static Optional<Operation> getOperation(SpellcasterWorkbenchRecipe recipe) {
        var upgrade = recipe.getLuminousDeviceUpgrade();
        if (upgrade == null) {
            return Optional.empty();
        }
        return Optional.of(new Operation(
                "add_luminous_device_upgrade",
                upgrade.id(),
                Optional.ofNullable(recipe.getRequiredSpell()),
                recipe.getMinimumSpellLevel()
        ));
    }

    private record Operation(
            String type,
            ResourceLocation feature,
            Optional<ResourceLocation> requiredSpell,
            int minimumSpellLevel
    ) {
    }

    @Override
    public MapCodec<SpellcasterWorkbenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SpellcasterWorkbenchRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
