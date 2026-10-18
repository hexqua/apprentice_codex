package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseDevice;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ManaForceReleaseDeviceModel extends GeoModel<ManaForceReleaseDevice> {
    @Override
    public ResourceLocation getModelResource(ManaForceReleaseDevice item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/" + item.tier().id + ".geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ManaForceReleaseDevice item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/" + item.tier().id + ".png");
    }
    @Override
    public ResourceLocation getAnimationResource(ManaForceReleaseDevice item) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/" + item.tier().id + ".animation.json");
    }
}
