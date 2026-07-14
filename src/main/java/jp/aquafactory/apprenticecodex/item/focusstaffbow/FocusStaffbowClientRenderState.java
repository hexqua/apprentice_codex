package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.player.LocalPlayer;
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

    public static FocusStaffbowChargeEffectState resolveChargeEffectState(@Nullable ItemStack renderingStack,
                                                                          @Nullable ItemDisplayContext perspective,
                                                                          @Nullable LivingEntity owner,
                                                                          float partialTick) {
        if (!isRenderedHeldFocusStaffbow(renderingStack, perspective, owner)) {
            return FocusStaffbowChargeEffectState.HIDDEN;
        }

        if (owner instanceof LocalPlayer localPlayer) {
            return FocusStaffbowClientCastState.resolveChargeEffectState(localPlayer);
        }

        return FocusStaffbowClientPresentationState.resolveChargeEffectState(owner.getUUID());
    }

    private static boolean isRenderedHeldFocusStaffbow(@Nullable ItemStack renderingStack,
                                                       @Nullable ItemDisplayContext perspective,
                                                       @Nullable LivingEntity owner) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof FocusStaffbow)) {
            return false;
        }
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        var renderedHand = resolveRenderedHand(owner, perspective);
        return renderedHand == InteractionHand.MAIN_HAND
                && ItemStack.isSameItemSameTags(owner.getMainHandItem(), renderingStack);
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(LivingEntity player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(LivingEntity player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
