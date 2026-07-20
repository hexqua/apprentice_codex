package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ChargecastCatalystbookModel extends GeoModel<ChargecastCatalystbook> {
    private static final ResourceLocation OPEN_MODEL = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "geo/chargecast_catalystbook_open.geo.json"
    );
    private static final ResourceLocation CLOSE_MODEL = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "geo/chargecast_catalystbook_close.geo.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/chargecast_catalystbook.png"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "animations/chargecast_catalystbook.animation.json"
    );
    private final boolean open;

    public ChargecastCatalystbookModel(boolean open) {
        this.open = open;
    }

    @Override
    public ResourceLocation getModelResource(ChargecastCatalystbook animatable) {
        return open ? OPEN_MODEL : CLOSE_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChargecastCatalystbook animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChargecastCatalystbook animatable) {
        return ANIMATION;
    }
}
