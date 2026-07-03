package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public record AutocastAmuletSpellProfile(
        List<AutocastAmuletMobEffectCondition> mobEffects,
        Optional<Float> healthRatioAtMost
) {
    private static final Codec<AutocastAmuletSpellProfile> BASE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    AutocastAmuletMobEffectCondition.CODEC.listOf()
                            .optionalFieldOf("mob_effects", List.of())
                            .forGetter(AutocastAmuletSpellProfile::mobEffects),
                    Codec.floatRange(0.0F, 1.0F)
                            .optionalFieldOf("health_ratio_at_most")
                            .forGetter(AutocastAmuletSpellProfile::healthRatioAtMost)
            ).apply(instance, AutocastAmuletSpellProfile::new)
    );

    public static final Codec<AutocastAmuletSpellProfile> CODEC =
            BASE_CODEC.flatXmap(AutocastAmuletSpellProfile::validate, AutocastAmuletSpellProfile::validate);

    public boolean matches(LivingEntity entity) {
        if (healthRatioAtMost.isPresent() && !matchesHealthRatio(entity, healthRatioAtMost.get())) {
            return false;
        }

        for (var condition : mobEffects) {
            if (!condition.matches(entity)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesHealthRatio(LivingEntity entity, float threshold) {
        var maxHealth = entity.getMaxHealth();
        return maxHealth > 0.0F && entity.getHealth() / maxHealth <= threshold;
    }

    private static DataResult<AutocastAmuletSpellProfile> validate(AutocastAmuletSpellProfile profile) {
        if (profile.mobEffects().isEmpty() && profile.healthRatioAtMost().isEmpty()) {
            return DataResult.error(() -> "Autocast Amulet spell profile requires at least one condition");
        }
        return DataResult.success(profile);
    }
}
