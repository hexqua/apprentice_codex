package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractSpellAmplifierItem;
import jp.aquafactory.apprenticecodex.model.SpellAmplifierModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SpellAmplifierRenderer extends GeoItemRenderer<AbstractSpellAmplifierItem> {
    private static final String CIRCUIT_EMISSIVE_BONE = "circuit_emissive";
    private static final String DEVICE_CORE_BONE = "device_core";

    public SpellAmplifierRenderer() {
        super(new SpellAmplifierModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AbstractSpellAmplifierItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, CIRCUIT_EMISSIVE_BONE)) {
            var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
            var schoolType = MagicTools.getImbuedSpellSchool(currentStack);
            if (schoolType == null) {
                return;
            }

            int circuitColor = MagicTools.resolveSchoolTintColor(schoolType);
            float circuitRed = ((circuitColor >> 16) & 0xFF) / 255.0f;
            float circuitGreen = ((circuitColor >> 8) & 0xFF) / 255.0f;
            float circuitBlue = (circuitColor & 0xFF) / 255.0f;
            // circuit_emissive は学派色の加算発光だけを通し、foil を重ねない。
            var additiveRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                    "spell_amplifier_circuit_additive",
                    getTextureLocation(animatable)
            );
            super.renderRecursively(
                    poseStack, animatable, bone, additiveRenderType, bufferSource, bufferSource.getBuffer(additiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    circuitRed, circuitGreen, circuitBlue, alpha
            );
            return;
        }

        if (isBoneOrChildOf(bone, DEVICE_CORE_BONE)) {
            // 中核部は base pass を通さず、軽い明滅つき emissive のみで描画する。
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            float brightness = resolveCoreBrightness(partialTick);
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    red * brightness, green * brightness, blue * brightness, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private static float resolveCoreBrightness(float partialTick) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return 0.94f;
        }

        float time = level.getGameTime() + partialTick;
        return 0.88f + 0.12f * (0.5f + 0.5f * Mth.sin(time * 0.18f));
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }
}
