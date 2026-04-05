package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class CopperSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.COMMON,
            14,
            3.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes()
    );

    public CopperSwingcastStaff() {
        super("copper_swingcast_staff", TIER);
    }
}
