package jp.aquafactory.apprenticecodex.item.offhand;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class SoulstainedSteelSpellAmplifier extends AbstractSpellAmplifierItem {
    public static final double MAGIC_PROFICIENCY_BONUS = 0.15D;
    public static final double SOUL_WARD_CAPACITY_BONUS = 3.0D;
    private static final int ENCHANTMENT_VALUE = 16;
    private static final ResourceLocation MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_proficiency");
    private static final ResourceLocation SOUL_WARD_CAPACITY =
            ResourceLocation.fromNamespaceAndPath("malum", "soul_ward_capacity");

    public SoulstainedSteelSpellAmplifier() {
        super(Rarity.COMMON, "soulstained_steel_spell_amplifier");
    }

    @Override
    protected boolean addStackDependentModifiers(
            ItemAttributeModifiers.Builder builder,
            ItemStack stack,
            String modifierKeyPrefix
    ) {
        // Malum を必須依存にせず、Lodestone / Malum が登録した属性だけを実行時に接続する。
        var magicProficiency = BuiltInRegistries.ATTRIBUTE.getOptional(MAGIC_PROFICIENCY).orElse(null);
        addStackDependentModifier(
                builder,
                magicProficiency,
                MAGIC_PROFICIENCY_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                modifierKeyPrefix + "_magic_proficiency"
        );

        var soulWardCapacity = BuiltInRegistries.ATTRIBUTE.getOptional(SOUL_WARD_CAPACITY).orElse(null);
        addStackDependentModifier(
                builder,
                soulWardCapacity,
                SOUL_WARD_CAPACITY_BONUS,
                AttributeModifier.Operation.ADD_VALUE,
                modifierKeyPrefix + "_soul_ward_capacity"
        );
        return magicProficiency != null || soulWardCapacity != null;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }
}
