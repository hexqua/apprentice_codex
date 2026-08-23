package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorCalibration;
import net.minecraft.world.entity.EquipmentSlot;

/** Create本体が存在するときだけ読み込まれる、防具調整とゴーグル判定の接着コード。 */
@SuppressWarnings("unused")
public final class EndgameArmorCreateCompat {
    private EndgameArmorCreateCompat() {
    }

    public static void register() {
        GogglesItem.addIsWearingPredicate(player ->
                EndgameArmorCalibration.hasCreateGoggles(player.getItemBySlot(EquipmentSlot.HEAD))
        );
    }
}
