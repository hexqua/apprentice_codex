package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.compat.create.CreateCompat;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** エンドゲーム防具で共有する調整候補と、その効果の適用を一か所に保つ。 */
public final class EndgameArmorCalibration {
    public static final int SLOT_COUNT = 3;
    public static final double MAX_MANA_PER_RUNE = 25.0D;
    public static final double SPELL_RESIST_PER_RUNE = 0.05D;
    public static final double KNOCKBACK_RESIST_PER_PLATE = 0.10D;
    public static final double SOUL_WARD_CAPACITY = 3.0D;
    public static final double SOUL_WARD_RECOVERY_RATE = 0.15D;
    public static final double MAGIC_PROFICIENCY = 0.15D;
    public static final int STORED_SCROLL_SLOT_COUNT = 1;

    private static final String SCROLL_SLOT_EMPTY_KEY =
            "item.apprenticecodex.endgame_armor.scroll_slot.empty";
    private static final String SCROLL_SLOT_SPELL_KEY =
            "item.apprenticecodex.endgame_armor.scroll_slot.spell";
    private static final String SCROLL_SLOT_INACTIVE_KEY =
            "item.apprenticecodex.endgame_armor.scroll_slot.inactive";
    private static final String SCROLL_SLOT_REQUIRES_KEY =
            "item.apprenticecodex.endgame_armor.scroll_slot.requires";

    public static final ResourceLocation CREATE_GOGGLES =
            ResourceLocation.fromNamespaceAndPath(CreateCompat.MOD_ID, "goggles");
    public static final ResourceLocation SOUL_WARD_CAPACITY_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("malum", "soul_ward_capacity");
    public static final ResourceLocation SOUL_WARD_RECOVERY_RATE_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("malum", "soul_ward_recovery_rate");
    public static final ResourceLocation MAGIC_PROFICIENCY_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_proficiency");

    private EndgameArmorCalibration() {
    }

    public static @NotNull CalibrationAdjustmentProfile createProfile(
            ArmorItem.Type armorType,
            boolean acceptsSchoolRune
    ) {
        var rules = new ArrayList<CalibrationAdjustmentRule>();
        if (acceptsSchoolRune) {
            rules.add(CalibrationAdjustmentRule.unique(
                    "school_rune",
                    ScrollcasterSchoolRuneResolver::isSchoolRune,
                    CalibrationAdjustmentHints.schoolRunes(),
                    CalibrationAdjustmentHints.schoolRuneConstraint()
            ).withEffectLines(() -> CalibrationAdjustmentEffects.addSpellPower(
                    jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig
                            .magiAgentSuitSchoolSpellPowerBonus()
            )));
        }

        rules.add(CalibrationAdjustmentRule.repeatable(
                "endgame_armor_arcane_rune",
                stack -> stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                specific(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE)
        ).withEffectLines(CalibrationAdjustmentEffects.addMaxMana(MAX_MANA_PER_RUNE)));
        rules.add(CalibrationAdjustmentRule.unique(
                "endgame_armor_protective_rune",
                stack -> stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()),
                specific(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE)
        ).withEffectLines(CalibrationAdjustmentEffects.addSpellResist(SPELL_RESIST_PER_RUNE)));
        rules.add(uniqueItemRule(
                "endgame_armor_shock_absorption_plate",
                ItemRegistry.SHOCK_ABSORPTION_PLATE,
                CalibrationAdjustmentEffects.addKnockbackResistance(KNOCKBACK_RESIST_PER_PLATE)
        ));
        rules.add(uniqueItemRule(
                "endgame_armor_blast_reactive_plate",
                ItemRegistry.BLAST_REACTIVE_PLATE,
                CalibrationAdjustmentEffects.inactiveIn1201()
        ));
        rules.add(uniqueItemRule(
                "endgame_armor_wind_accumulation_weave",
                ItemRegistry.WIND_ACCUMULATION_WEAVE,
                CalibrationAdjustmentEffects.inactiveIn1201()
        ));

        if (armorType != ArmorItem.Type.CHESTPLATE) {
            rules.add(uniqueItemRule(
                    "endgame_armor_scrollwoven_parchment",
                    ItemRegistry.SCROLLWOVEN_PARCHMENT,
                    CalibrationAdjustmentEffects.addScrollSlot(1)
            ));
        }
        if (armorType == ArmorItem.Type.BOOTS) {
            rules.add(CalibrationAdjustmentRule.unique(
                    "endgame_armor_leather_boots",
                    stack -> stack.is(Items.LEATHER_BOOTS),
                    specific(() -> Items.LEATHER_BOOTS)
            ).withEffectLines(CalibrationAdjustmentEffects.gainPowderSnowWalk()));
        }
        if (armorType == ArmorItem.Type.HELMET && ModList.get().isLoaded(CreateCompat.MOD_ID)) {
            rules.add(CalibrationAdjustmentRule.unique(
                    "endgame_armor_create_goggles",
                    stack -> isRegistryItem(stack, CREATE_GOGGLES),
                    specific(() -> BuiltInRegistries.ITEM.get(CREATE_GOGGLES))
            ).withEffectLines(CalibrationAdjustmentEffects.gainCreateGoggles()));
        }
        if (ModList.get().isLoaded("malum")) {
            rules.add(CalibrationAdjustmentRule.unique(
                    "endgame_armor_soul_covered_plate",
                    stack -> stack.is(ItemRegistry.SOUL_COVERED_PLATE.get()),
                    specific(ItemRegistry.SOUL_COVERED_PLATE)
            ).withEffectLines(() -> CalibrationAdjustmentEffects.addSoulWard(
                            attributeName(SOUL_WARD_CAPACITY_ATTRIBUTE),
                            SOUL_WARD_CAPACITY,
                            attributeName(SOUL_WARD_RECOVERY_RATE_ATTRIBUTE),
                            SOUL_WARD_RECOVERY_RATE
                    )));
            rules.add(CalibrationAdjustmentRule.unique(
                    "endgame_armor_soul_augmented_weave",
                    stack -> stack.is(ItemRegistry.SOUL_AUGMENTED_WEAVE.get()),
                    specific(ItemRegistry.SOUL_AUGMENTED_WEAVE)
            ).withEffectLines(() -> CalibrationAdjustmentEffects.addMagicProficiency(
                            attributeName(MAGIC_PROFICIENCY_ATTRIBUTE),
                            MAGIC_PROFICIENCY
                    )));
        }
        return CalibrationAdjustmentProfile.of(rules.toArray(CalibrationAdjustmentRule[]::new));
    }

    public static void addAttributeModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack armorStack,
            ArmorItem.Type armorType,
            SpellCalibrationAdjustmentTarget target
    ) {
        var armorId = BuiltInRegistries.ITEM.getKey(armorStack.getItem());
        var prefix = (armorId == null ? "endgame_armor" : armorId.getPath()) + "_calibration";

        for (var slot = 0; slot < target.getCalibrationAdjustmentSlotCount(armorStack); ++slot) {
            var adjustment = target.getCalibrationAdjustment(armorStack, slot);
            if (adjustment.isEmpty()) {
                continue;
            }
            var modifierPrefix = prefix + "_slot_" + slot;
            if (adjustment.is(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())) {
                add(builder, AttributeRegistry.MAX_MANA.get(), MAX_MANA_PER_RUNE,
                        AttributeModifier.Operation.ADDITION, modifierPrefix + "_max_mana");
            } else if (adjustment.is(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())) {
                add(builder, AttributeRegistry.SPELL_RESIST.get(), SPELL_RESIST_PER_RUNE,
                        AttributeModifier.Operation.MULTIPLY_BASE, modifierPrefix + "_spell_resist");
            } else if (adjustment.is(ItemRegistry.SHOCK_ABSORPTION_PLATE.get())) {
                add(builder, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_PER_PLATE,
                        AttributeModifier.Operation.ADDITION, modifierPrefix + "_knockback_resist");
            } else if (adjustment.is(ItemRegistry.SOUL_COVERED_PLATE.get())) {
                addOptional(builder, SOUL_WARD_CAPACITY_ATTRIBUTE, SOUL_WARD_CAPACITY,
                        AttributeModifier.Operation.ADDITION, modifierPrefix + "_soul_ward_capacity");
                addOptional(builder, SOUL_WARD_RECOVERY_RATE_ATTRIBUTE, SOUL_WARD_RECOVERY_RATE,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        modifierPrefix + "_soul_ward_recovery_rate");
            } else if (adjustment.is(ItemRegistry.SOUL_AUGMENTED_WEAVE.get())) {
                addOptional(builder, MAGIC_PROFICIENCY_ATTRIBUTE, MAGIC_PROFICIENCY,
                        AttributeModifier.Operation.MULTIPLY_BASE,
                        modifierPrefix + "_magic_proficiency");
            }
        }
    }

    public static boolean canWalkOnPowderedSnow(ItemStack armorStack, LivingEntity wearer) {
        if (!(armorStack.getItem() instanceof ArmorItem armorItem)
                || armorItem.getType() != ArmorItem.Type.BOOTS) {
            return false;
        }
        return containsAdjustment(armorStack, stack -> stack.is(Items.LEATHER_BOOTS));
    }

    public static boolean hasCreateGoggles(ItemStack armorStack) {
        return armorStack.getItem() instanceof ArmorItem armorItem
                && armorItem.getType() == ArmorItem.Type.HELMET
                && isEndgameArmor(armorStack.getItem())
                && containsAdjustment(armorStack, stack -> isRegistryItem(stack, CREATE_GOGGLES));
    }

    public static boolean usesStoredCalibrationScrolls(ItemStack armorStack) {
        return armorStack.getItem() instanceof ArmorItem armorItem
                && armorItem.getType() != ArmorItem.Type.CHESTPLATE
                && isEndgameArmor(armorStack.getItem());
    }

    public static int getEnabledStoredScrollSlotCount(ItemStack armorStack) {
        return usesStoredCalibrationScrolls(armorStack) && hasScrollwovenParchment(armorStack)
                ? STORED_SCROLL_SLOT_COUNT
                : 0;
    }

    public static @NotNull ItemStack getStoredScroll(ItemStack armorStack, int slot) {
        return slot == 0 && usesStoredCalibrationScrolls(armorStack)
                ? EndgameArmorScrollStorage.get(armorStack)
                : ItemStack.EMPTY;
    }

    public static void setStoredScroll(ItemStack armorStack, int slot, ItemStack scrollStack) {
        if (slot == 0 && usesStoredCalibrationScrolls(armorStack)) {
            EndgameArmorScrollStorage.set(armorStack, scrollStack);
        }
    }

    public static boolean hasAnyStoredScroll(ItemStack armorStack) {
        return usesStoredCalibrationScrolls(armorStack) && !EndgameArmorScrollStorage.get(armorStack).isEmpty();
    }

    public static @NotNull SpellCalibrationImbueState evaluateStoredScroll(
            ItemStack armorStack,
            int slot,
            SpellData spellData
    ) {
        if (!usesStoredCalibrationScrolls(armorStack)) {
            // 胴体は従来どおりSpellContainerを使うため、共通Helper側の検証を通過した呪文をそのまま受理する。
            return slot == 0 && spellData != SpellData.EMPTY && spellData.getSpell() != null
                    ? SpellCalibrationImbueState.ACCEPTED_USABLE
                    : SpellCalibrationImbueState.REJECTED;
        }
        return slot == 0
                && getEnabledStoredScrollSlotCount(armorStack) == STORED_SCROLL_SLOT_COUNT
                && spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                ? SpellCalibrationImbueState.ACCEPTED_USABLE
                : SpellCalibrationImbueState.REJECTED;
    }

    public static @NotNull SpellData getStoredSpellData(ItemStack armorStack) {
        var scrollStack = getStoredScroll(armorStack, 0);
        var container = ISpellContainer.get(scrollStack);
        if (container == null) {
            return SpellData.EMPTY;
        }
        var spellData = container.getSpellAtIndex(0);
        // Iron'sの@NotNull宣言に反してnullになり得るため、イベントへ渡す前に空として扱う。
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    public static void appendStoredScrollTooltip(ItemStack armorStack, List<Component> lines) {
        if (!usesStoredCalibrationScrolls(armorStack)) {
            return;
        }
        var enabled = getEnabledStoredScrollSlotCount(armorStack) > 0;
        var spellData = getStoredSpellData(armorStack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            if (enabled) {
                lines.add(Component.translatable(SCROLL_SLOT_EMPTY_KEY).withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        var spellName = spellData.getSpell().getDisplayName(null).copy()
                .append(" ")
                .append(Integer.toString(spellData.getLevel()))
                .withStyle(spellData.getSpell().getSchoolType().getDisplayName().getStyle());
        if (enabled) {
            lines.add(Component.translatable(SCROLL_SLOT_SPELL_KEY, spellName).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable(SCROLL_SLOT_INACTIVE_KEY, spellName).withStyle(ChatFormatting.YELLOW));
            lines.add(Component.translatable(
                    SCROLL_SLOT_REQUIRES_KEY,
                    ItemRegistry.SCROLLWOVEN_PARCHMENT.get().getDescription()
            ).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean hasScrollwovenParchment(ItemStack armorStack) {
        return containsAdjustment(armorStack, stack -> stack.is(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()));
    }

    private static boolean containsAdjustment(ItemStack armorStack, java.util.function.Predicate<ItemStack> matcher) {
        if (!(armorStack.getItem() instanceof SpellCalibrationAdjustmentTarget target)) {
            return false;
        }
        for (var slot = 0; slot < target.getCalibrationAdjustmentSlotCount(armorStack); ++slot) {
            if (matcher.test(target.getCalibrationAdjustment(armorStack, slot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEndgameArmor(Item item) {
        return item instanceof ChromaticMagiaDressItem
                || item instanceof MagiAgentSuitItem
                || item instanceof ElementMaidenRobeItem;
    }

    private static CalibrationAdjustmentRule uniqueItemRule(
            String id,
            Supplier<? extends Item> item,
            List<Component> effects
    ) {
        return CalibrationAdjustmentRule.unique(id, stack -> stack.is(item.get()), specific(item))
                .withEffectLines(effects);
    }

    private static CalibrationAdjustmentHint specific(Supplier<? extends Item> item) {
        return CalibrationAdjustmentHint.specificItem(item);
    }

    private static boolean isRegistryItem(ItemStack stack, ResourceLocation itemId) {
        return itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static Component attributeName(ResourceLocation attributeId) {
        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeId).orElse(null);
        return attribute == null ? Component.literal(attributeId.getPath()) : Component.translatable(attribute.getDescriptionId());
    }

    private static void addOptional(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ResourceLocation attributeId,
            double amount,
            AttributeModifier.Operation operation,
            String modifierPath
    ) {
        var attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute != null) {
            add(builder, attribute, amount, operation, modifierPath);
        }
    }

    private static void add(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierPath
    ) {
        MagicArmorAttributeHelper.addModifier(builder, attribute, amount, operation, modifierPath);
    }
}
