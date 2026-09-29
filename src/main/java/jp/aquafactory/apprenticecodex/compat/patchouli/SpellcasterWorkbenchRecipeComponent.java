package jp.aquafactory.apprenticecodex.compat.patchouli;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

// Patchouliがリフレクションで参照するため、IDE側の未使用検知を無効化する。
@SuppressWarnings("unused")
public final class SpellcasterWorkbenchRecipeComponent implements ICustomComponent {
    private static final int SLOT_SIZE = 18;
    private static final int MAX_RECIPES_PER_PAGE = 2;
    private static final int ROW_HEIGHT = 50;
    private static final int[] INPUT_X = {0, 20, 0};
    private static final int[] INPUT_Y = {0, 10, 20};
    private static final int OUTPUT_X = 68;
    private static final int OUTPUT_Y = 10;
    private static final int OUTPUT_COLUMNS = 2;
    private static final int OUTPUT_ROWS = 2;
    private static final int SLOT_SPACING = 20;
    private static final int SLOT_OUTER_COLOR = 0xFF111111;
    private static final int SLOT_INNER_COLOR = 0xFF8B8B8B;

    public String recipe = "";
    public String recipe2 = "";

    private transient List<ResourceLocation> recipeIds = List.of();
    private transient List<DisplayRecipe> displayRecipes = List.of();
    private transient int componentX;
    private transient int componentY;

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
        var parsedIds = new ArrayList<ResourceLocation>(MAX_RECIPES_PER_PAGE);
        collectConfiguredRecipeId(parsedIds, lookup, registries, recipe);
        collectConfiguredRecipeId(parsedIds, lookup, registries, recipe2);
        if (parsedIds.isEmpty()) {
            collectLookupRecipeId(parsedIds, lookup, registries, "recipe");
            collectLookupRecipeId(parsedIds, lookup, registries, "recipe2");
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
        for (var index = 0; index < displayRecipes.size(); ++index) {
            var displayRecipe = displayRecipes.get(index).recipe();
            if (displayRecipe != null) {
                renderRecipe(graphics, context, mouseX, mouseY, displayRecipe, componentY + index * ROW_HEIGHT);
            }
        }
    }

    private void renderRecipe(
            GuiGraphics graphics,
            IComponentRenderContext context,
            int mouseX,
            int mouseY,
            SpellcasterWorkbenchRecipe displayRecipe,
            int rowY
    ) {
        for (var index = 0; index < INPUT_X.length; ++index) {
            drawSlot(graphics, componentX + INPUT_X[index], rowY + INPUT_Y[index]);
        }
        graphics.drawString(Minecraft.getInstance().font, Component.literal("->"),
                componentX + 47, rowY + 16, context.getTextColor(), false);

        var ingredients = displayRecipe.getSizedIngredients();
        for (var index = 0; index < Math.min(INPUT_X.length, ingredients.size()); ++index) {
            var x = componentX + INPUT_X[index] + 1;
            var y = rowY + INPUT_Y[index] + 1;
            var sizedIngredient = ingredients.get(index);
            var preview = createIngredientPreview(displayRecipe, sizedIngredient);
            if (preview.isEmpty()) {
                context.renderIngredient(graphics, x, y, mouseX, mouseY, sizedIngredient.ingredient());
            } else {
                context.renderItemStack(graphics, x, y, mouseX, mouseY, preview);
            }
        }

        var results = displayRecipe.getResultTemplates();
        var slotCount = OUTPUT_COLUMNS * OUTPUT_ROWS;
        var hasOverflow = results.size() > slotCount;
        var visibleCount = Math.min(results.size(), hasOverflow ? slotCount - 1 : slotCount);
        for (var index = 0; index < visibleCount; ++index) {
            var x = componentX + OUTPUT_X + index % OUTPUT_COLUMNS * SLOT_SPACING;
            var y = rowY + OUTPUT_Y + index / OUTPUT_COLUMNS * SLOT_SPACING;
            drawSlot(graphics, x, y);
            context.renderItemStack(graphics, x + 1, y + 1, mouseX, mouseY, results.get(index));
        }
        if (hasOverflow) {
            // ページ内に収まらない出力は最後の枠のツールチップから確認できるようにする。
            var x = componentX + OUTPUT_X + (OUTPUT_COLUMNS - 1) * SLOT_SPACING;
            var y = rowY + OUTPUT_Y + (OUTPUT_ROWS - 1) * SLOT_SPACING;
            drawSlot(graphics, x, y);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.literal("+" + (results.size() - visibleCount)),
                    x + 2, y + 6, context.getTextColor(), false);
            if (context.isAreaHovered(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                context.setHoverTooltipComponents(results.subList(visibleCount, results.size()).stream()
                        .<Component>map(result -> Component.literal(result.getCount() + "x ").append(result.getHoverName()))
                        .toList());
            }
        }
    }

    private static ItemStack createIngredientPreview(
            SpellcasterWorkbenchRecipe workbenchRecipe,
            SpellcasterWorkbenchRecipe.SizedIngredient sizedIngredient
    ) {
        var candidates = sizedIngredient.ingredient().getItems();
        if (candidates.length == 0) {
            return ItemStack.EMPTY;
        }
        var preview = candidates[(int) ((System.currentTimeMillis() / 1000L) % candidates.length)].copy();
        if (workbenchRecipe.getRequiredSpell() != null
                && preview.is(ItemRegistry.SCROLL.get())) {
            var spell = SpellRegistry.getSpell(workbenchRecipe.getRequiredSpell());
            ISpellContainer.createScrollContainer(spell, workbenchRecipe.getMinimumSpellLevel(), preview);
        }
        preview.setCount(sizedIngredient.count());
        return preview;
    }

    private static void collectConfiguredRecipeId(
            List<ResourceLocation> parsedIds,
            UnaryOperator<IVariable> lookup,
            HolderLookup.Provider registries,
            @Nullable String configuredValue
    ) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return;
        }
        var configured = configuredValue.trim();
        if (configured.startsWith("#")) {
            configured = lookup.apply(IVariable.wrap(configured, registries)).asString("").trim();
        }
        collectRecipeId(parsedIds, configured);
    }

    private static void collectLookupRecipeId(
            List<ResourceLocation> parsedIds,
            UnaryOperator<IVariable> lookup,
            HolderLookup.Provider registries,
            String key
    ) {
        var configured = lookup.apply(IVariable.wrap(key, registries)).asString("").trim();
        if (!configured.equals(key)) {
            collectRecipeId(parsedIds, configured);
        }
    }

    private static void collectRecipeId(List<ResourceLocation> parsedIds, String configured) {
        if (configured.isEmpty() || configured.startsWith("#") || parsedIds.size() >= MAX_RECIPES_PER_PAGE) {
            return;
        }
        var recipeId = ResourceLocation.tryParse(configured);
        if (recipeId == null) {
            ApprenticeCodex.LOGGER.warn("Patchouli Spellcaster Workbench page skipped invalid recipe id: {}", configured);
            return;
        }
        parsedIds.add(recipeId);
    }

    private void refreshRecipes() {
        displayRecipes = List.of();
        if (recipeIds.isEmpty()) {
            return;
        }
        var recipeManager = getClientRecipeManager();
        if (recipeManager == null) {
            return;
        }
        var availableRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
        var resolved = new ArrayList<DisplayRecipe>(recipeIds.size());
        for (var recipeId : recipeIds) {
            var match = availableRecipes.stream()
                    .filter(holder -> holder.id().equals(recipeId))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                ApprenticeCodex.LOGGER.warn("Patchouli Spellcaster Workbench page could not resolve recipe {}.", recipeId);
            }
            resolved.add(new DisplayRecipe(match == null ? null : match.value()));
        }
        displayRecipes = List.copyOf(resolved);
    }

    private static @Nullable RecipeManager getClientRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getRecipeManager();
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_OUTER_COLOR);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_INNER_COLOR);
    }

    private record DisplayRecipe(@Nullable SpellcasterWorkbenchRecipe recipe) {
    }
}
