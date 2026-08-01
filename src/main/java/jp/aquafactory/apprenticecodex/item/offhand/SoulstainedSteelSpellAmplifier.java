package jp.aquafactory.apprenticecodex.item.offhand;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class SoulstainedSteelSpellAmplifier extends AbstractSpellAmplifierItem {
    public static final double MAGIC_PROFICIENCY_BONUS = 0.10D;
    private static final int ENCHANTMENT_VALUE = 16;
    private static final ResourceLocation MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_proficiency");

    public SoulstainedSteelSpellAmplifier() {
        super(Rarity.COMMON, "soulstained_steel_spell_amplifier");
    }

    @Override
    protected boolean addStackDependentModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack,
            String modifierKeyPrefix
    ) {
        // Malum を必須依存にせず、前提 MOD の Lodestone が登録した属性だけを実行時に接続する。
        var magicProficiency = BuiltInRegistries.ATTRIBUTE.getOptional(MAGIC_PROFICIENCY).orElse(null);
        if (magicProficiency == null) {
            return false;
        }

        addStackDependentModifier(
                builder,
                magicProficiency,
                MAGIC_PROFICIENCY_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                modifierKeyPrefix + "_magic_proficiency"
        );
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }
}
