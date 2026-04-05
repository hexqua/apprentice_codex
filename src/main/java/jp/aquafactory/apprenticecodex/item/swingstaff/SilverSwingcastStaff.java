package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class SilverSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            18,
            3.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes()
    );

    public SilverSwingcastStaff() {
        super("silver_swingcast_staff", TIER);
    }
}
