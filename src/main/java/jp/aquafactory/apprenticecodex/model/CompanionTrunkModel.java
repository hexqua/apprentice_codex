package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CompanionTrunkModel extends GeoModel<CompanionTrunkEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/companion_trunk.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/companion_trunk.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/companion_trunk.animation.json");

    @Override
    public ResourceLocation getModelResource(CompanionTrunkEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CompanionTrunkEntity animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(CompanionTrunkEntity animatable) {
        return ANIM;
    }
}
