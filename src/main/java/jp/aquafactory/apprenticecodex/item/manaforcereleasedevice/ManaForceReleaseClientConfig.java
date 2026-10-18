package jp.aquafactory.apprenticecodex.item.manaforcereleasedevice;

import jp.aquafactory.apprenticecodex.config.item.ManaForceReleaseDeviceServerConfig.Values;

public final class ManaForceReleaseClientConfig {
    private static Values values = Values.DEFAULT;

    private ManaForceReleaseClientConfig() {}

    public static Values values() { return values; }
    public static void set(Values updated) { values = updated; }
    public static void reset() { values = Values.DEFAULT; }
}
