package jp.aquafactory.apprenticecodex.compat.patchouli;

import com.google.gson.annotations.SerializedName;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

// Patchouliがリフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EssenceSmokerRecipeComponent implements ICustomComponent {
    private static final int MAX_RECIPES_PER_PAGE = 3;
    private static final int ROW_HEIGHT = 20;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_INNER_PADDING = 1;
    private static final int CATALYST_X = 4;
    private static final int MATERIAL_X = 26;
    private static final int OUTPUT_X = 78;
    private static final int ROW_TEXT_Y_OFFSET = 6;
    private static final int PLUS_X = 21;
    private static final int ARROW_X = 53;
    private static final int SLOT_OUTER_COLOR = 0xFF111111;
    private static final int SLOT_INNER_COLOR = 0xFF8B8B8B;
    private static final int WARNING_COLOR = 0xFFAA3333;

    public String recipe = "";
    public String recipe2 = "";
    public String recipe3 = "";
    @SerializedName("recipe_ids")
    public List<String> configuredRecipeIds = List.of();

    private transient List<ResourceLocation> recipeIds = List.of();
    private transient List<DisplayRecipe> displayRecipes = List.of();
    private transient int componentX;
    private transient int componentY;

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        var parsedIds = new ArrayList<ResourceLocation>(MAX_RECIPES_PER_PAGE);
        collectConfiguredRecipeId(parsedIds, lookup, recipe);
        collectConfiguredRecipeId(parsedIds, lookup, recipe2);
        collectConfiguredRecipeId(parsedIds, lookup, recipe3);

        if (parsedIds.isEmpty()) {
            for (var configuredRecipeId : configuredRecipeIds) {
                collectConfiguredRecipeId(parsedIds, lookup, configuredRecipeId);
                if (parsedIds.size() >= MAX_RECIPES_PER_PAGE) {
                    break;
                }
            }
        }

        if (parsedIds.isEmpty()) {
            collectLookupRecipeId(parsedIds, lookup, "recipe");
            collectLookupRecipeId(parsedIds, lookup, "recipe2");
            collectLookupRecipeId(parsedIds, lookup, "recipe3");
        }

        if (parsedIds.isEmpty()) {
            var variables = lookup.apply(IVariable.wrap("recipe_ids")).asListOrSingleton();
            for (var variable : variables) {
                var rawId = variable.asString("").trim();
                if (rawId.equals("recipe_ids")) {
                    continue;
                }
                collectRecipeId(parsedIds, rawId);
                if (parsedIds.size() >= MAX_RECIPES_PER_PAGE) {
                    break;
                }
            }
        }

        recipeIds = List.copyOf(parsedIds);
    }

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.componentX = componentX;
        this.componentY = componentY;
        refreshRecipes();
    }

    @Override
    public void onDisplayed(IComponentRenderContext context) {
        refreshRecipes();
    }

    @Override
    public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        if (displayRecipes.isEmpty()) {
            graphics.drawString(
                    Minecraft.getInstance().font,
                    Component.literal("No recipes configured."),
                    componentX + CATALYST_X,
                    componentY + ROW_TEXT_Y_OFFSET,
                    WARNING_COLOR,
                    false
            );
            return;
        }

        for (int index = 0; index < displayRecipes.size(); index++) {
            renderRow(graphics, context, displayRecipes.get(index), mouseX, mouseY, componentY + index * ROW_HEIGHT);
        }
    }

    private void renderRow(
            GuiGraphics graphics,
            IComponentRenderContext context,
            DisplayRecipe displayRecipe,
            int mouseX,
            int mouseY,
            int rowY
    ) {
        var catalystX = componentX + CATALYST_X;
        var materialX = componentX + MATERIAL_X;
        var outputX = componentX + OUTPUT_X;

        drawSlot(graphics, catalystX, rowY);
        drawSlot(graphics, materialX, rowY);
        drawSlot(graphics, outputX, rowY);

        if (displayRecipe.recipe() != null) {
            context.renderIngredient(
                    graphics,
                    catalystX + SLOT_INNER_PADDING,
                    rowY + SLOT_INNER_PADDING,
                    mouseX,
                    mouseY,
                    displayRecipe.recipe().getCatalyst()
            );
            context.renderIngredient(
                    graphics,
                    materialX + SLOT_INNER_PADDING,
                    rowY + SLOT_INNER_PADDING,
                    mouseX,
                    mouseY,
                    displayRecipe.recipe().getMaterial()
            );
            context.renderItemStack(
                    graphics,
                    outputX + SLOT_INNER_PADDING,
                    rowY + SLOT_INNER_PADDING,
                    mouseX,
                    mouseY,
                    displayRecipe.recipe().getResultTemplate()
            );
        } else {
            var missingStack = new ItemStack(Items.BARRIER);
            context.renderItemStack(
                    graphics,
                    outputX + SLOT_INNER_PADDING,
                    rowY + SLOT_INNER_PADDING,
                    mouseX,
                    mouseY,
                    missingStack
            );
            if (context.isAreaHovered(mouseX, mouseY, outputX, rowY, SLOT_SIZE, SLOT_SIZE)) {
                context.setHoverTooltipComponents(List.of(
                        Component.literal("Missing Essence Smoker recipe"),
                        Component.literal(displayRecipe.recipeId().toString())
                ));
            }
        }

        graphics.drawString(
                Minecraft.getInstance().font,
                Component.literal("+"),
                componentX + PLUS_X,
                rowY + ROW_TEXT_Y_OFFSET,
                context.getTextColor(),
                false
        );
        graphics.drawString(
                Minecraft.getInstance().font,
                Component.literal("->"),
                componentX + ARROW_X,
                rowY + ROW_TEXT_Y_OFFSET,
                context.getTextColor(),
                false
        );
    }

    private void refreshRecipes() {
        if (recipeIds.isEmpty()) {
            displayRecipes = List.of();
            return;
        }

        var recipeManager = getClientRecipeManager();
        if (recipeManager == null) {
            displayRecipes = recipeIds.stream()
                    .map(DisplayRecipe::missing)
                    .toList();
            return;
        }

        var availableRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get());
        var resolved = new ArrayList<DisplayRecipe>(recipeIds.size());
        for (var recipeId : recipeIds) {
            var match = availableRecipes.stream()
                    .filter(recipe -> recipe.getId().equals(recipeId))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                ApprenticeCodex.LOGGER.warn("Patchouli Essence Smoker page could not resolve recipe {}.", recipeId);
                resolved.add(DisplayRecipe.missing(recipeId));
                continue;
            }

            resolved.add(DisplayRecipe.found(recipeId, match));
        }

        displayRecipes = List.copyOf(resolved);
    }

    @Nullable
    private static RecipeManager getClientRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getRecipeManager();
    }

    private static void drawSlot(@NotNull GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_OUTER_COLOR);
        graphics.fill(
                x + SLOT_INNER_PADDING,
                y + SLOT_INNER_PADDING,
                x + SLOT_SIZE - SLOT_INNER_PADDING,
                y + SLOT_SIZE - SLOT_INNER_PADDING,
                SLOT_INNER_COLOR
        );
    }

    private static void collectConfiguredRecipeId(
            List<ResourceLocation> parsedIds,
            UnaryOperator<IVariable> lookup,
            @Nullable String configuredValue
    ) {
        if (configuredValue == null) {
            return;
        }

        var rawValue = configuredValue.trim();
        if (rawValue.isEmpty()) {
            return;
        }

        if (!rawValue.startsWith("#")) {
            collectRecipeId(parsedIds, rawValue);
            return;
        }

        var resolvedValue = lookup.apply(IVariable.wrap(rawValue)).asString("").trim();
        if (!resolvedValue.equals(rawValue)) {
            collectRecipeId(parsedIds, resolvedValue);
        }
    }

    private static void collectLookupRecipeId(List<ResourceLocation> parsedIds, UnaryOperator<IVariable> lookup, String key) {
        var resolvedValue = lookup.apply(IVariable.wrap(key)).asString("").trim();
        if (!resolvedValue.equals(key)) {
            collectRecipeId(parsedIds, resolvedValue);
        }
    }

    private static void collectRecipeId(List<ResourceLocation> parsedIds, String rawId) {
        if (rawId.isEmpty() || parsedIds.size() >= MAX_RECIPES_PER_PAGE) {
            return;
        }

        var recipeId = ResourceLocation.tryParse(rawId);
        if (recipeId == null) {
            ApprenticeCodex.LOGGER.warn("Patchouli Essence Smoker page skipped invalid recipe id: {}", rawId);
            return;
        }
        parsedIds.add(recipeId);
    }

    private record DisplayRecipe(ResourceLocation recipeId, @Nullable EssenceSmokerRecipe recipe) {
        private static DisplayRecipe found(ResourceLocation recipeId, EssenceSmokerRecipe recipe) {
            return new DisplayRecipe(recipeId, recipe);
        }

        private static DisplayRecipe missing(ResourceLocation recipeId) {
            return new DisplayRecipe(recipeId, null);
        }
    }
}
