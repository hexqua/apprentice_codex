package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ElementalBowClientRenderState {
    private static final float OVERHEAT_PULSE_PERIOD_TICKS = 18.0F;
    private static final float OVERHEAT_PULSE_ATTACK_PORTION = 0.18F;
    private static final float OVERHEAT_BASE_ALPHA_MIN = 0.60F;
    private static final float OVERHEAT_BASE_ALPHA_MAX = 0.76F;
    private static final float OVERHEAT_WARNING_MIN_ALPHA = 0.14F;
    private static final float OVERHEAT_WARNING_MAX_ALPHA = 0.70F;
    private static final float OVERHEAT_WARNING_MIN_SCALE = 1.01F;
    private static final float OVERHEAT_WARNING_MAX_SCALE = 1.10F;
    private static final OrbRenderState HIDDEN = new OrbRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);

    private ElementalBowClientRenderState() {
    }

    public static boolean shouldPlayDrawAnimation(@Nullable ItemStack renderingStack, @Nullable ItemDisplayContext perspective) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof ElementalBow)) {
            return false;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return false;
        }

        var hand = resolveRenderedHand(player, perspective);
        if (hand == null || !player.isUsingItem() || player.getUsedItemHand() != hand) {
            return false;
        }

        return matchesRenderingStack(player.getUseItem(), renderingStack);
    }

    public static OrbRenderState resolveOrbState(@Nullable ItemStack renderingStack, @Nullable ItemDisplayContext perspective, float partialTick) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof ElementalBow)) {
            return HIDDEN;
        }

        var schoolId = ElementalBow.getConfiguredSchoolId(renderingStack);
        var mode = ElementalBowModeManager.getResolvedDefinition(schoolId);
        if (mode == null) {
            return HIDDEN;
        }

        var color = mode.color();
        var alpha = 1.0F;
        var warningAlpha = 0.0F;
        var warningPulse = 0.0F;
        var warningScale = 1.0F;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null && isRenderedHeldStack(player, renderingStack, perspective)) {
            var state = ElementalBowOverheatManager.getState(player, schoolId);
            if (state.active()) {
                warningPulse = resolveOverheatWarningPulse(partialTick);
                alpha = Mth.lerp(warningPulse, OVERHEAT_BASE_ALPHA_MAX, OVERHEAT_BASE_ALPHA_MIN);
                warningAlpha = Mth.lerp(warningPulse, OVERHEAT_WARNING_MIN_ALPHA, OVERHEAT_WARNING_MAX_ALPHA);
                warningScale = Mth.lerp(warningPulse, OVERHEAT_WARNING_MIN_SCALE, OVERHEAT_WARNING_MAX_SCALE);
            }
        }

        return new OrbRenderState(
                true,
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                alpha,
                warningAlpha,
                warningPulse,
                warningScale
        );
    }

    private static float resolveOverheatWarningPulse(float partialTick) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        float time = level == null ? partialTick : level.getGameTime() + partialTick;
        var phase = (time % OVERHEAT_PULSE_PERIOD_TICKS) / OVERHEAT_PULSE_PERIOD_TICKS;
        float pulse = phase <= OVERHEAT_PULSE_ATTACK_PORTION
                ? phase / OVERHEAT_PULSE_ATTACK_PORTION
                : 1.0F - (phase - OVERHEAT_PULSE_ATTACK_PORTION) / (1.0F - OVERHEAT_PULSE_ATTACK_PORTION);
        pulse = Mth.clamp(pulse, 0.0F, 1.0F);
        return pulse * pulse * (3.0F - 2.0F * pulse);
    }

    private static boolean isRenderedHeldStack(Player player, ItemStack renderingStack, @Nullable ItemDisplayContext perspective) {
        var hand = resolveRenderedHand(player, perspective);
        if (hand != null) {
            return matchesRenderingStack(player.getItemInHand(hand), renderingStack);
        }

        return matchesRenderingStack(player.getMainHandItem(), renderingStack)
                || matchesRenderingStack(player.getOffhandItem(), renderingStack);
    }

    private static boolean matchesRenderingStack(ItemStack expectedHandStack, ItemStack renderingStack) {
        return !expectedHandStack.isEmpty() && ItemStack.isSameItemSameTags(expectedHandStack, renderingStack);
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(Player player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(Player player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public record OrbRenderState(
            boolean visible,
            float red,
            float green,
            float blue,
            float alpha,
            float warningAlpha,
            float warningPulse,
            float warningScale
    ) {
    }
}
