package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.model.LuminousDeviceModel;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public final class LuminousDeviceRenderer extends GeoItemRenderer<LuminousDevice> {
    private static final String STAR_CORE_BONE = "star_core";
    private static final String PLACE_BONE = "place";
    private static final String CLEAN_BONE = "clean";
    private static final String SPELL_BONE = "spell";
    private static final String ORB_ANCHOR_BONE = "orb_anchor";
    private static final float STAR_CORE_PULSE_PERIOD_TICKS = 40.0F;
    private static final float DISPLAY_ITEM_SCALE = 0.2F;
    private static final float DISPLAY_ITEM_ROTATION_X_PER_TICK = 0.55F * 2;
    private static final float DISPLAY_ITEM_ROTATION_Y_PER_TICK = 0.85F * 2;
    private static final float DISPLAY_ITEM_ROTATION_Z_PER_TICK = 0.70F * 2;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/luminous_device.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType STAR_CORE_RENDER_TYPE = ApprenticeRenderTypes.entityTranslucentNoCull(
            "luminous_device_star_core_emissive",
            TEXTURE
    );
    private static final RenderType MODE_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;
    private LuminousDevice.Mode mode = LuminousDevice.Mode.PLACE;
    private ItemStack displayStack = ItemStack.EMPTY;
    private float starCoreBrightness = 1.0F;
    private float renderTime;

    public LuminousDeviceRenderer() {
        super(new LuminousDeviceModel());
        addRenderLayer(new DisplayItemLayer(this));
    }

    @Override
    public RenderType getRenderType(LuminousDevice animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void preRender(PoseStack poseStack, LuminousDevice animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int renderColor) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, renderColor);
        if (isReRender) {
            return;
        }

        var stack = currentItemStack == null ? ItemStack.EMPTY : currentItemStack;
        this.mode = LuminousDevice.getMode(stack);
        this.displayStack = resolveDisplayStack(stack, this.mode);
        this.renderTime = resolveRenderTime(partialTick);
        this.starCoreBrightness = resolveStarCoreBrightness(this.renderPerspective, this.renderTime);
    }

    @Override
    public void postRender(PoseStack poseStack, LuminousDevice animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, int renderColor) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, renderColor);
        if (isReRender) {
            return;
        }

        renderSpecialPass(
                model, poseStack, bufferSource, animatable, SpecialPass.STAR_CORE, STAR_CORE_RENDER_TYPE,
                partialTick, this.starCoreBrightness
        );
        renderSpecialPass(
                model, poseStack, bufferSource, animatable, SpecialPass.MODE, MODE_RENDER_TYPE,
                partialTick, 1.0F
        );
    }

    @Override
    public void renderRecursively(PoseStack poseStack, LuminousDevice animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int renderColor) {
        if (this.specialPass == SpecialPass.NONE) {
            if (isSpecialBone(bone)) {
                renderChildBonesOnly(
                        poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                        partialTick, packedLight, packedOverlay, renderColor
                );
                return;
            }

            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, renderColor
            );
            return;
        }

        var target = switch (this.specialPass) {
            case STAR_CORE -> isBoneOrChildOf(bone, STAR_CORE_BONE);
            case MODE -> isBoneOrChildOf(bone, boneForMode(this.mode));
            case NONE -> false;
        };
        if (target) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, renderColor
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
        this.mode = LuminousDevice.Mode.PLACE;
        this.displayStack = ItemStack.EMPTY;
        this.starCoreBrightness = 1.0F;
        this.renderTime = 0.0F;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   LuminousDevice animatable, SpecialPass pass, RenderType renderType,
                                   float partialTick, float brightness) {
        this.specialPass = pass;
        try {
            reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    colorForBrightness(brightness)
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, LuminousDevice animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int renderColor) {
        poseStack.pushPose();
        if (bone.isTrackingMatrices()) {
            var poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }
        RenderUtil.prepMatrixForBone(poseStack, bone);
        renderChildBones(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor
        );
        poseStack.popPose();
    }

    private void renderDisplayItem(PoseStack poseStack, LuminousDevice animatable, GeoBone bone,
                                   MultiBufferSource bufferSource, RenderType renderType, int packedLight) {
        if (this.displayStack.isEmpty() || this.displayStack.getItem() instanceof LuminousDevice) {
            return;
        }

        poseStack.pushPose();
        RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
        poseStack.mulPose(Axis.XP.rotationDegrees(this.renderTime * DISPLAY_ITEM_ROTATION_X_PER_TICK));
        poseStack.mulPose(Axis.YP.rotationDegrees(this.renderTime * DISPLAY_ITEM_ROTATION_Y_PER_TICK));
        poseStack.mulPose(Axis.ZP.rotationDegrees(this.renderTime * DISPLAY_ITEM_ROTATION_Z_PER_TICK));
        poseStack.scale(DISPLAY_ITEM_SCALE, DISPLAY_ITEM_SCALE, DISPLAY_ITEM_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                this.displayStack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                Minecraft.getInstance().level,
                (int)getInstanceId(animatable)
        );
        bufferSource.getBuffer(renderType);
        poseStack.popPose();
    }

    private static ItemStack resolveDisplayStack(ItemStack deviceStack, LuminousDevice.Mode mode) {
        if (mode == LuminousDevice.Mode.CLEAN) {
            return new ItemStack(Items.BRUSH);
        }
        if (mode == LuminousDevice.Mode.SPELL) {
            var selectedSpell = LuminousDevice.getSelectedSpellData(deviceStack);
            if (selectedSpell.getSpell() == SpellRegistry.MAGE_LIGHT.get()) {
                return new ItemStack(Items.TORCH);
            }
            if (selectedSpell.getSpell() == SpellRegistry.WIZARDLAMP.get()) {
                return new ItemStack(Items.LANTERN);
            }
            return ItemStack.EMPTY;
        }
        return LuminousDevice.getSelectedStack(deviceStack).copyWithCount(1);
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0.0F : level.getGameTime() + partialTick;
    }

    private static float resolveStarCoreBrightness(ItemDisplayContext perspective, float time) {
        if (!isHandheldPerspective(perspective)) {
            return 1.0F;
        }
        return 0.95F + 0.05F * Mth.sin(time * Mth.TWO_PI / STAR_CORE_PULSE_PERIOD_TICKS);
    }

    private static int colorForBrightness(float brightness) {
        var channel = Mth.clamp(Math.round(brightness * 255.0F), 0, 255);
        return 0xFF000000 | channel << 16 | channel << 8 | channel;
    }

    private static boolean isHandheldPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static boolean isSpecialBone(GeoBone bone) {
        return isBoneOrChildOf(bone, STAR_CORE_BONE)
                || isBoneOrChildOf(bone, PLACE_BONE)
                || isBoneOrChildOf(bone, CLEAN_BONE)
                || isBoneOrChildOf(bone, SPELL_BONE);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String boneForMode(LuminousDevice.Mode mode) {
        return switch (mode) {
            case PLACE -> PLACE_BONE;
            case CLEAN -> CLEAN_BONE;
            case SPELL -> SPELL_BONE;
        };
    }

    private final class DisplayItemLayer extends GeoRenderLayer<LuminousDevice> {
        private DisplayItemLayer(LuminousDeviceRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, LuminousDevice animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  float partialTick, int packedLight, int packedOverlay) {
            if (ORB_ANCHOR_BONE.equals(bone.getName())) {
                renderDisplayItem(poseStack, animatable, bone, bufferSource, renderType, packedLight);
            }
        }
    }

    private enum SpecialPass {
        NONE,
        STAR_CORE,
        MODE
    }
}
