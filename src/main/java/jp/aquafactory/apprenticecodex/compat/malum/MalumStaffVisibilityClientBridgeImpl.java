package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.config.CommonConfig;
import com.sammy.malum.core.handlers.hiding.HiddenTagHandler;
import com.sammy.malum.registry.common.item.MalumItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MalumStaffVisibilityClientBridgeImpl {
    private MalumStaffVisibilityClientBridgeImpl() {}

    static boolean isAnyStaffVisible() {
        // isHiddenItem 自体は設定を見ないため、JEI・クリエイティブタブと同じ設定条件も含める.
        // 開示状態のキャッシュや更新通知を追加せず、Malum の現在のタグと開示状態を読む.
        return !CommonConfig.HIDE_RECIPES.getConfigValue()
                || !HiddenTagHandler.isHiddenItem(MalumItems.MNEMONIC_HEX_STAFF.get().getDefaultInstance())
                || !HiddenTagHandler.isHiddenItem(MalumItems.EROSION_SCEPTER.get().getDefaultInstance())
                || !HiddenTagHandler.isHiddenItem(MalumItems.UNWINDING_CHAOS.get().getDefaultInstance());
    }
}
