package jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.SpellBook;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class SpellStainedRunicTablet extends SpellBook implements IJeiInfoItem {
    private static final String MODIFIER_NAME_PREFIX = "apprenticecodex.spellstained_runic_tablet.";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellstained_runic_tablet.desc_";

    public SpellStainedRunicTablet() {
        super(8);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slotContext, uuid, stack);
        if (!Curios.SPELLBOOK_SLOT.equals(slotContext.identifier())) {
            return baseModifiers;
        }

        var dynamicModifiers = buildDynamicSpellbookAttributes(slotContext, stack);
        if (dynamicModifiers.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(dynamicModifiers);
        return builder.build();
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    /**
     * 将来の状態依存能力値をここに集約する.
     */
    protected Multimap<Attribute, AttributeModifier> buildDynamicSpellbookAttributes(SlotContext slotContext, ItemStack stack) {
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
        var schoolSpellPowerBonuses = new HashMap<Attribute, Double>();
        var schoolSpellCounts = new HashMap<String, Integer>();
        var config = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();

        for (var spellSlot : activeSpells) {
            var spellData = spellSlot.spellData();
            var spell = spellData.getSpell();
            var rarity = spellData.getRarity();
            var schoolType = spell.getSchoolType();
            var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);

            maxManaBonus += config.maxMana().forRarity(rarity);

            if (schoolAttribute != null) {
                var schoolSpellPowerBonus = config.schoolSpellPower().forRarity(rarity);
                schoolSpellPowerBonuses.merge(schoolAttribute, schoolSpellPowerBonus, Double::sum);
            }

            globalSpellPowerBonus += config.generalSpellPower().forRarity(rarity);

            var schoolId = schoolType.getId().toString();
            schoolSpellCounts.merge(schoolId, 1, Integer::sum);
        }

        double cooldownReductionBonus = config.cooldownReduction().resolve(schoolSpellCounts.size());
        double castTimeReductionBonus = schoolSpellCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .stream()
                .mapToDouble(config.castTimeReduction()::resolve)
                .sum();

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        addModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                maxManaBonus,
                AttributeModifier.Operation.ADDITION,
                slotContext,
                "max_mana"
        );
        addModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                globalSpellPowerBonus,
                AttributeModifier.Operation.MULTIPLY_BASE,
                slotContext,
                "global_spell_power"
        );
        addModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                cooldownReductionBonus,
                AttributeModifier.Operation.MULTIPLY_BASE,
                slotContext,
                "cooldown_reduction"
        );
        addModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                castTimeReductionBonus,
                AttributeModifier.Operation.MULTIPLY_BASE,
                slotContext,
                "cast_time_reduction"
        );

        for (var entry : schoolSpellPowerBonuses.entrySet()) {
            var schoolKey = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (schoolKey == null) {
                continue;
            }

            addModifier(
                    builder,
                    entry.getKey(),
                    entry.getValue(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    slotContext,
                    "school_spell_power." + schoolKey
            );
        }

        return builder.build();
    }

    private void addModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            SlotContext slotContext,
            String modifierKey
    ) {
        if (amount == 0.0D) {
            return;
        }

        var modifierName = MODIFIER_NAME_PREFIX + modifierKey;
        builder.put(
                attribute,
                new AttributeModifier(
                        createModifierId(slotContext, modifierKey),
                        modifierName,
                        amount,
                        operation
                )
        );
    }

    private UUID createModifierId(SlotContext slotContext, String modifierKey) {
        var seed = Curios.SPELLBOOK_SLOT + ":" + slotContext.index() + ":" + modifierKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
