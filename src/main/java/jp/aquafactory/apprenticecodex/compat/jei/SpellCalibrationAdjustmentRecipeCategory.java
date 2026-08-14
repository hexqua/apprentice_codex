package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellCalibrationAdjustmentRecipeCategory
        extends AbstractApprenticeCodexRecipeCategory<SpellCalibrationAdjustmentJeiRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 90;
    private static final int TARGET_X = 4;
    private static final int ADJUSTMENT_X = 38;
    private static final int INPUT_Y = 3;
    private static final int ARROW_X = 68;
    private static final int ARROW_Y = 4;
    private static final int OUTPUT_X = 104;
    private static final int OUTPUT_Y = 2;
    private static final int EFFECT_X = 4;
    private static final int EFFECT_Y = 32;
    private static final int EFFECT_WIDTH = WIDTH - EFFECT_X * 2;
    private static final int TEXT_LINE_HEIGHT = 11;
    private static final int TEXT_COLOR = 0x404040;

    public SpellCalibrationAdjustmentRecipeCategory(IGuiHelper guiHelper) {
        super(
                ApprenticeCodexJeiRecipeTypes.SPELL_CALIBRATION_ADJUSTMENT,
                new ItemStack(ItemRegistry.SPELL_CALIBRATION_BENCH.get()).getHoverName(),
                guiHelper,
                new ItemStack(ItemRegistry.SPELL_CALIBRATION_BENCH.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull SpellCalibrationAdjustmentJeiRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        builder.addInputSlot(TARGET_X, INPUT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.target().copy());
        var adjustmentSlot = builder.addInputSlot(ADJUSTMENT_X, INPUT_Y)
                .setStandardSlotBackground()
                .addItemStacks(recipe.adjustments());
        var resultSlot = builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStacks(recipe.results());
        builder.createFocusLink(adjustmentSlot, resultSlot);
    }

    @Override
    public void draw(
            @NotNull SpellCalibrationAdjustmentJeiRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        drawLabel(guiGraphics, Component.literal("+"), 27, 8);
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        var description = Component.empty().append(recipe.effectLines().getFirst());
        for (var line = 1; line < recipe.effectLines().size(); ++line) {
            description.append("\n").append(recipe.effectLines().get(line));
        }
        recipe.constraintDisplay().translationKey()
                .map(Component::translatable)
                .ifPresent(constraint -> description.append("\n").append(constraint));
        drawDescription(guiGraphics, description);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(SpellCalibrationAdjustmentJeiRecipe recipe) {
        return recipe.id();
    }

    private static void drawDescription(GuiGraphics guiGraphics, Component description) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(description, EFFECT_WIDTH);
        for (var line = 0; line < lines.size(); ++line) {
            guiGraphics.drawString(
                    font,
                    lines.get(line),
                    EFFECT_X,
                    EFFECT_Y + line * TEXT_LINE_HEIGHT,
                    TEXT_COLOR,
                    false
            );
        }
    }
}
