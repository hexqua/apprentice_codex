package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LuminousDeviceServerConfig {
    public static final int DEFAULT_MAX_STORED_ITEMS = 1024;
    public static final int DEFAULT_MAX_STORED_MANA = 2000;
    public static final int DEFAULT_CLEAN_RADIUS = 4;
    public static final int MAX_CLEAN_RADIUS = 16;
    public static final int DEFAULT_MAGE_LIGHT_MANA_RECOVERY = 20;
    public static final int DEFAULT_WIZARDLAMP_MANA_RECOVERY = 50;

    private final ForgeConfigSpec.IntValue maxStoredItems;
    private final ForgeConfigSpec.IntValue maxStoredMana;
    private final ForgeConfigSpec.IntValue cleanRadius;
    private final ForgeConfigSpec.IntValue mageLightManaRecovery;
    private final ForgeConfigSpec.IntValue wizardlampManaRecovery;
    private Integer maxStoredItemsOverride;
    private Integer maxStoredManaOverride;
    private Integer cleanRadiusOverride;
    private Integer mageLightManaRecoveryOverride;
    private Integer wizardlampManaRecoveryOverride;

    private LuminousDeviceServerConfig(
            ForgeConfigSpec.IntValue maxStoredItems,
            ForgeConfigSpec.IntValue maxStoredMana,
            ForgeConfigSpec.IntValue cleanRadius,
            ForgeConfigSpec.IntValue mageLightManaRecovery,
            ForgeConfigSpec.IntValue wizardlampManaRecovery
    ) {
        this.maxStoredItems = maxStoredItems;
        this.maxStoredMana = maxStoredMana;
        this.cleanRadius = cleanRadius;
        this.mageLightManaRecovery = mageLightManaRecovery;
        this.wizardlampManaRecovery = wizardlampManaRecovery;
    }

    public static LuminousDeviceServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("LuminousDevice");
        var maxStoredItems = builder
                .comment("Maximum total item count stored by a Luminous Device.")
                .defineInRange("maxStoredItems", DEFAULT_MAX_STORED_ITEMS, 0, Integer.MAX_VALUE);
        var maxStoredMana = builder
                .comment("Maximum mana stored by a Luminous Device.")
                .defineInRange("maxStoredMana", DEFAULT_MAX_STORED_MANA, 0, Integer.MAX_VALUE);
        var cleanRadius = builder
                .comment("Cleanup radius of a Luminous Device. The cube side length is 1 + radius * 2.")
                .defineInRange("cleanRadius", DEFAULT_CLEAN_RADIUS, 0, MAX_CLEAN_RADIUS);
        var mageLightManaRecovery = builder
                .comment("Mana recovered for each Mage Light removed by a Luminous Device.")
                .defineInRange(
                        "mageLightManaRecovery",
                        DEFAULT_MAGE_LIGHT_MANA_RECOVERY,
                        0,
                        Integer.MAX_VALUE
                );
        var wizardlampManaRecovery = builder
                .comment("Mana recovered for each Wizardlamp removed by a Luminous Device.")
                .defineInRange(
                        "wizardlampManaRecovery",
                        DEFAULT_WIZARDLAMP_MANA_RECOVERY,
                        0,
                        Integer.MAX_VALUE
                );
        builder.pop();
        return new LuminousDeviceServerConfig(
                maxStoredItems,
                maxStoredMana,
                cleanRadius,
                mageLightManaRecovery,
                wizardlampManaRecovery
        );
    }

    public int maxStoredItems() {
        return maxStoredItemsOverride == null ? maxStoredItems.get() : maxStoredItemsOverride;
    }

    public int maxStoredMana() {
        return maxStoredManaOverride == null ? maxStoredMana.get() : maxStoredManaOverride;
    }

    public int cleanRadius() {
        return cleanRadiusOverride == null ? cleanRadius.get() : cleanRadiusOverride;
    }

    public int mageLightManaRecovery() {
        return mageLightManaRecoveryOverride == null
                ? mageLightManaRecovery.get()
                : mageLightManaRecoveryOverride;
    }

    public int wizardlampManaRecovery() {
        return wizardlampManaRecoveryOverride == null
                ? wizardlampManaRecovery.get()
                : wizardlampManaRecoveryOverride;
    }

    public void setForGameTest(int maxStoredItems, int maxStoredMana) {
        maxStoredItemsOverride = maxStoredItems;
        maxStoredManaOverride = maxStoredMana;
    }

    public void setCleanForGameTest(int cleanRadius, int mageLightManaRecovery, int wizardlampManaRecovery) {
        cleanRadiusOverride = cleanRadius;
        this.mageLightManaRecoveryOverride = mageLightManaRecovery;
        this.wizardlampManaRecoveryOverride = wizardlampManaRecovery;
    }
}
