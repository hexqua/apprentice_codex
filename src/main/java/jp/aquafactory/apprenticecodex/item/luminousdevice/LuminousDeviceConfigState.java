package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig;

public final class LuminousDeviceConfigState {
    private static int maxStoredItems = LuminousDeviceServerConfig.DEFAULT_MAX_STORED_ITEMS;
    private static int maxStoredMana = LuminousDeviceServerConfig.DEFAULT_MAX_STORED_MANA;
    private static int cleanRadius = LuminousDeviceServerConfig.DEFAULT_CLEAN_RADIUS;
    private static double mageLightExtendedRange = LuminousDeviceServerConfig.DEFAULT_MAGE_LIGHT_EXTENDED_RANGE;

    private LuminousDeviceConfigState() {
    }

    public static int maxStoredItems() {
        return maxStoredItems;
    }

    public static int maxStoredMana() {
        return maxStoredMana;
    }

    public static int cleanRadius() {
        return cleanRadius;
    }

    public static int cleanSize() {
        return 1 + cleanRadius * 2;
    }

    public static double mageLightExtendedRange() {
        return mageLightExtendedRange;
    }

    public static void set(int maxStoredItems, int maxStoredMana) {
        set(maxStoredItems, maxStoredMana, cleanRadius);
    }

    public static void set(int maxStoredItems, int maxStoredMana, int cleanRadius) {
        set(maxStoredItems, maxStoredMana, cleanRadius, mageLightExtendedRange);
    }

    public static void set(int maxStoredItems, int maxStoredMana, int cleanRadius, double mageLightExtendedRange) {
        LuminousDeviceConfigState.maxStoredItems = Math.max(0, maxStoredItems);
        LuminousDeviceConfigState.maxStoredMana = Math.max(0, maxStoredMana);
        LuminousDeviceConfigState.cleanRadius = Math.max(
                0,
                Math.min(LuminousDeviceServerConfig.MAX_CLEAN_RADIUS, cleanRadius)
        );
        LuminousDeviceConfigState.mageLightExtendedRange = Math.max(
                0.0D,
                Math.min(LuminousDeviceServerConfig.MAX_MAGE_LIGHT_EXTENDED_RANGE, mageLightExtendedRange)
        );
    }

    public static void reset() {
        set(
                LuminousDeviceServerConfig.DEFAULT_MAX_STORED_ITEMS,
                LuminousDeviceServerConfig.DEFAULT_MAX_STORED_MANA,
                LuminousDeviceServerConfig.DEFAULT_CLEAN_RADIUS,
                LuminousDeviceServerConfig.DEFAULT_MAGE_LIGHT_EXTENDED_RANGE
        );
    }
}
