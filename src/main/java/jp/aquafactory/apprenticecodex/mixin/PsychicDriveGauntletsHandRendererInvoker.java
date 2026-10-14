package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface PsychicDriveGauntletsHandRendererInvoker {
    @Invoker("renderArmWithItem")
    void apprenticecodex$renderGauntletsEmptyHand(AbstractClientPlayer player, float partialTicks, float pitch,
                                               InteractionHand hand, float swingProgress, ItemStack stack,
                                               float equipProgress, PoseStack poseStack, MultiBufferSource buffer,
                                               int packedLight);
}
