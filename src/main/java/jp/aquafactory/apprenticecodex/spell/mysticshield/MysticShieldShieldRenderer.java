package jp.aquafactory.apprenticecodex.spell.mysticshield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class MysticShieldShieldRenderer extends EntityRenderer<MysticShieldShieldEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/mystic_shield_shield.png");
    private static final float PANEL_X_OFFSET = 0.52f;
    private static final float OUTER_SCALE = 0.98f;
    private static final float INNER_SCALE = 0.68f;
    private static final float OUTER_SPIN_SPEED = 15.2f;
    private static final float INNER_SPIN_SPEED = 20.8f;

    public MysticShieldShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull MysticShieldShieldEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if (entity.isFading()) {
            return;
        }

        poseStack.pushPose();
        var yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        var age = entity.tickCount + partialTicks;
        drawShieldPanel(poseStack, buffer, -PANEL_X_OFFSET, age, 1.0f);
        drawShieldPanel(poseStack, buffer, PANEL_X_OFFSET, age, -1.0f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, LightTexture.FULL_BRIGHT);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MysticShieldShieldEntity entity) {
        return TEXTURE;
    }

    private static void drawShieldPanel(PoseStack poseStack, MultiBufferSource buffer, float xOffset,
                                        float age, float spinDirection) {
        poseStack.pushPose();
        poseStack.translate(xOffset, 0.0f, 0.0f);
        renderSpriteLayer(poseStack, buffer, age * OUTER_SPIN_SPEED * spinDirection, OUTER_SCALE);
        renderSpriteLayer(poseStack, buffer, -age * INNER_SPIN_SPEED * spinDirection + 45.0f, INNER_SCALE);
        poseStack.popPose();
    }

    private static void renderSpriteLayer(PoseStack poseStack, MultiBufferSource buffer, float spinDegrees, float scale) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinDegrees));
        poseStack.scale(scale, scale, scale);
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT,
                TEXTURE,
                ExtrudedSpriteRenderer.RenderMode.ADDITIVE_COLOR_ONLY
        );
        poseStack.popPose();
    }
}
