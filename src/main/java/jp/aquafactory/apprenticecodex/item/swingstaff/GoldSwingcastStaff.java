package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.world.item.Rarity;

public class GoldSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            22,
            3.0D,
            createCommonSpellPowerBonuses(),
            allNonContinuousCastTypes(),
            SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME
    );

    public GoldSwingcastStaff() {
        super("gold_swingcast_staff", TIER);
    }
}
