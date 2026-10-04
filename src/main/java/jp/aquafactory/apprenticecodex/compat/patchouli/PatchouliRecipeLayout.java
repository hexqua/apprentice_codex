package jp.aquafactory.apprenticecodex.compat.patchouli;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    static final int SMOKER_ROW_HEIGHT = 24;
    static final int WORKBENCH_ROW_HEIGHT = 50;
    static final int TEXT_GAP = 5;

    private PatchouliRecipeLayout() {
    }

    static Component resolveTitle(String configured, UnaryOperator<IVariable> lookup) {
        var value = lookup.apply(IVariable.wrap(configured));
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
