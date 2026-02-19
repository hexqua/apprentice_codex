package jp.aquafactory.apprenticecodex.block.apprenticestable;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ApprenticesTableScreen extends AbstractContainerScreen<ApprenticesTableMenu> {
    private static final ResourceLocation VANILLA_BG_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");

    // Keep custom GUI parameters reachable for future texture swap.
    @SuppressWarnings("unused")
    private static final ResourceLocation APPRENTICES_TABLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/apprentices_table.png");
    @SuppressWarnings("unused")
    private static final int SPELL_LIST_X = 89;
    @SuppressWarnings("unused")
    private static final int SPELL_LIST_Y = 15;
    @SuppressWarnings("unused")
    private static final int SCROLL_BAR_X = 199;
    @SuppressWarnings("unused")
    private static final int SCROLL_BAR_Y = 15;
    @SuppressWarnings("unused")
    private static final int SCROLL_BAR_WIDTH = 12;
    @SuppressWarnings("unused")
    private static final int SCROLL_BAR_HEIGHT = 56;

    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
    private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
    private static final int SCROLLER_FULL_HEIGHT = 54;
    private static final int RECIPES_X = 52;
    private static final int RECIPES_Y = 14;

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    public ApprenticesTableScreen(ApprenticesTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.registerUpdateListener(this::containerChanged);
        --titleLabelY;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        renderBackground(gui);
        var left = leftPos;
        var top = topPos;
        gui.blit(VANILLA_BG_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);

        var scrollerOffset = (int) (41.0F * scrollOffs);
        gui.blit(VANILLA_BG_TEXTURE, left + 119, top + 15 + scrollerOffset, 176 + (isScrollBarActive() ? 0 : 12), 0, SCROLLER_WIDTH, SCROLLER_HEIGHT);

        var recipesLeft = leftPos + RECIPES_X;
        var recipesTop = topPos + RECIPES_Y;
        var recipeEnd = startIndex + RECIPES_COLUMNS * RECIPES_ROWS;
        renderButtons(gui, mouseX, mouseY, recipesLeft, recipesTop, recipeEnd);
        renderRecipes(gui, recipesLeft, recipesTop, recipeEnd);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (!displayRecipes) {
            return;
        }

        var recipesLeft = leftPos + RECIPES_X;
        var recipesTop = topPos + RECIPES_Y;
        var recipeEnd = startIndex + RECIPES_COLUMNS * RECIPES_ROWS;
        var recipes = menu.getRecipes();

        for (var recipeIndex = startIndex; recipeIndex < recipeEnd && recipeIndex < menu.getNumRecipes(); ++recipeIndex) {
            var recipeOffset = recipeIndex - startIndex;
            var recipeX = recipesLeft + recipeOffset % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            var recipeY = recipesTop + recipeOffset / RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            if (mouseX >= recipeX && mouseX < recipeX + RECIPES_IMAGE_SIZE_WIDTH
                    && mouseY >= recipeY && mouseY < recipeY + RECIPES_IMAGE_SIZE_HEIGHT) {
                gui.renderTooltip(font, recipes.get(recipeIndex).getResultItem(minecraft.level.registryAccess()), mouseX, mouseY);
            }
        }
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY, int recipesLeft, int recipesTop, int recipeEnd) {
        for (var recipeIndex = startIndex; recipeIndex < recipeEnd && recipeIndex < menu.getNumRecipes(); ++recipeIndex) {
            var recipeOffset = recipeIndex - startIndex;
            var recipeX = recipesLeft + recipeOffset % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            var row = recipeOffset / RECIPES_COLUMNS;
            var recipeY = recipesTop + row * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            var textureY = imageHeight;
            if (recipeIndex == menu.getSelectedRecipeIndex()) {
                textureY += RECIPES_IMAGE_SIZE_HEIGHT;
            } else if (mouseX >= recipeX && mouseY >= recipeY
                    && mouseX < recipeX + RECIPES_IMAGE_SIZE_WIDTH && mouseY < recipeY + RECIPES_IMAGE_SIZE_HEIGHT) {
                textureY += RECIPES_IMAGE_SIZE_HEIGHT * 2;
            }

            gui.blit(VANILLA_BG_TEXTURE, recipeX, recipeY - 1, 0, textureY, RECIPES_IMAGE_SIZE_WIDTH, RECIPES_IMAGE_SIZE_HEIGHT);
        }
    }

    private void renderRecipes(GuiGraphics gui, int recipesLeft, int recipesTop, int recipeEnd) {
        var recipes = menu.getRecipes();

        for (var recipeIndex = startIndex; recipeIndex < recipeEnd && recipeIndex < menu.getNumRecipes(); ++recipeIndex) {
            var recipeOffset = recipeIndex - startIndex;
            var recipeX = recipesLeft + recipeOffset % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH;
            var row = recipeOffset / RECIPES_COLUMNS;
            var recipeY = recipesTop + row * RECIPES_IMAGE_SIZE_HEIGHT + 2;
            gui.renderItem(recipes.get(recipeIndex).getResultItem(minecraft.level.registryAccess()), recipeX, recipeY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        scrolling = false;
        if (displayRecipes) {
            var recipesLeft = leftPos + RECIPES_X;
            var recipesTop = topPos + RECIPES_Y;
            var recipeEnd = startIndex + RECIPES_COLUMNS * RECIPES_ROWS;

            for (var recipeIndex = startIndex; recipeIndex < recipeEnd; ++recipeIndex) {
                var recipeOffset = recipeIndex - startIndex;
                var insideX = mouseX - (double) (recipesLeft + recipeOffset % RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_WIDTH);
                var insideY = mouseY - (double) (recipesTop + recipeOffset / RECIPES_COLUMNS * RECIPES_IMAGE_SIZE_HEIGHT);
                if (insideX >= 0.0D && insideY >= 0.0D
                        && insideX < RECIPES_IMAGE_SIZE_WIDTH && insideY < RECIPES_IMAGE_SIZE_HEIGHT
                        && menu.clickMenuButton(minecraft.player, recipeIndex)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, recipeIndex);
                    return true;
                }
            }

            var scrollBarLeft = leftPos + 119;
            var scrollBarTop = topPos + 9;
            if (mouseX >= (double) scrollBarLeft && mouseX < (double) (scrollBarLeft + SCROLLER_WIDTH)
                    && mouseY >= (double) scrollBarTop && mouseY < (double) (scrollBarTop + SCROLLER_FULL_HEIGHT)) {
                scrolling = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling && isScrollBarActive()) {
            var scrollTop = topPos + RECIPES_Y;
            var scrollBottom = scrollTop + SCROLLER_FULL_HEIGHT;
            scrollOffs = ((float) mouseY - (float) scrollTop - 7.5F) / ((float) (scrollBottom - scrollTop) - (float) SCROLLER_HEIGHT);
            scrollOffs = Mth.clamp(scrollOffs, 0.0F, 1.0F);
            startIndex = (int) ((double) (scrollOffs * (float) getOffscreenRows()) + 0.5D) * RECIPES_COLUMNS;
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (isScrollBarActive()) {
            var offscreenRows = getOffscreenRows();
            var scrollStep = (float) scrollDelta / (float) offscreenRows;
            scrollOffs = Mth.clamp(scrollOffs - scrollStep, 0.0F, 1.0F);
            startIndex = (int) ((double) (scrollOffs * (float) offscreenRows) + 0.5D) * RECIPES_COLUMNS;
        }

        return true;
    }

    private boolean isScrollBarActive() {
        return displayRecipes && menu.getNumRecipes() > RECIPES_COLUMNS * RECIPES_ROWS;
    }

    protected int getOffscreenRows() {
        return (menu.getNumRecipes() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS;
    }

    private void containerChanged() {
        displayRecipes = menu.hasInputItem();
        if (!displayRecipes) {
            scrollOffs = 0.0F;
            startIndex = 0;
        }
    }
}