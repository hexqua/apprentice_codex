package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;

public record AutocastAmuletMobEffectCondition(
        ResourceLocation effect,
        int remainingTicksAtMost
) {
    public static final Codec<AutocastAmuletMobEffectCondition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("effect").forGetter(AutocastAmuletMobEffectCondition::effect),
                    Codec.intRange(0, Integer.MAX_VALUE)
                            .fieldOf("remaining_ticks_at_most")
                            .forGetter(AutocastAmuletMobEffectCondition::remainingTicksAtMost)
            ).apply(instance, AutocastAmuletMobEffectCondition::new)
    );

    public boolean matches(LivingEntity entity) {
        var mobEffect = BuiltInRegistries.MOB_EFFECT.get(effect);
        if (mobEffect == null) {
            return false;
        }

        return getRemainingTicks(entity, BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect)) <= remainingTicksAtMost;
    }

    private static int getRemainingTicks(LivingEntity entity, Holder<MobEffect> mobEffect) {
        var effectInstance = entity.getEffect(mobEffect);
        return effectInstance == null ? 0 : effectInstance.getDuration();
    }
}
