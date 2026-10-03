package jp.aquafactory.apprenticecodex.compat.patchouli;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

final class PatchouliRecipeLayout {
    static final int HEADER_HEIGHT = 12;
    static final int PAGE_WIDTH = 116;
    static final int SMOKER_ROW_HEIGHT = 32;
    static final int WORKBENCH_ROW_HEIGHT = 52;
    static final int TEXT_GAP = 5;
    static final int SLOT_SIZE = 24;
    static final int SLOT_PADDING = 4;
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 256;

    private PatchouliRecipeLayout() {
    }

    static int centeredX(int componentX, int width) {
        return componentX + (PAGE_WIDTH - width) / 2;
    }

    static void drawSlot(GuiGraphics graphics, IComponentRenderContext context, int x, int y) {
        drawTexture(graphics, context, x, y, 11, 71, SLOT_SIZE, SLOT_SIZE);
    }

    static void drawArrow(GuiGraphics graphics, IComponentRenderContext context, int x, int y) {
        drawTexture(graphics, context, x, y, 38, 79, 9, 9);
    }

    private static void drawTexture(GuiGraphics graphics, IComponentRenderContext context,
                                    int x, int y, int u, int v, int width, int height) {
        // 標準ページと同じ描画条件で、本ごとのcrafting textureと透明な枠を利用する。
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(context.getCraftingTexture(), x, y, u, v, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    static Component resolveTitle(String configured, UnaryOperator<IVariable> lookup,
                                  HolderLookup.Provider registries) {
        var value = lookup.apply(IVariable.wrap(configured, registries));
        if (value.unwrap().isJsonPrimitive()) {
            var text = value.asString("");
            return text.isBlank() ? Component.empty() : Component.translatable(text);
        }
        return value.unwrap().isJsonNull() ? Component.empty() : value.as(Component.class);
    }

    static Layout create(List<ItemStack> outputs, Component customTitle, int rowHeight) {
        var rows = new ArrayList<Row>(outputs.size());
        Component previousTitle = Component.empty();
        var y = 0;
        for (var index = 0; index < outputs.size(); ++index) {
            var output = outputs.get(index);
            var title = output.isEmpty() ? Component.empty() : output.getHoverName();
            if (!customTitle.getString().isEmpty()) {
                title = index == 0 ? customTitle : Component.empty();
            }
            var duplicate = title.equals(previousTitle);
            previousTitle = title;
            if (duplicate) {
                title = Component.empty();
            }
            var titleY = y;
            if (!title.getString().isEmpty()) {
                y += HEADER_HEIGHT;
            }
            rows.add(new Row(title, titleY, y));
            y += rowHeight;
        }
        return new Layout(List.copyOf(rows), Math.max(y, rowHeight) + TEXT_GAP);
    }

    static void renderHeader(GuiGraphics graphics, IComponentRenderContext context,
                             Component title, int x, int y, int mouseX, int mouseY) {
        if (title.getString().isEmpty()) {
            return;
        }
        var font = Minecraft.getInstance().font;
        var styledTitle = title.copy().withStyle(context.getFont());
        var width = font.width(styledTitle);
        var scale = Math.min(1F, (float) PAGE_WIDTH / Math.max(1, width));
        // 長い翻訳名もページ幅に収め、完成品の正式名はホバーで確認できるようにする。
        graphics.pose().pushPose();
        graphics.pose().translate(x + PAGE_WIDTH / 2F, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, styledTitle, -width / 2, 0, context.getHeaderColor(), false);
        graphics.pose().popPose();
        if (scale < 1 && context.isAreaHovered(mouseX, mouseY, x, y, PAGE_WIDTH, HEADER_HEIGHT)) {
            context.setHoverTooltipComponents(List.of(title));
        }
    }

    record Row(Component title, int titleY, int recipeY) {
    }

    record Layout(List<Row> rows, int textY) {
    }
}
