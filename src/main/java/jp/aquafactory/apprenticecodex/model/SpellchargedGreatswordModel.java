package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SpellchargedGreatswordModel extends GeoModel<SpellchargedGreatsword> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spellcharged_greatsword.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spellcharged_greatsword.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spellcharged_greatsword.animation.json");

    @Override
    public ResourceLocation getModelResource(SpellchargedGreatsword animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SpellchargedGreatsword animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpellchargedGreatsword animatable) {
        return ANIMATION;
    }
}
