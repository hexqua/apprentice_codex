package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SoulstainedSteelSwingcastStaffClientManaCost {
    private SoulstainedSteelSwingcastStaffClientManaCost() {
    }

    public static double resolveFullBurstManaCost() {
        var baseManaCost = SoulstainedSteelSwingcastStaffConfigState.manaCostPerBlade();
        if (baseManaCost <= 0.0D) {
            return 0.0D;
        }

        return baseManaCost * SoulstainedSteelSwingcastStaff.MAX_BLADE_COUNT;
    }

    public static long resolveDisplayedFullBurstManaCost() {
        return (long) Math.ceil(resolveFullBurstManaCost() - 1.0e-9D);
    }
}
