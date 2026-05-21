package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ClientItemRenderContext;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffClientRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MulticastEchoStaffModel extends GeoModel<MulticastEchoStaff> {
    private static final String SHARD_BONE = "shard";
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/multicast_echo_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/multicast_echo_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/multicast_echo_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(MulticastEchoStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MulticastEchoStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(MulticastEchoStaff animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(MulticastEchoStaff animatable, long instanceId,
                                    AnimationState<MulticastEchoStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var shard = getBone(SHARD_BONE).orElse(null);
        if (shard == null) {
            return;
        }

        var itemStack = animationState.getData(DataTickets.ITEMSTACK);
        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var initial = shard.getInitialSnapshot();
        var baseRotY = initial == null ? 0.0F : initial.getRotY();
        shard.setRotY(baseRotY + MulticastEchoStaffClientRenderState.resolveShardRotation(
                itemStack,
                perspective,
                ClientItemRenderContext.getRenderingEntity(),
                animationState.getPartialTick()
        ));
    }
}
