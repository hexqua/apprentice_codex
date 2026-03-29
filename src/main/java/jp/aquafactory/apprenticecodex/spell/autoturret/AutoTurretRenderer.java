package jp.aquafactory.apprenticecodex.spell.autoturret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.AutoTurretModel;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AutoTurretRenderer extends GeoEntityRenderer<AutoTurretEntity> {
    private static final String SPRITE_ANCHOR_BONE = "sprite_anchor";
    private static final float SPRITE_WORLD_LIFT = 10.0f / 16.0f;
    private static final ResourceLocation[] CROSSBOW_TEX = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_0.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_1.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_2.png"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/auto_turret_crossbow_3.png")
    };
    private Matrix4f spritePoseMatrix;
    private Matrix3f spriteNormalMatrix;
    private int spriteStage;

    public AutoTurretRenderer(EntityRendererProvider.Context context) {
        super(context, new AutoTurretModel());
    }

    @Override
    public void render(@NotNull AutoTurretEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        spritePoseMatrix = null;
        spriteNormalMatrix = null;
        spriteStage = 0;
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

        if (spritePoseMatrix != null && spriteNormalMatrix != null) {
            ExtrudedSpriteRenderer.renderCenter(spritePoseMatrix, spriteNormalMatrix, bufferSource, packedLight, CROSSBOW_TEX[spriteStage]);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AutoTurretEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (SPRITE_ANCHOR_BONE.equals(bone.getName())) {
            spriteStage = Math.max(0, Math.min(animatable.getStage(), CROSSBOW_TEX.length - 1));

            poseStack.pushPose();

            // 寝かせて描画.
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));

            var pose = poseStack.last();
            spritePoseMatrix = new Matrix4f(pose.pose()).translateLocal(0.0f, SPRITE_WORLD_LIFT, 0.0f);
            spriteNormalMatrix = new Matrix3f(pose.normal());
            poseStack.popPose();
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }
}
