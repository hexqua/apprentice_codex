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
        // 全体のpause/resume操作は他MODの停止状態も解除し得るため、独立した条件として追加する。
        // モデル・テクスチャは置換せず、ライフルから呼び出した一人称の手描画だけを対象にする。
        EMFAnimationApi.registerPauseCondition(entity -> entity instanceof Player player
                && EmfCompat.isRenderingSpellrifleHand(player));
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
