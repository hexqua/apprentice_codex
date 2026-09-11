package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.ChargeSpellLayer;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChargeSpellLayer.class, remap = false)
public abstract class LightningArrowChargeSpellLayerMixin {
    @Inject(method = "handleRender", at = @At("TAIL"))
    private static void apprenticecodex$renderLightningArrow(PoseStack pose, MultiBufferSource buffers, int light,
                                                            LivingEntity entity, String spellId, boolean offhand, CallbackInfo ci) {
        // Iron'sは魔法IDで手元の描画を選ぶ。既存レイヤーを通すことでMagicArrowと同じ対応範囲にする。
        if (!spellId.equals(SpellRegistry.LIGHTNING_ARROW.get().getSpellId())
                || !ClientMagicData.getSyncedSpellData(entity).isCasting()) return;
        pose.pushPose();
        pose.translate((offhand ? -1.0F : 1.0F) / 32, 0.5, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        LightningArrowRenderer.renderModel(pose, buffers);
        pose.popPose();
    }
}
