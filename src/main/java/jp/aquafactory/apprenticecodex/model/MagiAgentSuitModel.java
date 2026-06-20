package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MagiAgentSuitModel extends GeoModel<MagiAgentSuitItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/magi_agent_suit.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/magi_agent_suit.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/magi_agent_suit.animation.json");

    @Override
    public ResourceLocation getModelResource(MagiAgentSuitItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MagiAgentSuitItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MagiAgentSuitItem animatable) {
        return ANIMATION;
    }
}
