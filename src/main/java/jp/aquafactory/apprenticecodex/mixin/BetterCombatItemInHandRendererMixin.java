package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class BetterCombatItemInHandRendererMixin {
    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Invoker("renderArmWithItem")
    protected abstract void apprenticecodex$renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight
    );

    @Inject(
            method = "renderHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"
            )
    )
    private void apprenticecodex$renderBetterCombatScrollcasterOffhandWhileCasting(
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            LocalPlayer player,
            int combinedLight,
            CallbackInfo callback
    ) {
        if (!ApprenticeCodexClientConfig.enableBetterCombatScrollcasterGauntletFirstPersonOffhandVisual()) {
            return;
        }
        if (!offHandItem.isEmpty() || !BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return;
        }
        var syncedSpellData = ClientMagicData.getSyncedSpellData(player);
        if (!syncedSpellData.isCasting()
                || !SpellSelectionManager.OFFHAND.equals(syncedSpellData.getCastingEquipmentSlot())) {
            return;
        }

        var offhandStack = BetterCombatScrollcasterGauntletCompat.getPhysicalOffhandStack(player);
        if (offhandStack.isEmpty()) {
            return;
        }

        var swingProgress = player.swingingArm == InteractionHand.OFF_HAND ? player.getAttackAnim(partialTicks) : 0.0F;
        var pitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        var equippedProgress = 1.0F - Mth.lerp(partialTicks, oOffHandHeight, offHandHeight);
        apprenticecodex$renderArmWithItem(
                player,
                partialTicks,
                pitch,
                InteractionHand.OFF_HAND,
                swingProgress,
                offhandStack,
                equippedProgress,
                poseStack,
                buffer,
                combinedLight
        );
    }
}
