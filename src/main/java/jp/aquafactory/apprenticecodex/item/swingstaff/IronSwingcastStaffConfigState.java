package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.config.item.IronSwingcastStaffServerConfig;
import net.minecraft.util.Mth;

public final class IronSwingcastStaffConfigState {
    private static double crystallineArcaneShardDropChance =
            IronSwingcastStaffServerConfig.DEFAULT_CRYSTALLINE_ARCANE_SHARD_DROP_CHANCE;

    private IronSwingcastStaffConfigState() {
    }

    public static double crystallineArcaneShardDropChance() {
        return crystallineArcaneShardDropChance;
    }

    public static void setCrystallineArcaneShardDropChance(double value) {
        crystallineArcaneShardDropChance = Mth.clamp(value, 0.0D, 1.0D);
    }

    public static void reset() {
        crystallineArcaneShardDropChance =
                IronSwingcastStaffServerConfig.DEFAULT_CRYSTALLINE_ARCANE_SHARD_DROP_CHANCE;
    }
}
