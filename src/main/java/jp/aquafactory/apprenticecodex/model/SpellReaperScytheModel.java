package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SpellReaperScytheModel extends GeoModel<SpellReaperScythe> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/spell_reaper_scythe.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spell_reaper_scythe.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/spell_reaper_scythe.animation.json");

    @Override
    public ResourceLocation getModelResource(SpellReaperScythe animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SpellReaperScythe animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpellReaperScythe animatable) {
        return ANIMATION;
    }
}
