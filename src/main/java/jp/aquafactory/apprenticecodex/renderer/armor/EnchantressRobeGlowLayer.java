package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class EnchantressRobeGlowLayer extends AutoGlowingGeoLayer<EnchantressRobeItem> {
    public EnchantressRobeGlowLayer(GeoRenderer<EnchantressRobeItem> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, EnchantressRobeItem animatable, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        var schoolType = MagicTools.getImbuedSpellSchool(getCurrentStack());
        if (schoolType == null) {
            return;
        }

        int runeColor = EnchantressRobeRenderer.resolveSchoolTintColor(schoolType);
        float red = ((runeColor >> 16) & 0xFF) / 255.0f;
        float green = ((runeColor >> 8) & 0xFF) / 255.0f;
        float blue = (runeColor & 0xFF) / 255.0f;
        var glowRenderType = getRenderType(animatable);

        getRenderer().reRender(
                model,
                poseStack,
                bufferSource,
                animatable,
                glowRenderType,
                bufferSource.getBuffer(glowRenderType),
                partialTick,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                red,
                green,
                blue,
                1.0f
        );
    }

    private ItemStack getCurrentStack() {
        if (getRenderer() instanceof GeoArmorRenderer<?> armorRenderer) {
            return armorRenderer.getCurrentStack();
        }

        return ItemStack.EMPTY;
    }
}
