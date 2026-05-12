package jp.aquafactory.apprenticecodex.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SmashcastScepterClientRenderState {
    private SmashcastScepterClientRenderState() {
    }

    public static boolean shouldPlayReadyAnimation(@Nullable ItemStack renderingStack,
                                                   @Nullable ItemDisplayContext perspective) {
        if (renderingStack == null || renderingStack.isEmpty()
                || !(renderingStack.getItem() instanceof SmashcastScepter)) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        return player != null
                && isRenderedMainHandStack(player, renderingStack, perspective)
                && SmashcastScepter.isSmashAttack(player);
    }

    private static boolean isRenderedMainHandStack(Player player, ItemStack renderingStack,
                                                   @Nullable ItemDisplayContext perspective) {
        var renderedHand = resolveRenderedHand(player, perspective);
        return renderedHand == InteractionHand.MAIN_HAND
                && ItemStack.isSameItemSameTags(player.getMainHandItem(), renderingStack);
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(Player player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(Player player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
