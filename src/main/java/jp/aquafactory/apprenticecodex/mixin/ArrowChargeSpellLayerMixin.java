package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.ChargeSpellLayer;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowRenderer;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowRenderer;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowRenderer;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimArrowRenderer;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(value = ChargeSpellLayer.class, remap = false)
public abstract class ArrowChargeSpellLayerMixin {
    @Inject(method = "handleRender", at = @At("TAIL"))
    private static void apprenticecodex$renderArrow(PoseStack pose, MultiBufferSource buffers, int light,
                                                   LivingEntity entity, String spellId, boolean offhand, CallbackInfo ci) {
        // Iron'sは魔法IDで手元の描画を選ぶ。既存レイヤーを通すことでMagicArrowと同じ対応範囲にする。
        var renderer = apprenticecodex$findArrowRenderer(spellId);
        if (renderer == null || !ClientMagicData.getSyncedSpellData(entity).isCasting()) return;
        pose.pushPose();
        pose.translate((offhand ? -1.0F : 1.0F) / 32, 0.5, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        renderer.accept(pose, buffers);
        pose.popPose();
    }

    @Unique
    private static @Nullable BiConsumer<PoseStack, MultiBufferSource> apprenticecodex$findArrowRenderer(String spellId) {
        if (spellId.equals(SpellRegistry.LUNAR_AIM.get().getSpellId())) {
            return LunarAimArrowRenderer::renderCharge;
        }
        if (spellId.equals(SpellRegistry.LIGHTNING_ARROW.get().getSpellId())) {
            return LightningArrowRenderer::renderModel;
        }
        if (spellId.equals(SpellRegistry.BLOODY_ARROW.get().getSpellId())) {
            return BloodyArrowRenderer::renderModel;
        }
        if (spellId.equals(SpellRegistry.SACRED_ARROW.get().getSpellId())) {
            return SacredArrowRenderer::renderModel;
        }
        if (spellId.equals(SpellRegistry.ECHO_ARROW.get().getSpellId())) {
            return EchoArrowRenderer::renderModel;
        }
        return null;
    }
}
