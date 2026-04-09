package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class SpellDispenserSpellValidator {
    private static final String IRONS_NAMESPACE = "irons_spellbooks";
    private static final Set<ResourceLocation> DENYLIST = Set.of(
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "counterspell"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "pocket_dimension"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "portal"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "raise_dead"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "recall"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "spectral_hammer"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "summon_ender_chest"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "summon_horse"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "summon_polar_bear"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "summon_swords"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "summon_vex"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "telekinesis"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "thunder_step"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "touch_dig"),
            ResourceLocation.fromNamespaceAndPath(IRONS_NAMESPACE, "volt_strike")
    );

    private SpellDispenserSpellValidator() {
    }

    public static ValidationResult validate(ItemStack stack) {
        if (stack.isEmpty()) {
            return new ValidationResult(ItemStack.EMPTY, SpellData.EMPTY, FailureReason.EMPTY);
        }

        if (!ISpellContainer.isSpellContainer(stack)) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NOT_SPELL_CONTAINER);
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NOT_SPELL_CONTAINER);
        }

        var activeSpells = spellContainer.getActiveSpells().stream()
                .map(slot -> slot.spellData())
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
        var spellId = spell.getSpellResource();
        if (spellId == null) {
            return new ValidationResult(stack, SpellData.EMPTY, FailureReason.NO_ACTIVE_SPELL);
        }
        if (!IRONS_NAMESPACE.equals(spellId.getNamespace())) {
            return new ValidationResult(stack, spellData, FailureReason.NOT_IRONS_SPELL);
        }
        if (spell.getCastType() != CastType.INSTANT) {
            return new ValidationResult(stack, spellData, FailureReason.NOT_INSTANT_CAST);
        }
        if (DENYLIST.contains(spellId)) {
            return new ValidationResult(stack, spellData, FailureReason.DENYLISTED);
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
        NOT_SPELL_CONTAINER,
        NO_ACTIVE_SPELL,
        MULTIPLE_ACTIVE_SPELLS,
        NOT_IRONS_SPELL,
        NOT_INSTANT_CAST,
        DENYLISTED;

        public Component createMessage(SpellData spellData, @Nullable Player player) {
            var keyBase = "container." + ApprenticeCodex.MODID + ".spell_dispenser.status.";
            return switch (this) {
                case NONE -> Component.empty();
                case EMPTY -> Component.translatable(keyBase + "empty");
                case NOT_SPELL_CONTAINER -> Component.translatable(keyBase + "invalid_item");
                case NO_ACTIVE_SPELL -> Component.translatable(keyBase + "no_active_spell");
                case MULTIPLE_ACTIVE_SPELLS -> Component.translatable(keyBase + "multiple_spells");
                case NOT_IRONS_SPELL -> Component.translatable(keyBase + "not_irons");
                case NOT_INSTANT_CAST -> Component.translatable(
                        keyBase + "not_instant",
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
