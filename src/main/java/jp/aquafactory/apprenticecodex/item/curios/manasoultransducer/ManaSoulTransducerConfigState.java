package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

public final class ManaSoulTransducerConfigState {
    private static double castRate = 0.8D;
    private static double recoveryRate = 0.8D;

    private ManaSoulTransducerConfigState() {}

    public static double castRate() { return castRate; }
    public static double recoveryRate() { return recoveryRate; }

    public static void set(double cast, double recovery) {
        castRate = cast;
        recoveryRate = recovery;
    }

    public static void reset() { set(0.8D, 0.8D); }
}
