package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.model.QuickcastScrollCartridgeModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class QuickcastScrollCartridgeRenderer extends GeoItemRenderer<QuickcastScrollCartridge> {
    private static final int MIN_STAR_LIGHT = 10;
    private boolean renderingCore;

    public QuickcastScrollCartridgeRenderer() { super(new QuickcastScrollCartridgeModel()); }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay, int colour) {
        // キューブだけを選別し、core の親である cover の変形は両パスで共有する。
        if (isBoneOrChildOf(bone, "core") != renderingCore) {
            return;
        }
        if (!renderingCore && isBoneOrChildOf(bone, "star")) {
            packedLight = LightTexture.pack(Math.max(MIN_STAR_LIGHT, LightTexture.block(packedLight)),
                    LightTexture.sky(packedLight));
        }
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public void postRender(PoseStack poseStack, QuickcastScrollCartridge animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        if (isReRender) {
            return;
        }
        var coreColour = resolveCoreColour();
        if (coreColour == null) {
            return;
        }

        // 発光には通常パスの色・Glint・overlay を引き継がず、選択スクロールの学派色だけを使う。
        var coreRenderType = RenderType.entityTranslucentEmissive(getTextureLocation(animatable));
        renderingCore = true;
        try {
            reRender(model, poseStack, bufferSource, animatable, coreRenderType,
                    bufferSource.getBuffer(coreRenderType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, coreColour);
        } finally {
            renderingCore = false;
        }
    }

    private @Nullable Integer resolveCoreColour() {
        if (currentItemStack == null || currentItemStack.isEmpty()) {
            return null;
        }
        var data = QuickcastScrollCartridge.getSelectedSpellData(currentItemStack);
        if (data == SpellData.EMPTY || data.getSpell() == null || data.getSpell() == SpellRegistry.none()) {
            return null;
        }
        var school = data.getSpell().getSchoolType();
        if (school == null) {
            return null;
        }
        var color = school.getDisplayName().getStyle().getColor();
        // 色を解決できない学派では、代替色で表示せず core 自体を隠す。
        return color == null ? null : 0xFF000000 | (color.getValue() & 0xFFFFFF);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String name) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (name.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
