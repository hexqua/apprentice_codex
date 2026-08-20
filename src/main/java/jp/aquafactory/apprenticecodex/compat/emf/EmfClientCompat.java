package jp.aquafactory.apprenticecodex.compat.emf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import net.minecraft.world.entity.player.Player;
import traben.entity_model_features.EMFAnimationApi;

public final class EmfClientCompat {
    public static final String HOVERRIDE_BROOM_RIDER_VARIABLE =
            "is_apprenticecodex_hoverride_broom_rider";
    private static final String HOVERRIDE_BROOM_RIDER_EXPLANATION =
            "emf.variable.apprenticecodex.is_hoverride_broom_rider";

    private EmfClientCompat() {
    }

    public static void register() throws Exception {
        // EMF は評価中の描画対象を共有コンテキストで公開するため、モデル評価の都度その乗り物を判定する。
        EMFAnimationApi.registerSingletonAnimationVariable(
                ApprenticeCodex.MODID,
                HOVERRIDE_BROOM_RIDER_VARIABLE,
                HOVERRIDE_BROOM_RIDER_EXPLANATION,
                EmfClientCompat::isHoverrideBroomRider
        );
    }

    private static boolean isHoverrideBroomRider() {
        return EMFAnimationApi.getCurrentEntity() instanceof Player player
                && player.getVehicle() instanceof HoverrideBroomEntity;
    }
}
