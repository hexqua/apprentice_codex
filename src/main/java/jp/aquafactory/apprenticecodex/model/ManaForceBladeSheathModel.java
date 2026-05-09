package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeSheathItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaForceBladeSheathModel extends GeoModel<ManaForceBladeSheathItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/mana_force_blade_sheath.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/mana_force_blade_sheath.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/mana_force_blade_sheath.animation.json");

    @Override
    public ResourceLocation getModelResource(ManaForceBladeSheathItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ManaForceBladeSheathItem animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(ManaForceBladeSheathItem animatable) {
        return ANIM;
    }
}
