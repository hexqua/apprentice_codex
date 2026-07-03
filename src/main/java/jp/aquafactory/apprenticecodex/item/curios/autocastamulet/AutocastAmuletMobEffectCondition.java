package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

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
        var mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect);
        if (mobEffect == null) {
            return false;
        }

        return getRemainingTicks(entity, mobEffect) <= remainingTicksAtMost;
    }

    private static int getRemainingTicks(LivingEntity entity, MobEffect mobEffect) {
        var effectInstance = entity.getEffect(mobEffect);
        return effectInstance == null ? 0 : effectInstance.getDuration();
    }
}
