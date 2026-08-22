package jp.aquafactory.apprenticecodex.item.curios.endergrimoire;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnderGrimoireInscriptionScreen extends AbstractContainerScreen<EnderGrimoireInscriptionMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/inscription_table.png");

    private static final int INSCRIBE_BUTTON_X = 43;
    private static final int INSCRIBE_BUTTON_Y = 35;
    private static final int SCROLL_SLOT = 36;
    private static final int SPELLBOOK_ICON_X = 17;
    private static final int SPELLBOOK_ICON_Y = 22;
    private static final int SPELL_BG_X = 67;
    private static final int SPELL_BG_Y = 15;
    private static final int SPELL_BG_WIDTH = 95;
    private static final int SPELL_BG_HEIGHT = 57;
    private static final int LORE_PAGE_X = 176;
    private static final int LORE_PAGE_WIDTH = 80;

    private boolean isDirty;
    private Button inscribeButton;
    private final List<SpellSlotInfo> spellSlots = new ArrayList<>();
    private int selectedSpellIndex = -1;
    private int inscriptionErrorCode = 0;
    private ISpellContainer lastSpellContainer;
    private final ItemStack spellbookDecorationStack = new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get());

    public EnderGrimoireInscriptionScreen(EnderGrimoireInscriptionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 256;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        inscribeButton = this.addWidget(
                Button.builder(CommonComponents.GUI_DONE, (button) -> this.onInscription()).bounds(0, 0, 14, 14).build()
        );
        generateSpellSlots();
        lastSpellContainer = menu.getSpellContainer();
    }

    @Override
    public void onClose() {
        super.onClose();
        resetSelectedSpell();
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        gui.renderItem(spellbookDecorationStack, leftPos + SPELLBOOK_ICON_X, topPos + SPELLBOOK_ICON_Y);
        gui.renderItemDecorations(font, spellbookDecorationStack, leftPos + SPELLBOOK_ICON_X, topPos + SPELLBOOK_ICON_Y);

        inscribeButton.active = isValidInscription() && inscriptionErrorCode == 0;
        renderButtons(gui, mouseX, mouseY);

        var currentContainer = menu.getSpellContainer();
        if (currentContainer != lastSpellContainer) {
            onSpellContainerChanged(currentContainer);
        }

        renderSpells(gui, mouseX, mouseY);
        renderLorePage(gui, mouseX, mouseY);

        if (menu.hasSpellContainer()) {
            inscriptionErrorCode = getErrorCode();
        } else {
            inscriptionErrorCode = 0;
        }

        if (inscriptionErrorCode > 0) {
            gui.blit(TEXTURE, leftPos + 35, topPos + 51, 0, 213, 28, 22);
            if (isHovering(leftPos + 35, topPos + 51, 28, 22, mouseX, mouseY)) {
                gui.renderTooltip(font, getErrorMessage(inscriptionErrorCode), mouseX, mouseY);
            }
        }
    }

    private int getErrorCode() {
        return 0;
    }

    private Component getErrorMessage(int code) {
        if (code == 1) {
            return Component.translatable("ui.irons_spellbooks.inscription_table_rarity_error");
        }
        return Component.empty();
    }

    private void renderSpells(GuiGraphics gui, int mouseX, int mouseY) {
        if (isDirty) {
            generateSpellSlots();
        }

        Vec2 center = new Vec2(SPELL_BG_X + leftPos + (float) SPELL_BG_WIDTH / 2, SPELL_BG_Y + topPos + (float) SPELL_BG_HEIGHT / 2);
        for (var i = 0; i < spellSlots.size(); ++i) {
            var slotInfo = spellSlots.get(i);
            var position = slotInfo.relativePosition.add(center);
            slotInfo.button.setX((int) position.x);
            slotInfo.button.setY((int) position.y);
            renderSpellSlot(gui, position, mouseX, mouseY, i, slotInfo);
        }
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY) {
        inscribeButton.setX(leftPos + INSCRIBE_BUTTON_X);
        inscribeButton.setY(topPos + INSCRIBE_BUTTON_Y);
        if (inscribeButton.active) {
            if (isHovering(inscribeButton.getX(), inscribeButton.getY(), 14, 14, mouseX, mouseY)) {
                gui.blit(TEXTURE, inscribeButton.getX(), inscribeButton.getY(), 28, 185, 14, 14);
            } else {
                gui.blit(TEXTURE, inscribeButton.getX(), inscribeButton.getY(), 14, 185, 14, 14);
            }
        } else {
            gui.blit(TEXTURE, inscribeButton.getX(), inscribeButton.getY(), 0, 185, 14, 14);
        }
    }

    private void renderSpellSlot(GuiGraphics gui, Vec2 position, int mouseX, int mouseY, int index, SpellSlotInfo slot) {
        var hovering = isHovering((int) position.x, (int) position.y, 19, 19, mouseX, mouseY);
        var iconToDraw = hovering ? 38 : slot.hasSpell() ? 19 : 0;
        gui.blit(TEXTURE, (int) position.x, (int) position.y, iconToDraw, 166, 19, 19);
        if (slot.hasSpell()) {
            drawSpellIcon(gui, position, slot);
            if (hovering && !slot.spellSlot.spellData().canRemove()) {
                gui.blit(TEXTURE, (int) position.x, (int) position.y, 76, 166, 19, 19);
            }
        }
        if (index == selectedSpellIndex) {
            gui.blit(TEXTURE, (int) position.x, (int) position.y, 57, 166, 19, 19);
        }
    }

    private void drawSpellIcon(GuiGraphics gui, Vec2 position, SpellSlotInfo slot) {
        gui.blit(slot.spellSlot.getSpell().getSpellIconResource(), (int) position.x + 2, (int) position.y + 2, 0, 0, 15, 15, 16, 16);
    }

    private void renderLorePage(GuiGraphics gui, int mouseX, int mouseY) {
        var x = leftPos + LORE_PAGE_X;
        var margin = 2;
        var textColor = Style.EMPTY.withColor(0x322c2a);
        var poseStack = gui.pose();

        var spellSelected = selectedSpellIndex >= 0 && selectedSpellIndex < spellSlots.size() && spellSlots.get(selectedSpellIndex).hasSpell();
        var title = selectedSpellIndex < 0
                ? Component.translatable("ui.irons_spellbooks.no_selection")
                : spellSelected
                ? spellSlots.get(selectedSpellIndex).spellSlot.getSpell().getDisplayName(Minecraft.getInstance().player)
                : Component.translatable("ui.irons_spellbooks.empty_slot");

        var titleLines = font.split(title.withStyle(ChatFormatting.UNDERLINE).withStyle(textColor), LORE_PAGE_WIDTH);
        var titleY = topPos + 10;

        for (FormattedCharSequence line : titleLines) {
            var titleWidth = font.width(line);
            var titleX = x + (LORE_PAGE_WIDTH - titleWidth) / 2;
            gui.drawString(font, line, titleX, titleY, 0xFFFFFF, false);

            if (spellSelected && isHovering(titleX, titleY, titleWidth, font.lineHeight, mouseX, mouseY)) {
                gui.renderTooltip(font, TooltipsUtils.createSpellDescriptionTooltip(spellSlots.get(selectedSpellIndex).spellSlot.getSpell(), font), mouseX, mouseY);
            }
            titleY += font.lineHeight;
        }

        var descLine = titleY + 4;
        if (selectedSpellIndex < 0 || selectedSpellIndex >= spellSlots.size() || !spellSlots.get(selectedSpellIndex).hasSpell()) {
            return;
        }

        var colorMana = Style.EMPTY.withColor(0x0044a9);
        var colorCooldown = Style.EMPTY.withColor(0x115511);
        var spell = spellSlots.get(selectedSpellIndex).spellSlot.getSpell();
        var spellLevel = spellSlots.get(selectedSpellIndex).spellSlot.getLevel();
        var textScale = 1f;
        var reverseScale = 1 / textScale;

        Component school = spell.getSchoolType().getDisplayName();
        poseStack.scale(textScale, textScale, textScale);

        drawTextWithShadow(font, gui, school, x + (LORE_PAGE_WIDTH - font.width(school.getString())) / 2, descLine, 0xFFFFFF, 1);
        descLine += (int) (font.lineHeight * textScale);

        var levelText = Component.translatable("ui.irons_spellbooks.level", spellLevel).withStyle(textColor);
        gui.drawString(font, levelText, x + (LORE_PAGE_WIDTH - font.width(levelText.getString())) / 2, descLine, 0xFFFFFF, false);
        descLine += (int) (font.lineHeight * textScale * 2);

        descLine += drawStatText(font, gui, x + margin, descLine, "ui.irons_spellbooks.mana_cost", textColor, Component.translatable(spell.getManaCost(spellLevel) + ""), colorMana, textScale);
        descLine += drawText(font, gui, TooltipsUtils.getCastTimeComponent(spell.getCastType(), Utils.timeFromTicks(spell.getEffectiveCastTime(spellLevel, null), 1)), x + margin, descLine, textColor.getColor().getValue(), textScale);
        descLine += drawStatText(font, gui, x + margin, descLine, "ui.irons_spellbooks.cooldown", textColor, Component.translatable(Utils.timeFromTicks(spell.getSpellCooldown(), 1)), colorCooldown, textScale);

        for (MutableComponent component : spell.getUniqueInfo(spellLevel, null)) {
            descLine += drawText(font, gui, component, x + margin, descLine, textColor.getColor().getValue(), 1);
        }

        poseStack.scale(reverseScale, reverseScale, reverseScale);
    }

    private void drawTextWithShadow(Font font, GuiGraphics gui, Component text, int x, int y, int color, float scale) {
        x /= (int) scale;
        y /= (int) scale;
        gui.drawString(font, text, x, y, color);
    }

    private int drawText(Font font, GuiGraphics gui, Component text, int x, int y, int color, float scale) {
        x /= (int) scale;
        y /= (int) scale;
        gui.drawWordWrap(font, text, x, y, LORE_PAGE_WIDTH, color);
        return font.wordWrapHeight(text, LORE_PAGE_WIDTH);
    }

    private int drawStatText(Font font, GuiGraphics gui, int x, int y, String translationKey, Style textStyle, MutableComponent stat, Style statStyle, float scale) {
        return drawText(font, gui, Component.translatable(translationKey, stat.withStyle(statStyle)).withStyle(textStyle), x, y, 0xFFFFFF, scale);
    }

    private void generateSpellSlots() {
        for (SpellSlotInfo slot : spellSlots) {
            removeWidget(slot.button);
        }
        spellSlots.clear();

        if (!menu.hasSpellContainer()) {
            return;
        }

        var spellContainer = menu.getSpellContainer();
        var storedSpells = spellContainer.getAllSpells();
        int spellCount = Math.min(spellContainer.getMaxSpellCount(), 15);
        if (spellCount <= 0) {
            return;
        }

        int boxSize = 19;
        int[] rowCounts = ClientRenderCache.getRowCounts(spellCount);
        int[] row1 = new int[rowCounts[0]];
        int[] row2 = new int[rowCounts[1]];
        int[] row3 = new int[rowCounts[2]];
        int[] rowWidth = {
                boxSize * row1.length,
                boxSize * row2.length,
                boxSize * row3.length
        };
        int[] rowHeight = {
                row1.length > 0 ? boxSize : 0,
                row2.length > 0 ? boxSize : 0,
                row3.length > 0 ? boxSize : 0
        };

        int[][] display = {row1, row2, row3};
        int overallHeight = rowHeight[0] + rowHeight[1] + rowHeight[2];
        int index = 0;
        for (int row = 0; row < display.length; row++) {
            for (int column = 0; column < display[row].length; column++) {
                int offset = -rowWidth[row] / 2;
                Vec2 location = new Vec2(offset + column * boxSize, (row) * boxSize - ((float) overallHeight / 2));
                location.add(-9);
                int slotIndex = index;
                spellSlots.add(new SpellSlotInfo(
                        storedSpells[index],
                        location,
                        this.addWidget(
                                Button.builder(Component.translatable(Integer.toString(slotIndex)), (button) -> this.setSelectedIndex(slotIndex))
                                        .pos((int) location.x, (int) location.y)
                                        .size(boxSize, boxSize)
                                        .build()
                        )
                ));
                index++;
            }
        }

        isDirty = false;
    }

    private void onSpellContainerChanged(ISpellContainer currentContainer) {
        isDirty = true;
        lastSpellContainer = currentContainer;
        if (currentContainer.getMaxSpellCount() <= selectedSpellIndex) {
            resetSelectedSpell();
        }
    }

    private void onInscription() {
        if (!isValidInscription() || spellSlots.isEmpty()) {
            return;
        }

        if (selectedSpellIndex < 0 || spellSlots.get(selectedSpellIndex).hasSpell()) {
            for (int i = selectedSpellIndex + 1; i < spellSlots.size(); i++) {
                if (!spellSlots.get(i).hasSpell()) {
                    setSelectedIndex(i);
                    break;
                }
            }
        }

        setSelectedIndex(Mth.clamp(selectedSpellIndex, 0, spellSlots.size() - 1));
        if (spellSlots.get(selectedSpellIndex).hasSpell()) {
            return;
        }

        isDirty = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F));
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, EnderGrimoireInscriptionMenu.INSCRIBE_BUTTON_ID);
        }
    }

    private void setSelectedIndex(int index) {
        selectedSpellIndex = index;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
    }

    private void resetSelectedSpell() {
        setSelectedIndex(-1);
    }

    private boolean isValidInscription() {
        return menu.slots.get(SCROLL_SLOT).hasItem() && menu.hasScrollSlotted();
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private record SpellSlotInfo(SpellSlot spellSlot, Vec2 relativePosition, Button button) {
        public boolean hasSpell() {
            return spellSlot != null && !spellSlot.spellData().equals(SpellData.EMPTY);
        }
    }
}
