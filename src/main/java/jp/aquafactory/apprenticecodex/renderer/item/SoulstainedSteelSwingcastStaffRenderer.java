package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.ClientItemRenderContext;
import jp.aquafactory.apprenticecodex.item.ImbuedSpellCoreClientEffectState;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaff;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaffClientManaCost;
import jp.aquafactory.apprenticecodex.model.SoulstainedSteelSwingcastStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SoulstainedSteelSwingcastStaffRenderer extends GeoItemRenderer<SoulstainedSteelSwingcastStaff> {
    private static final String STAFF_CORE_BONE = "staff_core";
    private static final String ORB_NONE_BONE = "orb_none";
    private static final String ORB_CONTAIN_BONE = "orb_contain";

    public SoulstainedSteelSwingcastStaffRenderer() {
        super(new SoulstainedSteelSwingcastStaffModel());
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            SoulstainedSteelSwingcastStaff animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (isBoneOrChildOf(bone, ORB_NONE_BONE)) {
            return;
        }

        if (isBoneOrChildOf(bone, STAFF_CORE_BONE)) {
            var stack = currentItemStack != null ? currentItemStack : ItemStack.EMPTY;
            var requiredMana = SoulstainedSteelSwingcastStaffClientManaCost.resolveFullBurstManaCost();
            var coreState = ImbuedSpellCoreClientEffectState.resolveWithManaRequirement(
                    stack,
                    ClientItemRenderContext.getRenderingEntity(),
                    partialTick,
                    requiredMana
            );
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    emissiveRenderType,
                    bufferSource,
                    bufferSource.getBuffer(emissiveRenderType),
                    isReRender,
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    coreState.red(), coreState.green(), coreState.blue(), coreState.alpha()
            );
            return;
        }

        if (isBoneOrChildOf(bone, ORB_CONTAIN_BONE)) {
            var additiveRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                    "soulstained_steel_swingcast_staff_orb_contain_additive",
                    getTextureLocation(animatable)
            );
            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    additiveRenderType,
                    bufferSource,
                    bufferSource.getBuffer(additiveRenderType),
                    isReRender,
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack,
                animatable,
                bone,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red, green, blue, alpha
        );
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
