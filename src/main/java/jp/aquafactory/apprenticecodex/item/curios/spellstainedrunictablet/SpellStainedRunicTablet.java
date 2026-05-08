package jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.SpellBook;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;

public class SpellStainedRunicTablet extends SpellBook implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellstained_runic_tablet.desc_";
    private static final double BASE_MAX_MANA_PER_SPELL = 15.0D;
    private static final double EPIC_MAX_MANA_PER_SPELL = 20.0D;
    private static final double LEGENDARY_MAX_MANA_PER_SPELL = 25.0D;
    private static final double BASE_SCHOOL_POWER_PER_SPELL = 0.02D;
    private static final double LEGENDARY_SCHOOL_POWER_PER_SPELL = 0.03D;
    private static final double LEGENDARY_GLOBAL_SPELL_POWER_PER_SPELL = 0.01D;
    private static final double COOLDOWN_REDUCTION_PER_SCHOOL = 0.03D;
    private static final double CAST_TIME_REDUCTION_AT_LV1 = 0.10D;
    private static final double CAST_TIME_REDUCTION_AT_LV2 = 0.25D;
    private static final double CAST_TIME_REDUCTION_AT_LV3 = 0.50D;

    public SpellStainedRunicTablet() {
        super(8);
    }

    @Override
    public @NotNull Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            ResourceLocation id,
            ItemStack stack
    ) {
        var baseModifiers = super.getAttributeModifiers(slotContext, id, stack);
        if (!Curios.SPELLBOOK_SLOT.equals(slotContext.identifier())) {
            return baseModifiers;
        }

        var dynamicModifiers = buildDynamicSpellbookAttributes(slotContext, stack);
        if (dynamicModifiers.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(dynamicModifiers);
        return builder.build();
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    /**
     * spellbook 内容依存の補正値を 1 箇所へ集約する。
     */
    protected Multimap<Holder<Attribute>, AttributeModifier> buildDynamicSpellbookAttributes(SlotContext slotContext, ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return ImmutableMultimap.of();
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return ImmutableMultimap.of();
        }

        var activeSpells = spellContainer.getActiveSpells();
        if (activeSpells.isEmpty()) {
            return ImmutableMultimap.of();
        }

        double maxManaBonus = 0.0D;
        double globalSpellPowerBonus = 0.0D;
        var schoolSpellPowerBonuses = new HashMap<ResourceLocation, Double>();
        var schoolSpellCounts = new HashMap<ResourceLocation, Integer>();

        for (var spellSlot : activeSpells) {
            var spellData = spellSlot.spellData();
            var spell = spellData.getSpell();
            var rarity = spellData.getRarity();
            var schoolType = spell.getSchoolType();
            var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);

            maxManaBonus += switch (rarity) {
                case LEGENDARY -> LEGENDARY_MAX_MANA_PER_SPELL;
                case EPIC -> EPIC_MAX_MANA_PER_SPELL;
                default -> BASE_MAX_MANA_PER_SPELL;
            };

            if (schoolAttribute != null) {
                var schoolKey = BuiltInRegistries.ATTRIBUTE.getKey(schoolAttribute);
                if (schoolKey != null) {
                    var schoolSpellPowerBonus = rarity == SpellRarity.LEGENDARY
                            ? LEGENDARY_SCHOOL_POWER_PER_SPELL
                            : BASE_SCHOOL_POWER_PER_SPELL;
                    schoolSpellPowerBonuses.merge(schoolKey, schoolSpellPowerBonus, Double::sum);
                }
            }

            if (rarity == SpellRarity.LEGENDARY) {
                globalSpellPowerBonus += LEGENDARY_GLOBAL_SPELL_POWER_PER_SPELL;
            }

            schoolSpellCounts.merge(schoolType.getId(), 1, Integer::sum);
        }

        double cooldownReductionBonus = schoolSpellCounts.size() >= 2
                ? schoolSpellCounts.size() * COOLDOWN_REDUCTION_PER_SCHOOL
                : 0.0D;
        double castTimeReductionBonus = schoolSpellCounts.values().stream()
                .mapToDouble(this::resolveCastTimeReductionBonus)
                .sum();

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        addModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                maxManaBonus,
                AttributeModifier.Operation.ADD_VALUE,
                slotContext,
                "max_mana"
        );
        addModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                globalSpellPowerBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "global_spell_power"
        );
        addModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                cooldownReductionBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "cooldown_reduction"
        );
        addModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                castTimeReductionBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "cast_time_reduction"
        );

        for (var entry : schoolSpellPowerBonuses.entrySet()) {
            var attribute = BuiltInRegistries.ATTRIBUTE.get(entry.getKey());
            if (attribute == null) {
                continue;
            }

            addModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    slotContext,
                    "school_spell_power/" + entry.getKey().getNamespace() + "/" + entry.getKey().getPath()
            );
        }

        return builder.build();
    }

    private double resolveCastTimeReductionBonus(int spellCount) {
        if (spellCount >= 12) {
            return CAST_TIME_REDUCTION_AT_LV3;
        }
        if (spellCount >= 8) {
            return CAST_TIME_REDUCTION_AT_LV2;
        }
        if (spellCount >= 4) {
            return CAST_TIME_REDUCTION_AT_LV1;
        }
        return 0.0D;
    }

    private void addModifier(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            SlotContext slotContext,
            String modifierKey
    ) {
        if (amount == 0.0D) {
            return;
        }

        builder.put(
                attribute,
                new AttributeModifier(
                        createModifierId(slotContext, modifierKey),
                        amount,
                        operation
                )
        );
    }

    private static ResourceLocation createModifierId(SlotContext slotContext, String modifierKey) {
        var sanitizedKey = modifierKey.replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "spellstained_runic_tablet/" + Curios.SPELLBOOK_SLOT + "_" + slotContext.index() + "_" + sanitizedKey
        );
    }
}
