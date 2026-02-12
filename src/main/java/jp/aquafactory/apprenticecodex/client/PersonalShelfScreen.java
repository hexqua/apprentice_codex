package jp.aquafactory.apprenticecodex.client;

import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class PersonalShelfScreen extends AbstractContainerScreen<PersonalInventoryMenu> {
    // todo:専用のGUIを作る(今はバニラのラージチェストを流用)
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public PersonalShelfScreen(PersonalInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // todo:テクスチャ作ったら合わせる.
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        var x = (width - imageWidth) / 2;
        var y = (height - imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }
}
