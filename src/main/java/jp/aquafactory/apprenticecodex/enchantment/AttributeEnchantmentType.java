package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Iron's の魔法 Attribute を増加させる6種のエンチャント効果定義。
 */
public enum AttributeEnchantmentType {
    ALACRITY(Enchantments.ALACRITY, "alacrityAmountPerLevel", "alacrity.cooldown_reduction", 0.02D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, stack -> AttributeRegistry.COOLDOWN_REDUCTION),
    REFLUX(Enchantments.REFLUX, "refluxAmountPerLevel", "reflux.mana_regen", 0.05D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, stack -> AttributeRegistry.MANA_REGEN),
    RESERVOIR(Enchantments.RESERVOIR, "reservoirAmountPerLevel", "reservoir.max_mana", 20.0D,
            AttributeModifier.Operation.ADD_VALUE, stack -> AttributeRegistry.MAX_MANA),
    SURGE(Enchantments.SURGE, "surgeAmountPerLevel", "surge.spell_power", 0.02D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, stack -> AttributeRegistry.SPELL_POWER),
    ATTUNEMENT(Enchantments.ATTUNEMENT, "attunementAmountPerLevel", "attunement.spell_power", 0.03D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            stack -> holder(MagicTools.resolveSchoolPowerAttribute(MagicTools.getImbuedSpellSchool(stack)))),
    TENSE(Enchantments.TENSE, "tenseAmountPerLevel", "tense.cast_time_reduction", 0.04D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, stack -> AttributeRegistry.CAST_TIME_REDUCTION);

    private final ResourceKey<Enchantment> enchantmentKey;
    private final String configKey;
    private final String modifierKey;
    private final double defaultAmountPerLevel;
    private final AttributeModifier.Operation operation;
    private final Function<ItemStack, @Nullable Holder<Attribute>> attributeResolver;

    AttributeEnchantmentType(
            ResourceKey<Enchantment> enchantmentKey,
            String configKey,
            String modifierKey,
            double defaultAmountPerLevel,
            AttributeModifier.Operation operation,
            Function<ItemStack, @Nullable Holder<Attribute>> attributeResolver
    ) {
        this.enchantmentKey = enchantmentKey;
        this.configKey = configKey;
        this.modifierKey = modifierKey;
        this.defaultAmountPerLevel = defaultAmountPerLevel;
        this.operation = operation;
        this.attributeResolver = attributeResolver;
    }

    public ResourceKey<Enchantment> enchantmentKey() {
        return enchantmentKey;
    }

    public String configKey() {
        return configKey;
    }

    public String modifierKey() {
        return modifierKey;
    }

    public double amountPerLevel() {
        return ApprenticeCodexServerConfig.attributeEnchantmentAmountPerLevel(this);
    }

    public double defaultAmountPerLevel() {
        return defaultAmountPerLevel;
    }

    public AttributeModifier.Operation operation() {
        return operation;
    }

    public @Nullable Holder<Attribute> resolveAttribute(ItemStack stack) {
        return attributeResolver.apply(stack);
    }

    public int getLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, enchantmentKey);
    }

    public static Optional<AttributeEnchantmentType> from(Holder<Enchantment> enchantment) {
        return Arrays.stream(values())
                .filter(type -> enchantment.is(type.enchantmentKey))
                .findFirst();
    }

    private static @Nullable Holder<Attribute> holder(@Nullable Attribute attribute) {
        return attribute == null ? null : BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }
}
