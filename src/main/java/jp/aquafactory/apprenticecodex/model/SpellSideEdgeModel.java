package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SpellSideEdgeModel extends GeoModel<SpellSideEdge> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spell_side_edge.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spell_side_edge.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spell_side_edge.animation.json");

    @Override
    public ResourceLocation getModelResource(SpellSideEdge animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SpellSideEdge animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpellSideEdge animatable) {
        return ANIMATION;
    }
}
