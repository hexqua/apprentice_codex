package jp.aquafactory.apprenticecodex.compat.malum;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

public final class MalumStaffChargeBridge {
    private MalumStaffChargeBridge() {}

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MalumCompatibility.MOD_ID);
    }

    public static boolean needsRecovery(LivingEntity entity) {
        return isAvailable() && MalumStaffChargeBridgeImpl.needsRecovery(entity);
    }

    public static boolean isWithinStaffChargeWindow(LivingEntity entity, int windowTicks) {
        return isAvailable() && MalumStaffChargeBridgeImpl.isWithinStaffChargeWindow(entity, windowTicks);
    }

    public static void recoverFully(ServerPlayer player) {
        if (isAvailable()) MalumStaffChargeBridgeImpl.recoverFully(player);
    }

    public static void updateAttributes(ServerPlayer player, double duration, double recovery) {
        if (isAvailable()) MalumStaffChargeBridgeImpl.updateAttributes(player, duration, recovery);
    }
}
