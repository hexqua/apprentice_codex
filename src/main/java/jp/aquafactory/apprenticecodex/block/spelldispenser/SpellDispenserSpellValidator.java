package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserSpellValidator {
    private SpellDispenserSpellValidator() {
    }

    public static boolean isPlaceableScroll(ItemStack stack) {
        return stack.getItem() instanceof Scroll;
    }

    public static ValidationResult validate(ItemStack stack) {
        if (stack.isEmpty()) {
            return new ValidationResult(ItemStack.EMPTY, SpellData.EMPTY, FailureReason.EMPTY);
        }

        if (!(stack.getItem() instanceof Scroll)) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NOT_SCROLL);
        }

        if (!ISpellContainer.isSpellContainer(stack)) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NOT_SPELL_CONTAINER);
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NOT_SPELL_CONTAINER);
        }

        var activeSpells = spellContainer.getActiveSpells().stream()
                .map(SpellSlot::spellData)
                .filter(spellData -> spellData != SpellData.EMPTY)
                .toList();
        if (activeSpells.isEmpty()) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NO_ACTIVE_SPELL);
        }
        if (activeSpells.size() != 1) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.MULTIPLE_ACTIVE_SPELLS);
        }

        var spellData = activeSpells.get(0);
        var spell = spellData.getSpell();
        if (spell == null || spell.getSpellResource() == null) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NO_ACTIVE_SPELL);
        }

        if (!ApprenticeCodexServerConfig.spellDispenserEnable()) {
            return new ValidationResult(stack, spellData, FailureReason.SERVER_DISABLED);
        }
        if (!ApprenticeCodexServerConfig.isSpellDispenserSpellAllowedByServerAllowlist(spell.getSpellResource())) {
            return new ValidationResult(stack, spellData, FailureReason.NOT_ALLOWLISTED);
        }

        var castType = spell.getCastType();
        if (castType != CastType.INSTANT && castType != CastType.LONG && castType != CastType.CONTINUOUS) {
            return new ValidationResult(stack, spellData, FailureReason.UNSUPPORTED_CAST_TYPE);
        }
        if (spell.getRecastCount(spellData.getLevel(), null) > 0) {
            return new ValidationResult(stack, spellData, FailureReason.HAS_RECAST);
        }
        if (SpellDispenserSpellProfileManager.getProfile(spell).isEmpty()) {
            return new ValidationResult(stack, spellData, FailureReason.NOT_PROFILED);
        }

        return new ValidationResult(stack, spellData, FailureReason.NONE);
    }

    public static boolean isSupported(ItemStack stack) {
        return validate(stack).isSupported();
    }

    public record ValidationResult(ItemStack sourceStack, SpellData spellData, FailureReason failureReason) {
        public boolean isSupported() {
            return failureReason == FailureReason.NONE && spellData != SpellData.EMPTY;
        }

        public boolean shouldUseHiddenPresentation() {
            return failureReason != FailureReason.NONE;
        }

        public @Nullable Component getGuiTooltip() {
            return failureReason.createGuiTooltip();
        }
    }

    public enum FailureReason {
        NONE,
        EMPTY,
        NOT_SCROLL,
        NOT_SPELL_CONTAINER,
        NO_ACTIVE_SPELL,
        MULTIPLE_ACTIVE_SPELLS,
        UNSUPPORTED_CAST_TYPE,
        HAS_RECAST,
        SERVER_DISABLED,
        NOT_ALLOWLISTED,
        NOT_PROFILED;

        public @Nullable Component createGuiTooltip() {
            var keyBase = "container.apprenticecodex.spell_dispenser.spell.tooltip.";
            return switch (this) {
                case NONE -> null;
                case EMPTY -> null;
                case NOT_SCROLL, NOT_SPELL_CONTAINER, NO_ACTIVE_SPELL, MULTIPLE_ACTIVE_SPELLS ->
                        Component.translatable(keyBase + "general_error");
                case UNSUPPORTED_CAST_TYPE -> Component.translatable(keyBase + "unsupported_cast");
                case HAS_RECAST -> Component.translatable(keyBase + "has_recast");
                case SERVER_DISABLED -> Component.translatable(keyBase + "server_disabled");
                case NOT_ALLOWLISTED -> Component.translatable(keyBase + "not_allowlisted");
                case NOT_PROFILED -> Component.translatable(keyBase + "not_profiled");
            };
        }
    }
}
