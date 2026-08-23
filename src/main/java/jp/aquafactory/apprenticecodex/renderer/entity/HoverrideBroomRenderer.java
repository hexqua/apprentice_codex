package jp.aquafactory.apprenticecodex.renderer.entity;

import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.model.HoverrideBroomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class HoverrideBroomRenderer extends BroomEntityRenderer<HoverrideBroomEntity> {
    public HoverrideBroomRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverrideBroomModel<>());
    }
}
