package jp.aquafactory.apprenticecodex.compat.patchouli;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.spellcasterworkbench.SpellcasterWorkbenchDisplayExamples;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
    private static final int MAX_RECIPES_PER_PAGE = 2;
    private static final int[] INPUT_X = {0, 24, 0};
    private static final int[] INPUT_Y = {0, 12, 24};
    private static final int INPUT_HEIGHT = 48;
    private static final int OUTPUT_X = 68;
    private static final int OUTPUT_COLUMNS = 2;
    private static final int OUTPUT_ROWS = 2;
    private static final int SLOT_SPACING = 24;

    public String recipe = "";
    public String recipe2 = "";
    public String example = "";
    public String example2 = "";
    public String title = "";

    private transient List<DisplayReference> references = List.of();
    private transient List<DisplayRecipe> displayRecipes = List.of();
    private transient int componentX;
    private transient int componentY;
    private transient Component customTitle = Component.empty();

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        customTitle = PatchouliRecipeLayout.resolveTitle(title, lookup);
        var parsed = new ArrayList<DisplayReference>(MAX_RECIPES_PER_PAGE);
        collectSlot(parsed, lookup, recipe, example);
        collectSlot(parsed, lookup, recipe2, example2);
        if (parsed.isEmpty()) {
            collectLookupReference(parsed, lookup, "recipe", false);
            collectLookupReference(parsed, lookup, "example", true);
            collectLookupReference(parsed, lookup, "recipe2", false);
            collectLookupReference(parsed, lookup, "example2", true);
        }
        references = List.copyOf(parsed);
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
        var layout = PatchouliRecipeLayout.create(displayRecipes.stream()
                .map(value -> value.recipe() == null ? ItemStack.EMPTY : value.recipe().getResultTemplates().get(0))
                .toList(), customTitle, PatchouliRecipeLayout.WORKBENCH_ROW_HEIGHT);
        for (var index = 0; index < displayRecipes.size(); ++index) {
            var displayRecipe = displayRecipes.get(index).recipe();
            if (displayRecipe != null) {
                var row = layout.rows().get(index);
                PatchouliRecipeLayout.renderHeader(graphics, context, row.title(), componentX,
                        componentY + row.titleY(), mouseX, mouseY);
                renderRecipe(graphics, context, mouseX, mouseY, displayRecipe, componentY + row.recipeY());
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
        var results = displayRecipe.getResultTemplates();
        var outputColumns = Math.min(results.size(), OUTPUT_COLUMNS);
        var outputRows = Math.min((results.size() + OUTPUT_COLUMNS - 1) / OUTPUT_COLUMNS, OUTPUT_ROWS);
        var bodyX = PatchouliRecipeLayout.centeredX(componentX, OUTPUT_X + outputColumns * SLOT_SPACING);
        var outputY = (INPUT_HEIGHT - outputRows * SLOT_SPACING) / 2;
        for (var index = 0; index < INPUT_X.length; ++index) {
            PatchouliRecipeLayout.drawSlot(graphics, context, bodyX + INPUT_X[index], rowY + INPUT_Y[index]);
        }
        context.renderItemStack(graphics, bodyX + 50, rowY + 8,
                mouseX, mouseY, new ItemStack(BlockRegistry.SPELLCASTER_WORKBENCH.get()));
        PatchouliRecipeLayout.drawArrow(graphics, context, bodyX + 53, rowY + 30);

        var ingredients = displayRecipe.getSizedIngredients();
        for (var index = 0; index < Math.min(INPUT_X.length, ingredients.size()); ++index) {
            var x = bodyX + INPUT_X[index] + PatchouliRecipeLayout.SLOT_PADDING;
            var y = rowY + INPUT_Y[index] + PatchouliRecipeLayout.SLOT_PADDING;
            var sizedIngredient = ingredients.get(index);
            var preview = createIngredientPreview(displayRecipe, sizedIngredient);
            if (preview.isEmpty()) {
                context.renderIngredient(graphics, x, y, mouseX, mouseY, sizedIngredient.ingredient());
            } else {
                context.renderItemStack(graphics, x, y, mouseX, mouseY, preview);
            }
        }

        var slotCount = OUTPUT_COLUMNS * OUTPUT_ROWS;
        var hasOverflow = results.size() > slotCount;
        var visibleCount = Math.min(results.size(), hasOverflow ? slotCount - 1 : slotCount);
        for (var index = 0; index < visibleCount; ++index) {
            var x = bodyX + OUTPUT_X + index % OUTPUT_COLUMNS * SLOT_SPACING;
            var y = rowY + outputY + index / OUTPUT_COLUMNS * SLOT_SPACING;
            PatchouliRecipeLayout.drawSlot(graphics, context, x, y);
            context.renderItemStack(graphics, x + PatchouliRecipeLayout.SLOT_PADDING,
                    y + PatchouliRecipeLayout.SLOT_PADDING, mouseX, mouseY, results.get(index));
        }
        if (hasOverflow) {
            // ページ内に収まらない出力は最後の枠のツールチップから確認できるようにする。
            var x = bodyX + OUTPUT_X + (OUTPUT_COLUMNS - 1) * SLOT_SPACING;
            var y = rowY + outputY + (OUTPUT_ROWS - 1) * SLOT_SPACING;
            PatchouliRecipeLayout.drawSlot(graphics, context, x, y);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.literal("+" + (results.size() - visibleCount)),
                    x + PatchouliRecipeLayout.SLOT_PADDING, y + 8, context.getTextColor(), false);
            if (context.isAreaHovered(mouseX, mouseY, x, y,
                    PatchouliRecipeLayout.SLOT_SIZE, PatchouliRecipeLayout.SLOT_SIZE)) {
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

    private static void collectSlot(
            List<DisplayReference> parsed,
            UnaryOperator<IVariable> lookup,
            @Nullable String configuredRecipe,
            @Nullable String configuredExample
    ) {
        var reference = parseConfiguredReference(lookup, configuredRecipe, false);
        if (reference == null) {
            reference = parseConfiguredReference(lookup, configuredExample, true);
        }
        if (reference != null && parsed.size() < MAX_RECIPES_PER_PAGE) {
            parsed.add(reference);
        }
    }

    private static @Nullable DisplayReference parseConfiguredReference(
            UnaryOperator<IVariable> lookup,
            @Nullable String configuredValue,
            boolean example
    ) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return null;
        }
        var configured = configuredValue.trim();
        if (configured.startsWith("#")) {
            configured = lookup.apply(IVariable.wrap(configured)).asString("").trim();
        }
        return parseReference(configured, example);
    }

    private static void collectLookupReference(
            List<DisplayReference> parsed,
            UnaryOperator<IVariable> lookup,
            String key,
            boolean example
    ) {
        if (parsed.size() >= MAX_RECIPES_PER_PAGE) {
            return;
        }
        var configured = lookup.apply(IVariable.wrap(key)).asString("").trim();
        if (!configured.equals(key)) {
            var reference = parseReference(configured, example);
            if (reference != null) {
                parsed.add(reference);
            }
        }
    }

    private static @Nullable DisplayReference parseReference(String configured, boolean example) {
        if (configured.isEmpty() || configured.startsWith("#")) {
            return null;
        }
        var id = ResourceLocation.tryParse(configured);
        if (id == null) {
            ApprenticeCodex.LOGGER.warn("Patchouli Spellcaster Workbench page skipped invalid {} id: {}",
                    example ? "example" : "recipe", configured);
            return null;
        }
        return new DisplayReference(id, example);
    }

    private void refreshRecipes() {
        displayRecipes = List.of();
        if (references.isEmpty()) {
            return;
        }
        var recipeManager = getClientRecipeManager();
        if (recipeManager == null) {
            return;
        }
        var availableRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
        // Patchouli の book 再構築タイミングには依存できないため、表示中の制作数は設定変更に追従させない。
        // 見本はページを表示した時点の同期済みサーバー設定で作り直す。
        var examples = references.stream().anyMatch(DisplayReference::example)
                ? SpellcasterWorkbenchDisplayExamples.all()
                : List.<SpellcasterWorkbenchDisplayExamples.Example>of();
        var resolved = new ArrayList<DisplayRecipe>(references.size());
        for (var reference : references) {
            SpellcasterWorkbenchRecipe match;
            if (reference.example()) {
                match = examples.stream()
                        .filter(value -> value.id().equals(reference.id()))
                        .map(SpellcasterWorkbenchDisplayExamples.Example::recipe)
                        .findFirst()
                        .orElse(null);
            } else {
                var holder = availableRecipes.stream()
                        .filter(value -> value.getId().equals(reference.id()))
                        .findFirst()
                        .orElse(null);
                match = holder;
            }
            if (match == null) {
                ApprenticeCodex.LOGGER.warn("Patchouli Spellcaster Workbench page could not resolve {} {}.",
                        reference.example() ? "example" : "recipe", reference.id());
            }
            resolved.add(new DisplayRecipe(match));
        }
        displayRecipes = List.copyOf(resolved);
    }

    private static @Nullable RecipeManager getClientRecipeManager() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getRecipeManager();
    }

    private record DisplayReference(ResourceLocation id, boolean example) {
    }

    private record DisplayRecipe(@Nullable SpellcasterWorkbenchRecipe recipe) {
    }
}
