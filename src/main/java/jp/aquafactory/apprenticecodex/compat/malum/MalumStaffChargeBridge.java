package jp.aquafactory.apprenticecodex.compat.malum;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class MalumStaffChargeBridge {
    private MalumStaffChargeBridge() {}
    public static boolean isAvailable() { return ModList.get().isLoaded("malum"); }
    public static void clearHeldCooldowns(ServerPlayer player) {
        if (isAvailable()) MalumStaffChargeBridgeImpl.clearHeldCooldowns(player);
    }
}
