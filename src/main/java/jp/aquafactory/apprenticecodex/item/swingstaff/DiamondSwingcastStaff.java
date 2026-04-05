package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class DiamondSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            10,
            6.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes()
    );

    public DiamondSwingcastStaff() {
        super("diamond_swingcast_staff", TIER);
    }
}
