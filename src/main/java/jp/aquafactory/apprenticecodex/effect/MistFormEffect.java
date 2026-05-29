package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MistFormEffect extends MobEffect {
    public static final double MOVEMENT_SPEED_BONUS = 0.20D;
    public static final double STEP_HEIGHT_ADDITION = 0.5D;
    public static final double SCHOOL_RESIST_WEAKNESS = -1.0D;
    public static final float JUMP_POWER_ADDITION = 0.3F;

    private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mist_form_movement_speed");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mist_form_step_height");
    private static final ResourceLocation FIRE_RESIST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mist_form_fire_resist");
    private static final ResourceLocation HOLY_RESIST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mist_form_holy_resist");

    public MistFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4FCFF);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> MOVEMENT_SPEED_BONUS
        );
        addAttributeModifier(
                Attributes.STEP_HEIGHT,
                STEP_HEIGHT_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                amplifier -> STEP_HEIGHT_ADDITION
        );
        addAttributeModifier(
                AttributeRegistry.FIRE_MAGIC_RESIST,
                FIRE_RESIST_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                amplifier -> SCHOOL_RESIST_WEAKNESS
        );
        addAttributeModifier(
                AttributeRegistry.HOLY_MAGIC_RESIST,
                HOLY_RESIST_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                amplifier -> SCHOOL_RESIST_WEAKNESS
        );
    }
}
