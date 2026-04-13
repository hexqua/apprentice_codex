package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ManaRegeneration extends MobEffect {
    private static final double MANA_REGEN_BONUS_PER_LEVEL = 0.25D;
    private static final ResourceLocation MANA_REGEN_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "mana_regeneration");

    public ManaRegeneration() {
        super(MobEffectCategory.BENEFICIAL, 0x8899FF);

        addAttributeModifier(
                AttributeRegistry.MANA_REGEN,
                MANA_REGEN_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> MANA_REGEN_BONUS_PER_LEVEL * (Math.max(0, amplifier) + 1)
        );
    }
}
