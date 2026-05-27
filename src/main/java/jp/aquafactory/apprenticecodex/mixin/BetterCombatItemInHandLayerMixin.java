package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class BetterCombatItemInHandLayerMixin {
    @Shadow
    protected abstract void renderArmWithItem(
            LivingEntity livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    );

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("TAIL")
    )
    private void apprenticecodex$renderBetterCombatScrollcasterOffhand(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        if (!ApprenticeCodexClientConfig.enableBetterCombatScrollcasterGauntletThirdPersonOffhandVisual()) {
            return;
        }
        if (!player.getOffhandItem().isEmpty() || !BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return;
        }
        if (apprentice_codex$isDeniedMainhandItem(player.getMainHandItem())) {
            return;
        }

        var offhandStack = BetterCombatScrollcasterGauntletCompat.getPhysicalOffhandStack(player);
        if (offhandStack.isEmpty()) {
            return;
        }

        var offhandArm = player.getMainArm().getOpposite();
        var displayContext = offhandArm == HumanoidArm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        poseStack.pushPose();
        renderArmWithItem(player, offhandStack, displayContext, offhandArm, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Unique
    private static boolean apprentice_codex$isDeniedMainhandItem(ItemStack mainHandStack) {
        var itemId = ForgeRegistries.ITEMS.getKey(mainHandStack.getItem());
        return itemId != null
                && ApprenticeCodexClientConfig
                .isBetterCombatScrollcasterGauntletThirdPersonOffhandVisualDeniedForMainhandItem(itemId.toString());
    }
}
