package jp.aquafactory.apprenticecodex.block.apprenticedesk;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
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

public class ApprenticeDeskScreen extends AbstractContainerScreen<ApprenticeDeskMenu> {
    private static final ResourceLocation APPRENTICE_DESK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/apprentice_desk.png");
    private static final int SPELL_LIST_X = 89;
    private static final int SPELL_LIST_Y = 15;
    private static final int SCROLL_BAR_X = 199;
    private static final int SCROLL_BAR_Y = 15;
    private static final int SCROLL_BAR_WIDTH = 12;
    private static final int SCROLL_BAR_HEIGHT = 56;
    private static final int VISIBLE_SPELL_COUNT = 3;
    private static final ResourceLocation RUNIC_FONT = ResourceLocation.withDefaultNamespace("illageralt");

    private final List<SpellSelectInfo> availableSpells = new ArrayList<>();
    private int scrollOffset;
    private boolean isScrollbarHeld;

    public ApprenticeDeskScreen(ApprenticeDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 218;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        menu.slotUpdateListener = this::generateSpellList;
        generateSpellList();
    }

    @Override
    public void onClose() {
        menu.slotUpdateListener = () -> {};
        resetList();
        super.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty()
                && hoveredSlot != null
                && hoveredSlot.hasItem()
                && hoveredSlot.index == ApprenticeDeskMenu.INK_SLOT) {
            var stack = hoveredSlot.getItem();
            var conversionTooltip = ApprenticeDeskInkTooltip.create(stack);
            if (conversionTooltip != null) {
                var lines = new ArrayList<>(getTooltipFromContainerItem(stack));
                lines.add(conversionTooltip);
                gui.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(APPRENTICE_DESK_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (shouldShowSpellList()) {
            var maxOffset = Math.max(totalRowCount() - VISIBLE_SPELL_COUNT, 0);
            var normalizedScrollOffset = maxOffset > 0
                    ? Mth.clamp((float) scrollOffset / maxOffset, 0, 1)
                    : 0.0F;
            gui.blit(
                    APPRENTICE_DESK_TEXTURE,
                    leftPos + SCROLL_BAR_X,
                    (int) (topPos + SCROLL_BAR_Y
                            + normalizedScrollOffset * (SCROLL_BAR_HEIGHT - 15)),
                    imageWidth + (isScrollbarHeld ? 12 : 0),
                    0,
                    12,
                    15
            );
        }
        renderSpellList(gui, mouseX, mouseY);
    }

    private void renderSpellList(GuiGraphics gui, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (!shouldShowSpellList()) {
            isScrollbarHeld = false;
            availableSpells.forEach(spellInfo -> spellInfo.button.active = false);
            return;
        }

        List<FormattedCharSequence> additionalTooltip = null;
        for (var i = 0; i < availableSpells.size(); ++i) {
            var spellInfo = availableSpells.get(i);
            var visibleIndex = i - scrollOffset;
            if (visibleIndex >= 0 && visibleIndex < VISIBLE_SPELL_COUNT) {
                spellInfo.updateActivityState(minecraft.player);
                var x = leftPos + SPELL_LIST_X;
                var y = topPos + SPELL_LIST_Y + visibleIndex * 19;
                spellInfo.button.setX(x);
                spellInfo.button.setY(y);
                spellInfo.draw(this, gui, minecraft.player, x, y);
                if (additionalTooltip == null) {
                    additionalTooltip = spellInfo.getTooltip(x, y, minecraft.player, mouseX, mouseY);
                }
                spellInfo.button.active = spellInfo.activityState == ActivityState.ENABLED;
            } else {
                spellInfo.activityState = ActivityState.DISABLED;
                spellInfo.button.active = false;
            }
        }

        if (additionalTooltip != null) {
            gui.renderTooltip(font, additionalTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!shouldShowSpellList()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        var maxOffset = Math.max(availableSpells.size() - VISIBLE_SPELL_COUNT, 0);
        var newScroll = Mth.clamp(scrollOffset - (int) scrollY, 0, maxOffset);
        if (newScroll != scrollOffset) {
            scrollOffset = newScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

        var maxOffset = totalRowCount() - VISIBLE_SPELL_COUNT;
        if (isScrollbarHeld && maxOffset > 0) {
            var barStartY = topPos + SCROLL_BAR_Y;
            var barEndY = barStartY + SCROLL_BAR_HEIGHT;
            var normalized = ((float) mouseY - barStartY - 7.5F)
                    / ((float) (barEndY - barStartY) - 15.0F);
            normalized = Mth.clamp(normalized, 0.0F, 1.0F);
            scrollOffset = Math.max((int) (normalized * maxOffset + 0.5D), 0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private int totalRowCount() {
        return availableSpells.size();
    }

    private void selectSpell(int index) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null
                || !shouldShowSpellList()) {
            return;
        }

        if (menu.clickMenuButton(minecraft.player, index)) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F)
            );
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    private boolean shouldShowSpellList() {
        return menu.hasAllInputs();
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
                    this.addWidget(new Button.Builder(
                            spell.getDisplayName(minecraft.player),
                            button -> selectSpell(index)
                    ).pos(0, 0).size(108, 19).build())
            ));
        }
    }

    private void resetList() {
        scrollOffset = 0;
        for (var spellInfo : availableSpells) {
            removeWidget(spellInfo.button);
        }
        availableSpells.clear();
    }

    private enum ActivityState {
        DISABLED,
        ENABLED,
        INK_ERROR,
        UNLEARNED_ERROR
    }

    private class SpellSelectInfo {
        private final AbstractSpell spell;
        private final int spellLevel;
        private final SpellRarity rarity;
        private final Button button;
        private final int index;
        private ActivityState activityState = ActivityState.DISABLED;

        SpellSelectInfo(AbstractSpell spell, int spellLevel, int index, Button button) {
            this.spell = spell;
            this.spellLevel = spellLevel;
            this.index = index;
            this.button = button;
            this.rarity = spell.getRarity(spellLevel);
        }

        void updateActivityState(Player player) {
            if (!menu.canInkCraft(spell)) {
                activityState = ActivityState.INK_ERROR;
            } else if (!spell.canBeCraftedBy(player)) {
                activityState = ActivityState.UNLEARNED_ERROR;
            } else {
                activityState = ActivityState.ENABLED;
            }
        }

        void draw(
                ApprenticeDeskScreen screen,
                GuiGraphics gui,
                Player player,
                int x,
                int y
        ) {
            if (activityState == ActivityState.ENABLED || activityState == ActivityState.UNLEARNED_ERROR) {
                gui.blit(
                        APPRENTICE_DESK_TEXTURE,
                        x,
                        y,
                        0,
                        index == screen.menu.getSelectedRecipeIndex() ? 204 : 166,
                        108,
                        19
                );
            } else {
                gui.blit(APPRENTICE_DESK_TEXTURE, x, y, 0, 185, 108, 19);
            }

            var texture = activityState == ActivityState.ENABLED
                    ? spell.getSpellIconResource()
                    : SpellRegistry.none().getSpellIconResource();
            gui.blit(texture, x + 90, y + 1, 0, 0, 16, 16, 16, 16);

            var maxWidth = 88;
            var style = activityState == ActivityState.ENABLED
                    ? Style.EMPTY
                    : Style.EMPTY.withFont(RUNIC_FONT);
            var text = trimText(font, getDisplayName(player).withStyle(style), maxWidth);
            gui.drawWordWrap(font, text, x + 2, y + 3, maxWidth, 0xFFFFFF);
        }

        @Nullable
        List<FormattedCharSequence> getTooltip(int x, int y, Player player, int mouseX, int mouseY) {
            var text = getDisplayName(player);
            var textX = x + 2;
            var textY = y + 3;
            if (mouseX < textX || mouseY < textY
                    || mouseX >= textX + font.width(text)
                    || mouseY >= textY + font.lineHeight) {
                return null;
            }

            if (activityState == ActivityState.INK_ERROR) {
                return List.of(FormattedCharSequence.forward(
                        Component.translatable("ui.irons_spellbooks.ink_rarity_error").getString(),
                        Style.EMPTY
                ));
            }
            if (activityState == ActivityState.UNLEARNED_ERROR) {
                return List.of(FormattedCharSequence.forward(
                        spell.getLockedMessage().getString(),
                        spell.getLockedMessage().getStyle()
                ));
            }
            return TooltipsUtils.createSpellDescriptionTooltip(spell, font);
        }

        private FormattedText trimText(Font font, Component component, int maxWidth) {
            var text = font.getSplitter().splitLines(component, maxWidth, component.getStyle()).getFirst();
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
