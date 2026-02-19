package jp.aquafactory.apprenticecodex.block.apprenticestable;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ApprenticesTableScreen extends AbstractContainerScreen<ApprenticesTableMenu> {
    private static final ResourceLocation APPRENTICES_TABLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/apprentices_table.png");
    private static final int SPELL_LIST_X = 46;
    private static final int SPELL_LIST_Y = 15;
    private static final int SCROLL_BAR_X = 156;
    private static final int SCROLL_BAR_Y = 15;
    private static final int SCROLL_BAR_WIDTH = 12;
    private static final int SCROLL_BAR_HEIGHT = 56;

    private final List<SpellSelectInfo> availableSpells = new ArrayList<>();
    private int scrollOffset;
    private boolean isScrollbarHeld;

    public ApprenticesTableScreen(ApprenticesTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        generateSpellList();
    }

    @Override
    public void onClose() {
        resetList();
        super.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(APPRENTICES_TABLE_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (shouldShowSpellList()) {
            var normalizedScrollOffset = totalRowCount() > 3
                    ? Mth.clamp((float) this.scrollOffset / (totalRowCount() - 3), 0, 1)
                    : 0.0F;
            gui.blit(
                    APPRENTICES_TABLE_TEXTURE,
                    leftPos + SCROLL_BAR_X,
                    (int) (topPos + SCROLL_BAR_Y + normalizedScrollOffset * (SCROLL_BAR_HEIGHT - 15)),
                    imageWidth + (isScrollbarHeld ? 12 : 0),
                    0,
                    12,
                    15
            );
        }
        renderSpellList(gui, mouseX, mouseY);
    }

    private void renderSpellList(GuiGraphics guiHelper, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (!shouldShowSpellList()) {
            isScrollbarHeld = false;
            for (var spellData : availableSpells) {
                spellData.button.active = false;
            }
            return;
        }

        List<FormattedCharSequence> additionalTooltip = null;
        for (var i = 0; i < availableSpells.size(); ++i) {
            var spellData = availableSpells.get(i);

            if (i - scrollOffset >= 0 && i - scrollOffset < 3) {
                var x = leftPos + SPELL_LIST_X;
                var y = topPos + SPELL_LIST_Y + (i - scrollOffset) * 19;
                spellData.button.setX(x);
                spellData.button.setY(y);
                spellData.draw(this, guiHelper, minecraft.player, x, y);
                if (additionalTooltip == null) {
                    additionalTooltip = spellData.getTooltip(x, y, minecraft.player, mouseX, mouseY);
                }
                spellData.button.active = true;
            } else {
                spellData.button.active = false;
            }
        }

        if (additionalTooltip != null) {
            guiHelper.renderTooltip(font, additionalTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!shouldShowSpellList()) {
            return super.mouseScrolled(mouseX, mouseY, scrollY);
        }
        var maxOffset = Math.max(availableSpells.size() - 3, 0);
        var newScroll = Mth.clamp(scrollOffset - (int) scrollY, 0, maxOffset);
        if (newScroll != scrollOffset) {
            scrollOffset = newScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        isScrollbarHeld = shouldShowSpellList()
                && isHovering(SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrollbarHeld = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!shouldShowSpellList()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        var maxOffset = totalRowCount() - 3;
        if (isScrollbarHeld && maxOffset > 0) {
            var barStartY = topPos + SCROLL_BAR_Y;
            var barEndY = barStartY + SCROLL_BAR_HEIGHT;
            var normalized = ((float) mouseY - (float) barStartY - 7.5F) / ((float) (barEndY - barStartY) - 15.0F);
            normalized = Mth.clamp(normalized, 0.0F, 1.0F);
            scrollOffset = Math.max((int) ((double) (normalized * (float) maxOffset) + 0.5D), 0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private int totalRowCount() {
        return availableSpells.size();
    }

    private void selectSpell(int index) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        if (!shouldShowSpellList()) {
            return;
        }

        if (menu.clickMenuButton(minecraft.player, index)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    private boolean shouldShowSpellList() {
        return menu.hasInputItem();
    }

    public void generateSpellList() {
        resetList();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        var spells = menu.getAvailableSpells();
        for (var i = 0; i < spells.size(); ++i) {
            var spell = spells.get(i);
            if (!spell.isEnabled()) {
                continue;
            }
            var index = i;
            availableSpells.add(new SpellSelectInfo(
                    spell,
                    1,
                    i,
                    this.addWidget(
                            new Button.Builder(spell.getDisplayName(minecraft.player), b -> selectSpell(index))
                                    .pos(0, 0)
                                    .size(108, 19)
                                    .build()
                    )
            ));
        }
    }

    private void resetList() {
        scrollOffset = 0;
        for (var s : availableSpells) {
            removeWidget(s.button);
        }
        availableSpells.clear();
    }

    private class SpellSelectInfo {
        AbstractSpell spell;
        int spellLevel;
        SpellRarity rarity;
        Button button;
        int index;

        SpellSelectInfo(AbstractSpell spell, int spellLevel, int index, Button button) {
            this.spell = spell;
            this.spellLevel = spellLevel;
            this.index = index;
            this.button = button;
            this.rarity = spell.getRarity(spellLevel);
        }

        void draw(ApprenticesTableScreen screen, GuiGraphics guiHelper, Player player, int x, int y) {
            if (index == screen.menu.getSelectedRecipeIndex()) {
                guiHelper.blit(APPRENTICES_TABLE_TEXTURE, x, y, 0, 204, 108, 19);
            } else {
                guiHelper.blit(APPRENTICES_TABLE_TEXTURE, x, y, 0, 166, 108, 19);
            }

            var texture = spell.getSpellIconResource();
            guiHelper.blit(texture, x + 108 - 18, y + 1, 0, 0, 16, 16, 16, 16);

            var maxWidth = 108 - 20;
            var text = trimText(font, getDisplayName(player).withStyle(Style.EMPTY), maxWidth);
            var textX = x + 2;
            var textY = y + 3;
            guiHelper.drawWordWrap(font, text, textX, textY, maxWidth, 0xFFFFFF);
        }

        @Nullable
        List<FormattedCharSequence> getTooltip(int x, int y, Player player, int mouseX, int mouseY) {
            var text = getDisplayName(player);
            var textX = x + 2;
            var textY = y + 3;
            if (mouseX >= textX && mouseY >= textY && mouseX < textX + font.width(text) && mouseY < textY + font.lineHeight) {
                return TooltipsUtils.createSpellDescriptionTooltip(this.spell, font);
            } else {
                return null;
            }
        }

        private FormattedText trimText(Font font, Component component, int maxWidth) {
            var text = font.getSplitter().splitLines(component, maxWidth, component.getStyle()).get(0);
            if (text.getString().length() < component.getString().length()) {
                text = FormattedText.composite(text, FormattedText.of("..."));
            }
            return text;
        }

        MutableComponent getDisplayName(Player player) {
            return spell.getDisplayName(player);
        }
    }
}
