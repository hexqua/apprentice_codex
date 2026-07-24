package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LuminousDeviceServerConfig {
    public static final int DEFAULT_MAX_STORED_ITEMS = 1024;
    public static final int DEFAULT_MAX_STORED_MANA = 2000;

    private final ForgeConfigSpec.IntValue maxStoredItems;
    private final ForgeConfigSpec.IntValue maxStoredMana;
    private Integer maxStoredItemsOverride;
    private Integer maxStoredManaOverride;

    private LuminousDeviceServerConfig(
            ForgeConfigSpec.IntValue maxStoredItems,
            ForgeConfigSpec.IntValue maxStoredMana
    ) {
        this.maxStoredItems = maxStoredItems;
        this.maxStoredMana = maxStoredMana;
    }

    public static LuminousDeviceServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("LuminousDevice");
        var maxStoredItems = builder
                .comment("Maximum total item count stored by a Luminous Device.")
                .defineInRange("maxStoredItems", DEFAULT_MAX_STORED_ITEMS, 0, Integer.MAX_VALUE);
        var maxStoredMana = builder
                .comment("Maximum mana stored by a Luminous Device.")
                .defineInRange("maxStoredMana", DEFAULT_MAX_STORED_MANA, 0, Integer.MAX_VALUE);
        builder.pop();
        return new LuminousDeviceServerConfig(maxStoredItems, maxStoredMana);
    }

    public int maxStoredItems() {
        return maxStoredItemsOverride == null ? maxStoredItems.get() : maxStoredItemsOverride;
    }

    public int maxStoredMana() {
        return maxStoredManaOverride == null ? maxStoredMana.get() : maxStoredManaOverride;
    }

    public void setForGameTest(int maxStoredItems, int maxStoredMana) {
        maxStoredItemsOverride = maxStoredItems;
        maxStoredManaOverride = maxStoredMana;
    }
}
