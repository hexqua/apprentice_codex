package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MulticastEchoStaffModel extends GeoModel<MulticastEchoStaff> {
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
}
