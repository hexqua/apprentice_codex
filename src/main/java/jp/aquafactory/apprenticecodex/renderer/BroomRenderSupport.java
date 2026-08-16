package jp.aquafactory.apprenticecodex.renderer;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.item.ImbuedSpellCoreClientEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;

public final class BroomRenderSupport {
    public static final String STAR_BONE = "star";
    public static final String CORE_BONE = "core";

    private static final float IDLE_PULSE_PERIOD_TICKS = 40.0F;
    private static final float IDLE_MIN_BRIGHTNESS = 0.9F;
    private static final float CAUTION_PULSE_PERIOD_TICKS = 60.0F;
    private static final float CAUTION_MIN_STRENGTH = 0.35F;
    private static final float CAUTION_MAX_STRENGTH = 0.75F;

    private BroomRenderSupport() {
    }

    public static int resolveStarColour(float partialTick) {
        return resolveIdleColour(partialTick);
    }

    public static int resolveItemCoreColour(float partialTick) {
        return resolveIdleColour(partialTick);
    }

    public static int resolveEntityCoreColour(AbstractBroomEntity broom, float partialTick) {
        return switch (broom.getCoreWarningState()) {
            case CRITICAL -> {
                var warning = ImbuedSpellCoreClientEffectState.resolveWarning(partialTick);
                yield composeColour(warning.red(), warning.green(), warning.blue(), warning.alpha());
            }
            case CAUTION -> resolveCautionColour(partialTick);
            case NONE -> resolveIdleColour(partialTick);
        };
    }

    public static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static int resolveIdleColour(float partialTick) {
        var brightness = idleBrightness(partialTick);
        return composeColour(brightness, brightness, brightness, 1.0F);
    }

    private static int resolveCautionColour(float partialTick) {
        var minecraft = Minecraft.getInstance();
        var time = minecraft.level == null ? partialTick : minecraft.level.getGameTime() + partialTick;
        var progress = (Mth.sin(time * Mth.TWO_PI / CAUTION_PULSE_PERIOD_TICKS) + 1.0F) * 0.5F;
        var strength = Mth.lerp(progress, CAUTION_MIN_STRENGTH, CAUTION_MAX_STRENGTH);
        return composeColour(strength, strength * 0.55F, strength * 0.08F, 0.9F);
    }

    private static float idleBrightness(float partialTick) {
        var minecraft = Minecraft.getInstance();
        var time = minecraft.level == null ? partialTick : minecraft.level.getGameTime() + partialTick;
        var progress = (Mth.sin(time * Mth.TWO_PI / IDLE_PULSE_PERIOD_TICKS) + 1.0F) * 0.5F;
        return Mth.lerp(progress, IDLE_MIN_BRIGHTNESS, 1.0F);
    }

    private static int composeColour(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
    }
}
