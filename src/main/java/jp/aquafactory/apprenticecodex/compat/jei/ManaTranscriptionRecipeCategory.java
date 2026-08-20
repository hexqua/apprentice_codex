package jp.aquafactory.apprenticecodex.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ManaTranscriptionRecipeCategory
        extends AbstractApprenticeCodexRecipeCategory<ManaTranscriptionJeiRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 76;
    private static final int TARGET_X = 4;
    private static final int OPERATION_ITEM_X = 34;
    private static final int INPUT_Y = 3;
    private static final int ARROW_X = 70;
    private static final int ARROW_Y = 4;
    private static final int OUTPUT_X = 106;
    private static final int OUTPUT_Y = 2;
    private static final int SECONDARY_OUTPUT_X = 132;
    private static final int DESCRIPTION_X = 4;
    private static final int DESCRIPTION_Y = 31;
    private static final int DESCRIPTION_WIDTH = WIDTH - DESCRIPTION_X * 2;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0x404040;

    ManaTranscriptionRecipeCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        super(
                ApprenticeCodexJeiRecipeTypes.MANA_TRANSCRIPTION,
                Component.translatable("spell.apprenticecodex.mana_transcription"),
                guiHelper,
                iconStack,
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull ManaTranscriptionJeiRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        builder.addInputSlot(TARGET_X, INPUT_Y)
                .setStandardSlotBackground()
                .addItemStack(recipe.target().copy())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                        Component.translatable(targetTooltipKey(recipe.mode())).withStyle(ChatFormatting.GRAY)
                ));
        builder.addInputSlot(OPERATION_ITEM_X, INPUT_Y)
                .setStandardSlotBackground()
                .addItemStacks(recipe.operationItems())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                        Component.translatable(operationItemTooltipKey(recipe.mode())).withStyle(ChatFormatting.GRAY)
                ));
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.targetResult().copy())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                        Component.translatable(resultTooltipKey(recipe.mode())).withStyle(ChatFormatting.GRAY)
                ));
        if (!recipe.secondaryResult().isEmpty()) {
            builder.addOutputSlot(SECONDARY_OUTPUT_X, OUTPUT_Y)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.secondaryResult().copy())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                            Component.translatable("jei.apprenticecodex.mana_transcription.extracted_enchantment")
                                    .withStyle(ChatFormatting.GRAY)
                    ));
        }
    }

    @Override
    public void draw(
            @NotNull ManaTranscriptionJeiRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        drawLabel(guiGraphics, Component.literal("+"), 26, 8);
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        drawDescription(guiGraphics, Component.translatable(descriptionKey(recipe.mode())));
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(ManaTranscriptionJeiRecipe recipe) {
        return recipe.id();
    }

    private static void drawDescription(GuiGraphics guiGraphics, Component description) {
        var font = Minecraft.getInstance().font;
        var fullDescription = Component.empty()
                .append(description)
                .append("\n")
                .append(Component.translatable("jei.apprenticecodex.mana_transcription.example_note")
                        .withStyle(ChatFormatting.DARK_GRAY));
        var lines = font.split(fullDescription, DESCRIPTION_WIDTH);
        for (var line = 0; line < lines.size(); ++line) {
            guiGraphics.drawString(
                    font,
                    lines.get(line),
                    DESCRIPTION_X,
                    DESCRIPTION_Y + line * TEXT_LINE_HEIGHT,
                    TEXT_COLOR,
                    false
            );
        }
    }

    private static String descriptionKey(ManaTranscriptionJeiRecipe.Mode mode) {
        return switch (mode) {
            case EXTRACTION -> "jei.apprenticecodex.mana_transcription.extraction";
            case REPAIR_COST_RESET -> "jei.apprenticecodex.mana_transcription.repair_cost_reset";
        };
    }

    private static String targetTooltipKey(ManaTranscriptionJeiRecipe.Mode mode) {
        return switch (mode) {
            case EXTRACTION -> "jei.apprenticecodex.mana_transcription.enchanted_target";
            case REPAIR_COST_RESET -> "jei.apprenticecodex.mana_transcription.worked_target";
        };
    }

    private static String operationItemTooltipKey(ManaTranscriptionJeiRecipe.Mode mode) {
        return switch (mode) {
            case EXTRACTION -> "jei.apprenticecodex.mana_transcription.unwritten_book";
            case REPAIR_COST_RESET -> "jei.apprenticecodex.mana_transcription.reset_medium";
        };
    }

    private static String resultTooltipKey(ManaTranscriptionJeiRecipe.Mode mode) {
        return switch (mode) {
            case EXTRACTION -> "jei.apprenticecodex.mana_transcription.extracted_target";
            case REPAIR_COST_RESET -> "jei.apprenticecodex.mana_transcription.reset_target";
        };
    }
}
