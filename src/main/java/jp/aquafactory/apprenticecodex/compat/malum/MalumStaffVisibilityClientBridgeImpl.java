package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.core.handlers.hiding.HiddenTagHandler;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MalumStaffVisibilityClientBridgeImpl {
    private MalumStaffVisibilityClientBridgeImpl() {}

    static boolean isAnyStaffVisible() {
        // 1.6.7の開示条件はHiddenTagHandlerの非表示タグ一覧に集約される。
        var hidden = HiddenTagHandler.tagsToHide();
        return java.util.stream.Stream.of(ItemRegistry.MNEMONIC_HEX_STAFF.get(),
                        ItemRegistry.EROSION_SCEPTER.get(), ItemRegistry.STAFF_OF_THE_AURIC_FLAME.get())
                .anyMatch(item -> hidden.stream().noneMatch(item.getDefaultInstance()::is));
    }
}
