package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AlchemyBrewerRecipeCategory extends AbstractApprenticeCodexRecipeCategory<AlchemyBrewerJeiRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 50;
    private static final int BASE_X = 4;
    private static final int INGREDIENT_X = 28;
    private static final int MODIFIER_X = 52;
    private static final int INPUT_Y = 3;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 4;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 2;
    private static final int INFO_Y = 36;

    public AlchemyBrewerRecipeCategory(IGuiHelper guiHelper) {
        super(
                ApprenticeCodexJeiRecipeTypes.ALCHEMY_BREWER,
                new ItemStack(ItemRegistry.ALCHEMY_BREWER.get()).getHoverName(),
                guiHelper,
                new ItemStack(ItemRegistry.ALCHEMY_BREWER.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull AlchemyBrewerJeiRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        addIngredientSlot(builder, BASE_X, recipe.base());
        addIngredientSlot(builder, INGREDIENT_X, recipe.ingredient());
        if (recipe.modifier() != null) {
            addIngredientSlot(builder, MODIFIER_X, recipe.modifier());
        }

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.result().copy())
                .addRichTooltipCallback((slot, tooltip) -> {
                    tooltip.add(createAmountComponent(recipe));
                    tooltip.add(createProcessingTimeComponent(recipe));
                    tooltip.add(Component.translatable("jei.apprenticecodex.alchemy_brewer.tank_output")
                            .withStyle(ChatFormatting.GRAY));
                });
    }

    @Override
    public void draw(
            @NotNull AlchemyBrewerJeiRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        drawLabel(guiGraphics, Component.literal("+"), 21, 8);
        drawLabel(guiGraphics, Component.literal("+"), 45, 8);
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        if (recipe.modifier() == null) {
            drawLabel(guiGraphics, Component.translatable("jei.apprenticecodex.alchemy_brewer.no_additive"), MODIFIER_X, 8);
        }
        drawLabel(guiGraphics, createAmountComponent(recipe), 4, INFO_Y);
        drawLabel(guiGraphics, createProcessingTimeComponent(recipe), 104, INFO_Y);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(AlchemyBrewerJeiRecipe recipe) {
        return recipe.id();
    }

    private static void addIngredientSlot(IRecipeLayoutBuilder builder, int x, Ingredient ingredient) {
        var slot = builder.addInputSlot(x, INPUT_Y).setStandardSlotBackground();
        var stacks = copyIngredientStacks(ingredient, 1);
        if (stacks.isEmpty()) {
            slot.addIngredients(ingredient);
        } else {
            slot.addItemStacks(stacks);
        }
    }

    private static Component createAmountComponent(AlchemyBrewerJeiRecipe recipe) {
        return Component.translatable(
                "jei.apprenticecodex.alchemy_brewer.amount",
                recipe.fluidAmountMb(),
                recipe.fluidAmountMb() / AlchemyBrewerBlockEntity.DOSE_AMOUNT_MB
        ).withStyle(ChatFormatting.GRAY);
    }

    private static Component createProcessingTimeComponent(AlchemyBrewerJeiRecipe recipe) {
        int seconds = Math.max(1, (recipe.processingTimeTicks() + 19) / 20);
        return Component.translatable("jei.apprenticecodex.alchemy_brewer.time", seconds)
                .withStyle(ChatFormatting.GRAY);
    }
}
