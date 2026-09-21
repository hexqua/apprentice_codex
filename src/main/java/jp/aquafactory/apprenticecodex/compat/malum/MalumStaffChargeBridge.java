package jp.aquafactory.apprenticecodex.compat.malum;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public final class MalumStaffChargeBridge {
    private MalumStaffChargeBridge() {}
    public static boolean isAvailable() { return ModList.get().isLoaded("malum"); }
    public static boolean isWithinStaffChargeWindow(LivingEntity entity, int windowTicks) {
        return isAvailable() && MalumStaffChargeBridgeImpl.isWithinStaffChargeWindow(entity, windowTicks);
    }
    public static void clearHeldCooldowns(ServerPlayer player) {
        if (isAvailable()) MalumStaffChargeBridgeImpl.clearHeldCooldowns(player);
    }
}
