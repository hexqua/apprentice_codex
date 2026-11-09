package jp.aquafactory.apprenticecodex.gametest.equipment.consumable;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.flask.FlaskScenarios.assertPotionEffect;

final class ConsumableScenarios {
    private ConsumableScenarios() {}

    static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = ItemRegistry.COMFORT_BERRIES.get().getFoodProperties();
            helper.assertTrue(foodProperties != null, "Comfort Berries should remain edible");
            helper.assertTrue(foodProperties != null && foodProperties.getNutrition() == 1,
                    "Comfort Berries nutrition regression: " + (foodProperties == null ? "null" : foodProperties.getNutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.getSaturationModifier() - 1.2f) < 1.0e-6F,
                    "Comfort Berries saturation modifier regression: "
                            + (foodProperties == null ? "null" : foodProperties.getSaturationModifier()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Berries should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<Pair<MobEffectInstance, Float>>of()
                    : foodProperties.getEffects().stream()
                    .filter(effectPair -> effectPair.getFirst().getEffect() == EffectRegistry.MANA_REGENERATION.get())
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Berries should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.getFirst().getDuration() == 20 * 10,
                    "Comfort Berries mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.getFirst().getAmplifier() == 2,
                    "Comfort Berries mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.getSecond() - 1.0f) < 1.0e-6F,
                    "Comfort Berries mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.getSecond()));
        });
    }

    static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = ItemRegistry.COMFORT_SANDWICH.get().getFoodProperties();
            helper.assertTrue(foodProperties != null, "Comfort Sandwich should be edible");
            helper.assertTrue(foodProperties != null && foodProperties.getNutrition() == 7,
                    "Comfort Sandwich nutrition regression: " + (foodProperties == null ? "null" : foodProperties.getNutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.getSaturationModifier() - 1.6f) < 1.0e-6F,
                    "Comfort Sandwich saturation modifier regression: "
                            + (foodProperties == null ? "null" : foodProperties.getSaturationModifier()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Sandwich should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<Pair<MobEffectInstance, Float>>of()
                    : foodProperties.getEffects().stream()
                    .filter(effectPair -> effectPair.getFirst().getEffect() == EffectRegistry.MANA_REGENERATION.get())
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Sandwich should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.getFirst().getDuration() == 20 * 60,
                    "Comfort Sandwich mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.getFirst().getAmplifier() == 0,
                    "Comfort Sandwich mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.getSecond() - 1.0f) < 1.0e-6F,
                    "Comfort Sandwich mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.getSecond()));
        });
    }

    static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertPotionEffect(helper, PotionRegistry.MEDITATION.get(), "apprenticecodex:meditation", 20 * 60 * 3, 0);
            assertPotionEffect(helper, PotionRegistry.LONG_MEDITATION.get(), "apprenticecodex:long_meditation", 20 * 60 * 8, 0);
            assertPotionEffect(helper, PotionRegistry.STRONG_MEDITATION.get(), "apprenticecodex:strong_meditation", 20 * 90, 1);
        });
    }
}
