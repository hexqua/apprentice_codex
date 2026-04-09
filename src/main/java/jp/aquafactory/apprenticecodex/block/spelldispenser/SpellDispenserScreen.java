package jp.aquafactory.apprenticecodex.block.spelldispenser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserScreen extends AbstractContainerScreen<SpellDispenserMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/dispenser.png");

    public SpellDispenserScreen(SpellDispenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        super.renderLabels(gui, mouseX, mouseY);

        if (minecraft == null || minecraft.player == null) {
            return;
        }

        var validation = menu.getValidation(minecraft.player);
        var currentSpell = validation.getCurrentSpellLabel(minecraft.player);
        var status = validation.getStatus(minecraft.player);
        if (currentSpell != null) {
            gui.drawString(font, font.plainSubstrByWidth(currentSpell.getString(), 160), 8, 18, 0x404040, false);
        } else {
            gui.drawString(font,
                    font.plainSubstrByWidth(Component.translatable("container.apprenticecodex.spell_dispenser.current_spell.none").getString(), 160),
                    8,
                    18,
                    0x606060,
                    false);
        }

        gui.drawString(
                font,
                font.plainSubstrByWidth(status.getString(), 160),
                8,
                30,
                validation.isSupported() ? 0x208040 : 0xAA3030,
                false
        );
    }
}
