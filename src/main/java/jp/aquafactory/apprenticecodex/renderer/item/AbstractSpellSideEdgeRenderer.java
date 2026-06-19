package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.AbstractSpellSideEdgeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

abstract class AbstractSpellSideEdgeRenderer<T extends AbstractSpellSideEdgeItem> extends GeoItemRenderer<T> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spell_side_edge.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    protected AbstractSpellSideEdgeRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource,
                                    float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }
}
