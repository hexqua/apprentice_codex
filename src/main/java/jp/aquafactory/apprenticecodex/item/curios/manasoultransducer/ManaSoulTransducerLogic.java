package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import io.redspace.ironsspellbooks.api.util.Utils;

public final class ManaSoulTransducerLogic {
    private ManaSoulTransducerLogic() {}

    public static double durationModifier(double attribute, double transferRate) {
        return -transferRate * Math.max(0D, Utils.softCapFormula(Math.max(1D, attribute)) - 1D);
    }

    public static double recoveryModifier(double attribute, double transferRate) {
        return transferRate * Math.max(0D, attribute - 1D);
    }
}
