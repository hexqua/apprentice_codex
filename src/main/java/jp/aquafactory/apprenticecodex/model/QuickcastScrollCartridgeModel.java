package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class QuickcastScrollCartridgeModel extends GeoModel<QuickcastScrollCartridge> {
    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    @Override
    public ResourceLocation getModelResource(QuickcastScrollCartridge item) {
        return resource("geo/quickcast_scroll_cartridge.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(QuickcastScrollCartridge item) {
        return resource("textures/geo/quickcast_scroll_cartridge.png");
    }

    @Override
    public ResourceLocation getAnimationResource(QuickcastScrollCartridge item) {
        return resource("animations/quickcast_scroll_cartridge.animation.json");
    }
}
