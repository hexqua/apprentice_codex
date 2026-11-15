package jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton;

public final class ExoskeletonConfigState {
    private static int lifterManaCost = 15;
    private static int striderManaCost = 20;

    private ExoskeletonConfigState() {}

    public static int lifterManaCost() { return lifterManaCost; }
    public static int striderManaCost() { return striderManaCost; }

    public static void set(int lifter, int strider) {
        lifterManaCost = Math.max(0, lifter);
        striderManaCost = Math.max(0, strider);
    }

    public static void reset() { set(15, 20); }
}
