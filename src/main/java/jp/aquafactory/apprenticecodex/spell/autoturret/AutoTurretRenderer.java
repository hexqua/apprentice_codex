package jp.aquafactory.apprenticecodex.spell.autoturret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.AutoTurretModel;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AutoTurretRenderer extends GeoEntityRenderer<AutoTurretEntity> {
    private static final float MODEL_RENDER_Y_OFFSET = 0.01f;
    private static final float SPRITE_ANCHOR_Y = 10.0f / 16.0f;
    private static final ResourceLocation[] CROSSBOW_TEX = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_0.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_1.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_2.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_3.png")
    };

    public AutoTurretRenderer(EntityRendererProvider.Context context) {
        super(context, new AutoTurretModel());
    }

    @Override
    public void render(@NotNull AutoTurretEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        int spriteStage = Math.max(0, Math.min(entity.getStage(), CROSSBOW_TEX.length - 1));
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        renderCrossbowSprite(entity, partialTicks, poseStack, bufferSource, packedLight, CROSSBOW_TEX[spriteStage]);
    }

    private void renderCrossbowSprite(@NotNull AutoTurretEntity entity, float partialTicks,
                                      @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                                      int packedLight, ResourceLocation texture) {
        poseStack.pushPose();

        float nativeScale = entity.getScale();
        float ageInTicks = entity.tickCount + partialTicks;
        float lerpBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);

        poseStack.scale(nativeScale, nativeScale, nativeScale);
        applyRotations(entity, poseStack, ageInTicks, lerpBodyRot, partialTicks);

        // bone 行列ではなく、root 回転とアンカー位置を明示的に積んで描画姿勢を確定させる.
        poseStack.translate(0.0f, MODEL_RENDER_Y_OFFSET + SPRITE_ANCHOR_Y, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getAimPitch()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));

        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(poseStack, bufferSource, packedLight, texture);
        poseStack.popPose();
    }
}
