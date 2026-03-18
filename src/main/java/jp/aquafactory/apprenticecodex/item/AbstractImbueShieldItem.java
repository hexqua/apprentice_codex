package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractImbueShieldItem extends ShieldItem implements IPresetSpellContainer, RestrictedSpellImbuableItem {
    protected AbstractImbueShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendImbueTargetSpellTooltip(lines);
    }

    @Override
    public final boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        return SpellGunCastType.from(spell.getCastType()) != null
                && spell.getRecastCount(spellLevel, null) <= 0;
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        SpellData spellData = SpellData.EMPTY;
        if (ISpellContainer.isSpellContainer(stack)) {
            var spellContainer = ISpellContainer.get(stack);
            if (spellContainer != null && spellContainer.getActiveSpellCount() > 0) {
                spellData = spellContainer.getSpellAtIndex(0);
            }
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    private static void appendImbueTargetSpellTooltip(List<Component> lines) {
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }

        if (!Screen.hasShiftDown()) {
            lines.add(Component.translatable("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        appendTooltipSection(
                lines,
                List.of(Component.translatable("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none")
                        .withStyle(ChatFormatting.GRAY)),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title"
        );
        appendTooltipSection(
                lines,
                collectRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title"
        );
    }

    private static void appendTooltipSection(List<Component> lines, List<Component> sectionLines, String titleTranslationKey) {
        lines.add(Component.translatable(titleTranslationKey).withStyle(ChatFormatting.GOLD));
        lines.addAll(sectionLines);
    }

    private static List<Component> collectRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_not_continuous"
        ).withStyle(ChatFormatting.GRAY));
        translatedLines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_no_recast"
        ).withStyle(ChatFormatting.GRAY));
        return translatedLines;
    }
}
