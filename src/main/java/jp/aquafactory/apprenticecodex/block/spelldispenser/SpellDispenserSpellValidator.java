package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserSpellValidator {
    private SpellDispenserSpellValidator() {
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

        var castType = spell.getCastType();
        if (castType != CastType.INSTANT && castType != CastType.LONG && castType != CastType.CONTINUOUS) {
            return new ValidationResult(stack, spellData, FailureReason.UNSUPPORTED_CAST_TYPE);
        }
        if (spell.getRecastCount(spellData.getLevel(), null) > 0) {
            return new ValidationResult(stack, spellData, FailureReason.HAS_RECAST);
        }
        if (ApprenticeCodexServerConfig.spellDispenserRelaxedSpellFilter()) {
            return new ValidationResult(stack, spellData, FailureReason.NONE);
        }
        if (SpellDispenserSpellListManager.isDenylisted(spell)) {
            return new ValidationResult(stack, spellData, FailureReason.DENYLISTED);
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

        public Component getStatus(Player player) {
            if (isSupported()) {
                return Component.translatable(
                        "container." + ApprenticeCodex.MODID + ".spell_dispenser.status.ready",
                        spellData.getSpell().getDisplayName(player)
                );
            }

            return failureReason.createMessage(spellData, player);
        }

        public @Nullable Component getCurrentSpellLabel(Player player) {
            if (spellData == SpellData.EMPTY) {
                return null;
            }

            return Component.translatable(
                    "container." + ApprenticeCodex.MODID + ".spell_dispenser.current_spell",
                    spellData.getSpell().getDisplayName(player)
            );
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
        NOT_PROFILED,
        DENYLISTED;

        public Component createMessage(SpellData spellData, @Nullable Player player) {
            var keyBase = "container." + ApprenticeCodex.MODID + ".spell_dispenser.status.";
            return switch (this) {
                case NONE -> Component.empty();
                case EMPTY -> Component.translatable(keyBase + "empty");
                case NOT_SCROLL -> Component.translatable(keyBase + "not_scroll");
                case NOT_SPELL_CONTAINER -> Component.translatable(keyBase + "invalid_item");
                case NO_ACTIVE_SPELL -> Component.translatable(keyBase + "no_active_spell");
                case MULTIPLE_ACTIVE_SPELLS -> Component.translatable(keyBase + "multiple_spells");
                case UNSUPPORTED_CAST_TYPE -> Component.translatable(
                        keyBase + "unsupported_cast",
                        spellData == SpellData.EMPTY ? Component.empty() : spellData.getSpell().getDisplayName(player)
                );
                case HAS_RECAST -> Component.translatable(
                        keyBase + "has_recast",
                        spellData == SpellData.EMPTY ? Component.empty() : spellData.getSpell().getDisplayName(player)
                );
                case NOT_PROFILED -> Component.translatable(
                        keyBase + "not_profiled",
                        spellData == SpellData.EMPTY ? Component.empty() : spellData.getSpell().getDisplayName(player)
                );
                case DENYLISTED -> Component.translatable(
                        keyBase + "denylisted",
                        spellData == SpellData.EMPTY ? Component.empty() : spellData.getSpell().getDisplayName(player)
                );
            };
        }
    }
}
