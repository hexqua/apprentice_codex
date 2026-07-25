package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.effect.InstantManaEffect;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.jetbrains.annotations.NotNull;

public final class ManaPotionRecoveryHelper {
    private ManaPotionRecoveryHelper() {
    }

    public static boolean isSupportedManaPotion(@NotNull ItemStack stack) {
        if (!stack.is(Items.POTION) || !MobEffectRegistry.INSTANT_MANA.isPresent()) {
            return false;
        }

        var effects = PotionUtils.getMobEffects(stack);
        return !effects.isEmpty()
                && effects.stream().allMatch(effect -> effect.getEffect() == MobEffectRegistry.INSTANT_MANA.get());
    }

    public static int getRecovery(@NotNull ItemStack potionStack, int maxMana, int amplifierBonus) {
        if (!isSupportedManaPotion(potionStack)) {
            return 0;
        }

        for (var effect : PotionUtils.getMobEffects(potionStack)) {
            if (effect.getEffect() != MobEffectRegistry.INSTANT_MANA.get()) {
                continue;
            }

            var level = effect.getAmplifier() + amplifierBonus + 1;
            if (level <= 0) {
                return 0;
            }

            // Iron's Spells の通常回復式を、マナを受け取る対象の最大マナへ置き換えて再現する。
            return (int) (
                    level * InstantManaEffect.manaPerAmplifier
                            + Math.max(0, maxMana) * (level * InstantManaEffect.manaPerAmplifierPercent)
            );
        }
        return 0;
    }
}
