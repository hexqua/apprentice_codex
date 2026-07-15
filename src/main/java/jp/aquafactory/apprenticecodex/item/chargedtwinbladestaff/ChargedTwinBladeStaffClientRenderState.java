package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ChargedTwinBladeStaffClientRenderState {
    private ChargedTwinBladeStaffClientRenderState() {
    }

    public static boolean shouldAccelerateIdle(@Nullable ItemStack renderingStack, @Nullable ItemDisplayContext perspective) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof ChargedTwinBladeStaff)) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        var renderedHand = resolveRenderedHand(player, perspective);
        if (renderedHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        var useItem = player.getUseItem();
        return ItemStack.isSameItemSameComponents(player.getMainHandItem(), renderingStack)
                && ItemStack.isSameItemSameComponents(useItem, renderingStack);
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
}
