package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CircuitHeatStaffClientRenderState {
    private static final CircuitRenderState CIRCUIT_NORMAL = new CircuitRenderState(false, 1.0F, 1.0F, 1.0F, 0.0F);
    private static final CoreRenderState CORE_NORMAL = new CoreRenderState(1.0F, 1.0F, 1.0F, 0.72F, 1.0F);
    private static final float CIRCUIT_PULSE_MIN = 0.90F;
    private static final float CIRCUIT_PULSE_MAX = 1.00F;
    private static final float CIRCUIT_BYPASS_PERIOD_TICKS = 28.0F;
    private static final float CIRCUIT_OVERHEAT_PERIOD_TICKS = 6.0F;

    private CircuitHeatStaffClientRenderState() {
    }

    public static double resolveFrameAnimationSpeed(@Nullable ItemStack stack, @Nullable ItemDisplayContext perspective) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !isRenderedHeldStack(player, stack, perspective)) {
            return 1.0D;
        }

        if (CircuitHeatStaff.isStaffOverheated(stack, minecraft.level)) {
            return 0.1D;
        }

        if (!ClientMagicData.isCasting() || !matchesActiveUse(player, stack, perspective)) {
            return 1.0D;
        }

        var castType = ClientMagicData.getCastType();
        return castType == CastType.LONG || castType == CastType.CONTINUOUS ? 4.0D : 1.0D;
    }

    public static double resolveCogAnimationSpeed(@Nullable ItemStack stack, @Nullable ItemDisplayContext perspective) {
        var player = Minecraft.getInstance().player;
        if (player == null || !isRenderedHeldStack(player, stack, perspective)) {
            return 1.0D;
        }

        return CircuitHeatStaffOverheatManager.hasAnyActiveState(player) ? 4.0D : 1.0D;
    }

    public static CircuitRenderState resolveCircuit(ItemStack stack, @Nullable ItemDisplayContext perspective, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !isRenderedHeldStack(player, stack, perspective)) {
            return CIRCUIT_NORMAL;
        }

        var time = resolveTime(partialTick);
        if (CircuitHeatStaff.isStaffOverheated(stack, minecraft.level)) {
            var pulse = pulse(time, CIRCUIT_OVERHEAT_PERIOD_TICKS);
            return new CircuitRenderState(
                    true,
                    1.0F,
                    Mth.lerp(pulse, 1.0F, 0.18F),
                    Mth.lerp(pulse, 1.0F, 0.12F),
                    Mth.lerp(pulse, CIRCUIT_PULSE_MAX, CIRCUIT_PULSE_MIN)
            );
        }

        if (!CircuitHeatStaff.isSelectedSpellOverheated(player)) {
            return CIRCUIT_NORMAL;
        }

        var brightness = Mth.lerp(pulse(time, CIRCUIT_BYPASS_PERIOD_TICKS), CIRCUIT_PULSE_MIN, CIRCUIT_PULSE_MAX);
        return new CircuitRenderState(true, brightness, brightness, brightness, brightness);
    }

    public static CoreRenderState resolveCore(ItemStack stack, @Nullable ItemDisplayContext perspective, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !isRenderedHeldStack(player, stack, perspective)) {
            return CORE_NORMAL;
        }

        if (CircuitHeatStaff.isStaffOverheated(stack, minecraft.level)) {
            return new CoreRenderState(0.53F, 0.53F, 0.53F, 0.72F, 0.5F);
        }

        return CircuitHeatStaff.isSelectedSpellOverheated(player)
                ? new CoreRenderState(1.0F, 1.0F, 1.0F, 0.72F, 4.0F)
                : CORE_NORMAL;
    }

    public static float resolveCoreRotation(ItemStack stack, @Nullable ItemDisplayContext perspective, float partialTick) {
        var state = resolveCore(stack, perspective, partialTick);
        return resolveTime(partialTick) * 0.045F * state.rotationSpeedMultiplier();
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

    private static boolean matchesActiveUse(Player player, @Nullable ItemStack stack, @Nullable ItemDisplayContext perspective) {
        var hand = resolveRenderedHand(player, perspective);
        return hand != null
                && player.isUsingItem()
                && player.getUsedItemHand() == hand
                && matchesRenderingStack(player.getUseItem(), stack);
    }

    private static boolean isRenderedHeldStack(Player player, @Nullable ItemStack stack, @Nullable ItemDisplayContext perspective) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof CircuitHeatStaff)) {
            return false;
        }

        var hand = resolveRenderedHand(player, perspective);
        if (hand != null) {
            return matchesRenderingStack(player.getItemInHand(hand), stack);
        }

        return matchesRenderingStack(player.getMainHandItem(), stack)
                || matchesRenderingStack(player.getOffhandItem(), stack);
    }

    private static boolean matchesRenderingStack(ItemStack expectedStack, @Nullable ItemStack renderingStack) {
        return renderingStack != null
                && !expectedStack.isEmpty()
                && ItemStack.isSameItemSameTags(expectedStack, renderingStack);
    }

    private static float resolveTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static float pulse(float time, float periodTicks) {
        return 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / periodTicks);
    }

    public record CircuitRenderState(boolean glow, float red, float green, float blue, float alpha) {
    }

    public record CoreRenderState(float red, float green, float blue, float alpha, float rotationSpeedMultiplier) {
    }
}
