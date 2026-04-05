package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class NetheriteSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.RARE,
            15,
            7.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes(),
            SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME
    );

    public NetheriteSwingcastStaff() {
        super("netherite_swingcast_staff", TIER);
    }
}
