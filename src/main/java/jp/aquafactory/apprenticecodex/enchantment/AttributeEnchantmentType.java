package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Iron's の魔法 Attribute を増加させる6種のエンチャント効果定義。
 */
public enum AttributeEnchantmentType {
    ALACRITY("alacrity", "alacrityAmountPerLevel", "alacrity.cooldown_reduction", 0.02D,
            AttributeModifier.Operation.MULTIPLY_BASE, stack -> AttributeRegistry.COOLDOWN_REDUCTION.get()),
    REFLUX("reflux", "refluxAmountPerLevel", "reflux.mana_regen", 0.05D,
            AttributeModifier.Operation.MULTIPLY_BASE, stack -> AttributeRegistry.MANA_REGEN.get()),
    RESERVOIR("reservoir", "reservoirAmountPerLevel", "reservoir.max_mana", 20.0D,
            AttributeModifier.Operation.ADDITION, stack -> AttributeRegistry.MAX_MANA.get()),
    SURGE("surge", "surgeAmountPerLevel", "surge.spell_power", 0.02D,
            AttributeModifier.Operation.MULTIPLY_BASE, stack -> AttributeRegistry.SPELL_POWER.get()),
    ATTUNEMENT("attunement", "attunementAmountPerLevel", "attunement.spell_power", 0.03D,
            AttributeModifier.Operation.MULTIPLY_BASE,
            stack -> MagicTools.resolveSchoolPowerAttribute(MagicTools.getImbuedSpellSchool(stack))),
    TENSE("tense", "tenseAmountPerLevel", "tense.cast_time_reduction", 0.04D,
            AttributeModifier.Operation.MULTIPLY_BASE, stack -> AttributeRegistry.CAST_TIME_REDUCTION.get());

    private final ResourceLocation enchantmentId;
    private final String configKey;
    private final String modifierKey;
    private final double defaultAmountPerLevel;
    private final AttributeModifier.Operation operation;
    private final Function<ItemStack, @Nullable Attribute> attributeResolver;

    AttributeEnchantmentType(
            String enchantmentPath,
            String configKey,
            String modifierKey,
            double defaultAmountPerLevel,
            AttributeModifier.Operation operation,
            Function<ItemStack, @Nullable Attribute> attributeResolver
    ) {
        this.enchantmentId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, enchantmentPath);
        this.configKey = configKey;
        this.modifierKey = modifierKey;
        this.defaultAmountPerLevel = defaultAmountPerLevel;
        this.operation = operation;
        this.attributeResolver = attributeResolver;
    }

    public ResourceLocation enchantmentId() {
        return enchantmentId;
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

    public @Nullable Attribute resolveAttribute(ItemStack stack) {
        return attributeResolver.apply(stack);
    }

    public int getLevel(ItemStack stack) {
        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        return enchantment == null ? 0 : stack.getEnchantmentLevel(enchantment);
    }

    public static Optional<AttributeEnchantmentType> from(Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(type -> type.enchantmentId.equals(enchantmentId))
                .findFirst();
    }
}
