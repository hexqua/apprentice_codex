package jp.aquafactory.apprenticecodex.item.elementalbow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ElementalBowEnchantmentBonus(
        ResourceLocation enchantment,
        int bonusPerLevel,
        int flatBonus
) {
    public static final Codec<ElementalBowEnchantmentBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(ElementalBowEnchantmentBonus::enchantment),
            Codec.INT.optionalFieldOf("bonus_per_level", 0).forGetter(ElementalBowEnchantmentBonus::bonusPerLevel),
            Codec.INT.optionalFieldOf("flat_bonus", 0).forGetter(ElementalBowEnchantmentBonus::flatBonus)
    ).apply(instance, ElementalBowEnchantmentBonus::new));
}
