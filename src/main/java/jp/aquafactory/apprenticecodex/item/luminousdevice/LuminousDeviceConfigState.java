package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig;

public final class LuminousDeviceConfigState {
    private static int maxStoredItems = LuminousDeviceServerConfig.DEFAULT_MAX_STORED_ITEMS;
    private static int maxStoredMana = LuminousDeviceServerConfig.DEFAULT_MAX_STORED_MANA;

    private LuminousDeviceConfigState() {
    }

    public static int maxStoredItems() {
        return maxStoredItems;
    }

    public static int maxStoredMana() {
        return maxStoredMana;
    }

    public static void set(int maxStoredItems, int maxStoredMana) {
        LuminousDeviceConfigState.maxStoredItems = Math.max(0, maxStoredItems);
        LuminousDeviceConfigState.maxStoredMana = Math.max(0, maxStoredMana);
    }

    public static void reset() {
        set(
                LuminousDeviceServerConfig.DEFAULT_MAX_STORED_ITEMS,
                LuminousDeviceServerConfig.DEFAULT_MAX_STORED_MANA
        );
    }
}
