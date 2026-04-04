package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractSpellAmplifierItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpellAmplifierModel extends GeoModel<AbstractSpellAmplifierItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spell_amplifier.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spell_amplifier.animation.json");

    @Override
    public ResourceLocation getModelResource(AbstractSpellAmplifierItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSpellAmplifierItem animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSpellAmplifierItem animatable) {
        return ANIMATION;
    }
}
