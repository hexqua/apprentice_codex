package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.util.TooltipsUtils;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SpellDispenserScreen extends AbstractContainerScreen<SpellDispenserMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/spell_dispenser.png");
    private static final ResourceLocation RUNIC_FONT = ResourceLocation.withDefaultNamespace("illageralt");
    private static final Component FLASK_LABEL =
            Component.translatable("container.apprenticecodex.spell_dispenser.flask");
    private static final Component OWNER_MISSING_TOOLTIP =
            Component.translatable("container.apprenticecodex.spell_dispenser.spell.tooltip.owner_missing");
    private static final Component HIDDEN_SPELL_LABEL =
            Component.literal("sealed sigil").withStyle(style -> style.withFont(RUNIC_FONT));

    private static final int SPELL_ENTRY_X = 35;
    private static final int SPELL_ENTRY_Y = 17;
    private static final int SPELL_ENTRY_WIDTH = 108;
    private static final int SPELL_ENTRY_HEIGHT = 19;
    private static final int SPELL_READY_V = 166;
    private static final int SPELL_ERROR_V = 185;

    private static final int MANA_BAR_X = 12;
    private static final int MANA_BAR_Y = 18;
    private static final int MANA_BAR_WIDTH = 8;
    private static final int MANA_BAR_HEIGHT = 52;
    private static final int MANA_BAR_U = 0;
    private static final int MANA_BAR_V = 204;
    public SpellDispenserScreen(SpellDispenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderDisplayedMana(gui);
        renderSpellEntry(gui);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        gui.drawString(font, FLASK_LABEL, SpellDispenserMenu.FLASK_SLOT_X, SpellDispenserMenu.FLASK_SLOT_Y - 11, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        if (isHoveringManaBar(mouseX, mouseY)) {
            gui.renderTooltip(
                    font,
                    Component.translatable(
                            "container.apprenticecodex.spell_dispenser.mana.tooltip",
                            menu.getCurrentMana(),
                            menu.getMaxMana()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (minecraft != null && minecraft.player != null && isHoveringSpellEntry(mouseX, mouseY)) {
            var validation = menu.getValidation(minecraft.player);
            if (validation.isSupported()) {
                gui.renderTooltip(font, TooltipsUtils.createSpellDescriptionTooltip(validation.spellData().getSpell(), font), mouseX, mouseY);
                return;
            }
        }

        var spellPresentation = resolveSpellPresentation();
        if (spellPresentation != null && spellPresentation.tooltip() != null && isHoveringSpellEntry(mouseX, mouseY)) {
            gui.renderTooltip(font, List.of(spellPresentation.tooltip().getVisualOrderText()), mouseX, mouseY);
            return;
        }

        super.renderTooltip(gui, mouseX, mouseY);
    }

    private void renderDisplayedMana(GuiGraphics gui) {
        var filledHeight = Mth.clamp(
                (int) Math.floor((double) menu.getCurrentMana() * MANA_BAR_HEIGHT / Math.max(1, menu.getMaxMana())),
                0,
                MANA_BAR_HEIGHT
        );
        if (filledHeight <= 0) {
            return;
        }

        var sourceY = MANA_BAR_V + (MANA_BAR_HEIGHT - filledHeight);
        var drawY = topPos + MANA_BAR_Y + (MANA_BAR_HEIGHT - filledHeight);
        gui.blit(TEXTURE, leftPos + MANA_BAR_X, drawY, MANA_BAR_U, sourceY, MANA_BAR_WIDTH, filledHeight);
    }

    private void renderSpellEntry(GuiGraphics gui) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        var presentation = resolveSpellPresentation();
        if (presentation == null) {
            return;
        }
        var entryX = leftPos + SPELL_ENTRY_X;
        var entryY = topPos + SPELL_ENTRY_Y;
        gui.blit(TEXTURE, entryX, entryY, 0, presentation.hidden() ? SPELL_ERROR_V : SPELL_READY_V, SPELL_ENTRY_WIDTH, SPELL_ENTRY_HEIGHT);
        gui.blit(presentation.icon(), entryX + SPELL_ENTRY_WIDTH - 18, entryY + 1, 0, 0, 16, 16, 16, 16);

        var text = trimText(font, presentation.label(), SPELL_ENTRY_WIDTH - 20);
        gui.drawWordWrap(font, text, entryX + 2, entryY + 3, SPELL_ENTRY_WIDTH - 20, 0xFFFFFF);
    }

    private @Nullable SpellPresentation resolveSpellPresentation() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }

        if (menu.getSpellSource().isEmpty()) {
            return null;
        }

        var player = minecraft.player;
        var validation = menu.getValidation(player);
        var hidden = !menu.hasOwnerProfile() || validation.shouldUseHiddenPresentation();
        var tooltip = !menu.hasOwnerProfile() ? OWNER_MISSING_TOOLTIP : validation.getGuiTooltip();

        if (hidden) {
            return new SpellPresentation(HIDDEN_SPELL_LABEL, SpellRegistry.none().getSpellIconResource(), true, tooltip);
        }

        var hasSpell = validation.spellData() != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY;
        var label = hasSpell
                ? validation.spellData().getSpell().getDisplayName(player)
                : menu.getSpellSource().getHoverName();
        var icon = hasSpell ? validation.spellData().getSpell().getSpellIconResource() : SpellRegistry.none().getSpellIconResource();
        return new SpellPresentation(label, icon, false, tooltip);
    }

    private boolean isHoveringManaBar(double mouseX, double mouseY) {
        return isHovering(MANA_BAR_X, MANA_BAR_Y, MANA_BAR_WIDTH, MANA_BAR_HEIGHT, mouseX, mouseY);
    }

    private boolean isHoveringSpellEntry(double mouseX, double mouseY) {
        return isHovering(SPELL_ENTRY_X, SPELL_ENTRY_Y, SPELL_ENTRY_WIDTH, SPELL_ENTRY_HEIGHT, mouseX, mouseY);
    }

    private FormattedText trimText(Font font, Component component, int maxWidth) {
        var lines = font.getSplitter().splitLines(component, maxWidth, component.getStyle());
        if (lines.isEmpty()) {
            return FormattedText.EMPTY;
        }

        var text = lines.get(0);
        if (text.getString().length() < component.getString().length()) {
            text = FormattedText.composite(text, FormattedText.of("..."));
        }
        return text;
    }

    private record SpellPresentation(
            Component label,
            ResourceLocation icon,
            boolean hidden,
            @Nullable Component tooltip
    ) {
    }
}
