package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ArcanumInAJarRecipeCategory extends AbstractApprenticeCodexRecipeCategory<ArcanumInAJarJeiRecipe> {
    private static final ResourceLocation RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "jei/arcanum_in_a_jar/configured_processing");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 44;
    private static final int INPUT_X = 28;
    private static final int INPUT_Y = 2;
    private static final int ARROW_X = 68;
    private static final int ARROW_Y = 5;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 1;
    private static final int INFO_X = 4;
    private static final int INFO_Y = 32;

    public ArcanumInAJarRecipeCategory(IGuiHelper guiHelper) {
        super(
                ApprenticeCodexJeiRecipeTypes.ARCANUM_IN_A_JAR,
                new ItemStack(ItemRegistry.ARCANUM_IN_A_JAR.get()).getHoverName(),
                guiHelper,
                new ItemStack(ItemRegistry.ARCANUM_IN_A_JAR.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull ArcanumInAJarJeiRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        builder.addInputSlot(INPUT_X, INPUT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.material().copy());
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.product().copy());
    }

    @Override
    public void draw(
            @NotNull ArcanumInAJarJeiRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        drawLabel(guiGraphics, createProcessingTimeComponent(recipe), INFO_X, INFO_Y);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(@NotNull ArcanumInAJarJeiRecipe recipe) {
        return RECIPE_ID;
    }

    private static Component createProcessingTimeComponent(ArcanumInAJarJeiRecipe recipe) {
        var seconds = Math.max(1, (recipe.processingTimeTicks() + 19) / 20);
        return Component.translatable("jei.apprenticecodex.arcanum_in_a_jar.time", seconds)
                .withStyle(ChatFormatting.GRAY);
    }
}
