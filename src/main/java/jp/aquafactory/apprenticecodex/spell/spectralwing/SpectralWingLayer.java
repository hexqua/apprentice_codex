package jp.aquafactory.apprenticecodex.spell.spectralwing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public class SpectralWingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final float WING_ALPHA = 0.82F;
    private static final ResourceLocation WINGS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/entity/spectral_wings.png"
    );

    private final ElytraModel<T> spectralWingModel;

    public SpectralWingLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.spectralWingModel = new ElytraModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA)
        );
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!shouldRender(entity)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.125D);
        this.getParentModel().copyPropertiesTo(this.spectralWingModel);
        this.spectralWingModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(WINGS_TEXTURE));
        this.spectralWingModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private boolean shouldRender(T entity) {
        return !entity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.SPECTRAL_WING.get()));
    }
}
