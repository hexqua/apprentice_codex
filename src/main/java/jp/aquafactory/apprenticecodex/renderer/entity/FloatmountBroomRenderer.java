package jp.aquafactory.apprenticecodex.renderer.entity;

import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class FloatmountBroomRenderer extends BroomEntityRenderer<FloatmountBroomEntity> {
    public FloatmountBroomRenderer(EntityRendererProvider.Context context) {
        super(context, new FloatmountBroomModel<>());
    }
}
