package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class IronSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.COMMON,
            14,
            5.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes()
    );

    public IronSwingcastStaff() {
        super("iron_swingcast_staff", TIER);
    }
}
