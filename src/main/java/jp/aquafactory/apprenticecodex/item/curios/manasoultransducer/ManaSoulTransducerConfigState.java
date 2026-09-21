package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

public final class ManaSoulTransducerConfigState {
    private static double castRate = 0.8D;
    private static int manaCost = 80;
    private ManaSoulTransducerConfigState() {}
    public static double castRate() { return castRate; }
    public static int manaCost() { return manaCost; }
    public static void set(double cast, int cost) { castRate = cast; manaCost = cost; }
    public static void reset() { set(0.8D, 80); }
}
