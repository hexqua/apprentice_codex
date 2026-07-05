package jp.aquafactory.apprenticecodex.spell.autoturret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.AutoTurretModel;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AutoTurretRenderer extends GeoEntityRenderer<AutoTurretEntity> {
    private static final float MODEL_RENDER_Y_OFFSET = 0.01f;
    private static final float SPRITE_ANCHOR_Y = 11.0f / 16.0f;
    private static final float AMMO_ICON_Y_OFFSET = AutoTurretEntity.HEIGHT + 0.25f;
    private static final float AMMO_ICON_SCALE = 0.45f;
    private static final float AMMO_ICON_SPIN_SPEED = 3.0f;
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/arrow.png");
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
        renderAmmoIcon(entity, partialTicks, poseStack, bufferSource);
    }

    private void renderCrossbowSprite(@NotNull AutoTurretEntity entity, float partialTicks,
                                      @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                                      int packedLight, ResourceLocation texture) {
        poseStack.pushPose();

        float nativeScale = entity.getScale();
        float ageInTicks = entity.tickCount + partialTicks;
        float lerpBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);

        poseStack.scale(nativeScale, nativeScale, nativeScale);
        applyRotations(entity, poseStack, ageInTicks, lerpBodyRot, partialTicks, nativeScale);

        // bone 行列ではなく、root 回転とアンカー位置を明示的に積んで描画姿勢を確定させる.
        poseStack.translate(0.0f, MODEL_RENDER_Y_OFFSET + SPRITE_ANCHOR_Y, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getAimPitch()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));

        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(poseStack, bufferSource, packedLight, texture);
        poseStack.popPose();
    }

    private void renderAmmoIcon(@NotNull AutoTurretEntity entity, float partialTicks,
                                @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource) {
        if (!shouldRenderAmmoIcon(entity)) {
            return;
        }

        var color = resolveAmmoIconColor(entity);
        poseStack.pushPose();
        poseStack.translate(0.0f, AMMO_ICON_Y_OFFSET, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * AMMO_ICON_SPIN_SPEED));
        poseStack.scale(AMMO_ICON_SCALE, AMMO_ICON_SCALE, AMMO_ICON_SCALE);
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT,
                ARROW_TEXTURE,
                ExtrudedSpriteRenderer.RenderMode.ADDITIVE_COLOR_ONLY,
                color.red,
                color.green,
                color.blue,
                1.0f
        );
        poseStack.popPose();
    }

    private static boolean shouldRenderAmmoIcon(AutoTurretEntity entity) {
        var player = Minecraft.getInstance().player;
        if (player == null || entity.getOwnerUuidForRendering().filter(player.getUUID()::equals).isEmpty()) {
            return false;
        }

        var initialBulletCount = entity.getInitialBulletCount();
        var restBulletCount = entity.getRestBulletCount();
        return initialBulletCount > 0 && restBulletCount < initialBulletCount;
    }

    private static AmmoIconColor resolveAmmoIconColor(AutoTurretEntity entity) {
        var initialBulletCount = entity.getInitialBulletCount();
        var restBulletCount = Math.max(0, entity.getRestBulletCount());
        if (restBulletCount <= 0) {
            return AmmoIconColor.EMPTY;
        }

        var restRatio = restBulletCount / (float) Math.max(1, initialBulletCount);
        return restRatio <= 0.25f ? AmmoIconColor.LOW : AmmoIconColor.REDUCED;
    }

    private record AmmoIconColor(float red, float green, float blue) {
        private static final AmmoIconColor REDUCED = fromRgb(0x88, 0x88, 0x88);
        private static final AmmoIconColor LOW = fromRgb(0x88, 0x88, 0x00);
        private static final AmmoIconColor EMPTY = fromRgb(0x88, 0x00, 0x00);

        private static AmmoIconColor fromRgb(int red, int green, int blue) {
            return new AmmoIconColor(red / 255.0f, green / 255.0f, blue / 255.0f);
        }
    }
}
