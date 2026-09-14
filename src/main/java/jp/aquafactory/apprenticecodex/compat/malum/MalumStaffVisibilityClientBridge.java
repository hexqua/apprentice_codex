package jp.aquafactory.apprenticecodex.compat.malum;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MalumStaffVisibilityClientBridge {
    private MalumStaffVisibilityClientBridge() {}

    public static boolean isAnyStaffVisible() {
        return MalumStaffChargeBridge.isAvailable() && MalumStaffVisibilityClientBridgeImpl.isAnyStaffVisible();
    }
}
