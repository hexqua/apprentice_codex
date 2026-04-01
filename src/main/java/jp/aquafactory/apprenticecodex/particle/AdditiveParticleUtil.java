package jp.aquafactory.apprenticecodex.particle;

import net.minecraft.util.Mth;

// todo:ease-in-out-cubicはいろいろな場所で使っているのでutility配下に移動していずれ共通化する.
final class AdditiveParticleUtil {
    private AdditiveParticleUtil() {
    }

    static float easeOutCubic(float progress) {
        var clamped = Mth.clamp(progress, 0.0F, 1.0F);
        var inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    static float easeInCubic(float progress) {
        var clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped * clamped * clamped;
    }

    static float mixFromWhite(float targetColor, int age, int whitenTicks) {
        if (whitenTicks <= 0) {
            return targetColor;
        }

        var progress = easeOutCubic((float) age / (float) whitenTicks);
        return Mth.lerp(progress, 1.0F, targetColor);
    }

    static float computeAlpha(int age, int lifetime, float fadeInEnd, float fadeOutStart, float baseAlpha) {
        var lifetimeProgress = Mth.clamp((float) age / (float) lifetime, 0.0F, 1.0F);
        var fadeIn = easeOutCubic(lifetimeProgress / fadeInEnd);
        var fadeOutProgress = (lifetimeProgress - fadeOutStart) / (1.0F - fadeOutStart);
        var fadeOut = 1.0F - easeInCubic(fadeOutProgress);
        return baseAlpha * Mth.clamp(fadeIn, 0.0F, 1.0F) * Mth.clamp(fadeOut, 0.0F, 1.0F);
    }

    static float computeAlphaLinear(int age, int lifetime, float fadeInEnd, float fadeOutStart, float baseAlpha) {
        var lifetimeProgress = Mth.clamp((float) age / (float) lifetime, 0.0F, 1.0F);
        var safeFadeInEnd = Math.max(1.0e-4F, fadeInEnd);
        var safeFadeOutRange = Math.max(1.0e-4F, 1.0F - fadeOutStart);
        var fadeIn = Mth.clamp(lifetimeProgress / safeFadeInEnd, 0.0F, 1.0F);
        var fadeOutProgress = (lifetimeProgress - fadeOutStart) / safeFadeOutRange;
        var fadeOut = 1.0F - Mth.clamp(fadeOutProgress, 0.0F, 1.0F);
        return baseAlpha * fadeIn * fadeOut;
    }
}
