package jp.aquafactory.apprenticecodex.effect;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PenetratedArmor extends MobEffect {
    private static final double ARMOR_REDUCTION_PER_LEVEL = -0.2D;
    private static final double ARMOR_TOUGHNESS_REDUCTION = -1.0D;
    private static final int MAX_AMPLIFIER = 3;
    private static final ResourceLocation ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "penetrated_armor_armor");
    private static final ResourceLocation ARMOR_TOUGHNESS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "penetrated_armor_toughness");

    public PenetratedArmor() {
        super(MobEffectCategory.HARMFUL, 0xAEEB9A);

        addAttributeModifier(
                Attributes.ARMOR,
                ARMOR_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> ARMOR_REDUCTION_PER_LEVEL * (Math.max(0, Math.min(MAX_AMPLIFIER, amplifier)) + 1)
        );
        addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                ARMOR_TOUGHNESS_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> ARMOR_TOUGHNESS_REDUCTION
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
