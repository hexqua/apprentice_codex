package jp.aquafactory.apprenticecodex.item.elementalbow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ElementalBowModeDefinition(
        ResourceLocation school,
        ResourceLocation spell,
        int requiredDrawTicks,
        List<ElementalBowEnchantmentBonus> enchantmentBonuses
) {
    public static final int DEFAULT_REQUIRED_DRAW_TICKS = 20;

    public static final Codec<ElementalBowModeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("school").forGetter(ElementalBowModeDefinition::school),
            ResourceLocation.CODEC.fieldOf("spell").forGetter(ElementalBowModeDefinition::spell),
            Codec.INT.optionalFieldOf("required_draw_ticks", DEFAULT_REQUIRED_DRAW_TICKS)
                    .forGetter(ElementalBowModeDefinition::requiredDrawTicks),
            ElementalBowEnchantmentBonus.CODEC.listOf()
                    .optionalFieldOf("enchantment_bonuses", List.of())
                    .forGetter(ElementalBowModeDefinition::enchantmentBonuses)
    ).apply(instance, ElementalBowModeDefinition::new));

    public ElementalBowModeDefinition {
        requiredDrawTicks = Math.max(0, requiredDrawTicks);
        enchantmentBonuses = List.copyOf(enchantmentBonuses);
    }
}
