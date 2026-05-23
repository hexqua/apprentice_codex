package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowClientRenderState {
    private static final float OVERHEAT_PULSE_PERIOD_TICKS = 18.0F;
    private static final float OVERHEAT_PULSE_ATTACK_PORTION = 0.18F;
    private static final float OVERHEAT_BASE_ALPHA_MIN = 0.60F;
    private static final float OVERHEAT_BASE_ALPHA_MAX = 0.76F;
    private static final float OVERHEAT_BASE_SCALE = 0.88F;
    private static final float OVERHEAT_WARNING_MIN_ALPHA = 0.14F;
    private static final float OVERHEAT_WARNING_MAX_ALPHA = 0.70F;
    private static final float OVERHEAT_WARNING_MIN_SCALE = 1.01F;
    private static final float OVERHEAT_WARNING_MAX_SCALE = 1.10F;
    private static final int ORB_RECOVERY_DURATION_TICKS = 40;
    private static final float ORB_ROTATION_SPEED_MIN = 7.5F * Mth.DEG_TO_RAD;
    private static final float ORB_ROTATION_SPEED_MAX = 13.0F * Mth.DEG_TO_RAD;
    private static final EnumMap<InteractionHand, HandAnimationState> HAND_ANIMATION_STATES = new EnumMap<>(InteractionHand.class);
    private static final RandomSource RANDOM = RandomSource.create();
    private static final OrbRotationState NO_ROTATION = new OrbRotationState(false, 0.0F, 0.0F, 0.0F);
    private static final OrbRenderState HIDDEN =
            new OrbRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
    private static final CastBarRenderState HIDDEN_CAST_BAR = new CastBarRenderState(false, 0.0F, 0.0F);

    static {
        for (var hand : InteractionHand.values()) {
            HAND_ANIMATION_STATES.put(hand, new HandAnimationState());
        }
    }

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
        var baseScale = 1.0F;
        var warningAlpha = 0.0F;
        var warningPulse = 0.0F;
        var warningScale = 1.0F;
        var rotation = resolveOrbRotation(renderingStack, perspective, partialTick);
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null && isRenderedHeldStack(player, renderingStack, perspective)) {
            var state = ElementalBowOverheatManager.getState(player, schoolId);
            if (state.active()) {
                warningPulse = resolveOverheatWarningPulse(partialTick);
                alpha = Mth.lerp(warningPulse, OVERHEAT_BASE_ALPHA_MAX, OVERHEAT_BASE_ALPHA_MIN);
                baseScale = OVERHEAT_BASE_SCALE;
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
                baseScale,
                warningAlpha,
                warningPulse,
                warningScale,
                rotation.rotX(),
                rotation.rotY(),
                rotation.rotZ()
        );
    }

    public static OrbRotationState resolveOrbRotation(@Nullable ItemStack renderingStack, @Nullable ItemDisplayContext perspective, float partialTick) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof ElementalBow)) {
            return NO_ROTATION;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return NO_ROTATION;
        }

        var hand = resolveRenderedHand(player, perspective);
        if (hand == null) {
            return NO_ROTATION;
        }

        var state = HAND_ANIMATION_STATES.get(hand);
        if (state == null || !state.matches(renderingStack)) {
            return NO_ROTATION;
        }

        return state.resolveRotation(level.getGameTime(), partialTick);
    }

    public static CastBarRenderState resolveCastBarState() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !player.isUsingItem()) {
            return HIDDEN_CAST_BAR;
        }

        var stack = player.getUseItem();
        if (!(stack.getItem() instanceof ElementalBow) || ElementalBow.getConfiguredSchoolId(stack) == null) {
            return HIDDEN_CAST_BAR;
        }

        float drawDuration = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        var requiredDrawTicks = ElementalBow.resolveMagicRequiredDrawTicks(stack);
        float completion = requiredDrawTicks <= 0 ? 1.0F : Mth.clamp(drawDuration / requiredDrawTicks, 0.0F, 1.0F);
        float remainingTicks = Math.max(0.0F, requiredDrawTicks - drawDuration);
        return new CastBarRenderState(true, completion, remainingTicks);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            resetAllHandStates();
            return;
        }

        updateHandAnimationState(player, level.getGameTime(), InteractionHand.MAIN_HAND);
        updateHandAnimationState(player, level.getGameTime(), InteractionHand.OFF_HAND);
    }

    private static void updateHandAnimationState(Player player, long gameTime, InteractionHand hand) {
        var state = HAND_ANIMATION_STATES.get(hand);
        if (state == null) {
            return;
        }

        var handStack = player.getItemInHand(hand);
        boolean usingElemental = isActiveElementalUse(player, hand);
        if (usingElemental) {
            state.beginDraw(handStack, gameTime);
            return;
        }

        if (state.drawing()) {
            state.beginRecovery(gameTime);
        }

        state.advanceRecovery(gameTime);
        if (!state.recovering() && !(handStack.getItem() instanceof ElementalBow)) {
            state.reset();
        }
    }

    private static boolean isActiveElementalUse(Player player, InteractionHand hand) {
        if (!player.isUsingItem() || player.getUsedItemHand() != hand) {
            return false;
        }

        var handStack = player.getItemInHand(hand);
        return handStack.getItem() instanceof ElementalBow
                && ElementalBow.getConfiguredSchoolId(handStack) != null
                && matchesRenderingStack(player.getUseItem(), handStack);
    }

    private static void resetAllHandStates() {
        for (var state : HAND_ANIMATION_STATES.values()) {
            state.reset();
        }
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
        return !expectedHandStack.isEmpty() && ItemStack.isSameItemSameComponents(expectedHandStack, renderingStack);
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

    private static float randomStartingRotation() {
        return RANDOM.nextFloat() * Mth.TWO_PI;
    }

    private static float randomRotationSpeed() {
        float speed = Mth.lerp(RANDOM.nextFloat(), ORB_ROTATION_SPEED_MIN, ORB_ROTATION_SPEED_MAX);
        return RANDOM.nextBoolean() ? speed : -speed;
    }

    public record OrbRenderState(
            boolean visible,
            float red,
            float green,
            float blue,
            float alpha,
            float baseScale,
            float warningAlpha,
            float warningPulse,
            float warningScale,
            float rotX,
            float rotY,
            float rotZ
    ) {
    }

    public record OrbRotationState(
            boolean applied,
            float rotX,
            float rotY,
            float rotZ
    ) {
    }

    public record CastBarRenderState(
            boolean visible,
            float completionPercent,
            float remainingTicks
    ) {
    }

    private static final class HandAnimationState {
        private ItemStack trackedStack = ItemStack.EMPTY;
        private boolean drawing;
        private long drawStartGameTime;
        private float drawStartRotX;
        private float drawStartRotY;
        private float drawStartRotZ;
        private float rotXSpeed;
        private float rotYSpeed;
        private float rotZSpeed;
        private boolean recovering;
        private long recoveryStartGameTime;
        private float recoveryStartRotX;
        private float recoveryStartRotY;
        private float recoveryStartRotZ;

        private void beginDraw(ItemStack stack, long gameTime) {
            if (drawing && matches(stack)) {
                trackedStack = stack.copy();
                return;
            }

            var currentRotation = resolveRotation(gameTime, 0.0F);
            trackedStack = stack.copy();
            drawing = true;
            recovering = false;
            drawStartGameTime = gameTime;
            drawStartRotX = currentRotation.applied() ? currentRotation.rotX() : randomStartingRotation();
            drawStartRotY = currentRotation.applied() ? currentRotation.rotY() : randomStartingRotation();
            drawStartRotZ = currentRotation.applied() ? currentRotation.rotZ() : randomStartingRotation();
            rotXSpeed = randomRotationSpeed();
            rotYSpeed = randomRotationSpeed();
            rotZSpeed = randomRotationSpeed();
        }

        private void beginRecovery(long gameTime) {
            var currentRotation = resolveRotation(gameTime, 0.0F);
            drawing = false;
            recoveryStartGameTime = gameTime;
            recoveryStartRotX = currentRotation.rotX();
            recoveryStartRotY = currentRotation.rotY();
            recoveryStartRotZ = currentRotation.rotZ();
            recovering = currentRotation.applied();
        }

        private void advanceRecovery(long gameTime) {
            if (!recovering) {
                return;
            }

            if (gameTime - recoveryStartGameTime >= ORB_RECOVERY_DURATION_TICKS) {
                reset();
            }
        }

        private OrbRotationState resolveRotation(long gameTime, float partialTick) {
            if (drawing) {
                float elapsed = (gameTime - drawStartGameTime) + partialTick;
                float accelerationDuration = Math.max(1.0F, ElementalBow.resolveMagicRequiredDrawTicks(trackedStack));
                return new OrbRotationState(
                        true,
                        drawStartRotX + resolveAcceleratedRotationOffset(elapsed, rotXSpeed, accelerationDuration),
                        drawStartRotY + resolveAcceleratedRotationOffset(elapsed, rotYSpeed, accelerationDuration),
                        drawStartRotZ + resolveAcceleratedRotationOffset(elapsed, rotZSpeed, accelerationDuration)
                );
            }

            if (!recovering) {
                return NO_ROTATION;
            }

            float progress = Mth.clamp(((gameTime - recoveryStartGameTime) + partialTick) / ORB_RECOVERY_DURATION_TICKS, 0.0F, 1.0F);
            float eased = easeOutSine(progress);
            return new OrbRotationState(
                    progress < 1.0F,
                    recoveryStartRotX * (1.0F - eased),
                    recoveryStartRotY * (1.0F - eased),
                    recoveryStartRotZ * (1.0F - eased)
            );
        }

        private static float resolveAcceleratedRotationOffset(float elapsed, float maxSpeed, float accelerationDuration) {
            if (elapsed <= 0.0F) {
                return 0.0F;
            }

            if (elapsed < accelerationDuration) {
                return maxSpeed * elapsed * elapsed / (2.0F * accelerationDuration);
            }

            return maxSpeed * (elapsed - accelerationDuration * 0.5F);
        }

        private boolean matches(ItemStack stack) {
            return !trackedStack.isEmpty() && ItemStack.isSameItemSameComponents(trackedStack, stack);
        }

        private boolean drawing() {
            return drawing;
        }

        private boolean recovering() {
            return recovering;
        }

        private void reset() {
            trackedStack = ItemStack.EMPTY;
            drawing = false;
            recovering = false;
            drawStartGameTime = 0L;
            recoveryStartGameTime = 0L;
            drawStartRotX = 0.0F;
            drawStartRotY = 0.0F;
            drawStartRotZ = 0.0F;
            rotXSpeed = 0.0F;
            rotYSpeed = 0.0F;
            rotZSpeed = 0.0F;
            recoveryStartRotX = 0.0F;
            recoveryStartRotY = 0.0F;
            recoveryStartRotZ = 0.0F;
        }
    }

    private static float easeOutSine(float progress) {
        return Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * Mth.HALF_PI);
    }
}
