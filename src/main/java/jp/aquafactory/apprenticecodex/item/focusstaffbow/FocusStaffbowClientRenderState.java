package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FocusStaffbowClientRenderState {
    private FocusStaffbowClientRenderState() {
    }

    public static boolean shouldAccelerateCoreIdle(@Nullable ItemStack renderingStack, @Nullable ItemDisplayContext perspective) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof FocusStaffbow)) {
            return false;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return false;
        }

        var renderedHand = resolveRenderedHand(player, perspective);
        if (renderedHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        if (!ItemStack.isSameItemSameTags(player.getMainHandItem(), renderingStack)) {
            return false;
        }

        if (FocusStaffbowClientCastState.hasPendingCast(player)) {
            return true;
        }

        var syncedSpellData = ClientMagicData.getSyncedSpellData(player);
        return syncedSpellData.isCasting()
                && SpellSelectionManager.MAINHAND.equals(syncedSpellData.getCastingEquipmentSlot());
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
