package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LuminousDeviceServerConfig {
    public static final int DEFAULT_MAX_STORED_ITEMS = 1024;
    public static final int DEFAULT_MAX_STORED_MANA = 2000;
    public static final int DEFAULT_UPGRADED_MAX_STORED_MANA = 5000;
    public static final int DEFAULT_CLEAN_RADIUS = 4;
    public static final int MAX_CLEAN_RADIUS = 16;
    public static final int DEFAULT_MAGE_LIGHT_MANA_RECOVERY = 20;
    public static final int DEFAULT_WIZARDLAMP_MANA_RECOVERY = 50;
    public static final double DEFAULT_MAGE_LIGHT_EXTENDED_RANGE = 32.0D;
    public static final double MAX_MAGE_LIGHT_EXTENDED_RANGE = 64.0D;

    private final ModConfigSpec.IntValue maxStoredItems;
    private final ModConfigSpec.IntValue maxStoredMana;
    private final ModConfigSpec.IntValue upgradedMaxStoredMana;
    private final ModConfigSpec.IntValue cleanRadius;
    private final ModConfigSpec.IntValue mageLightManaRecovery;
    private final ModConfigSpec.IntValue wizardlampManaRecovery;
    private final ModConfigSpec.DoubleValue mageLightExtendedRange;
    private Integer maxStoredItemsOverride;
    private Integer maxStoredManaOverride;
    private Integer upgradedMaxStoredManaOverride;
    private Integer cleanRadiusOverride;
    private Integer mageLightManaRecoveryOverride;
    private Integer wizardlampManaRecoveryOverride;
    private Double mageLightExtendedRangeOverride;

    private LuminousDeviceServerConfig(
            ModConfigSpec.IntValue maxStoredItems,
            ModConfigSpec.IntValue maxStoredMana,
            ModConfigSpec.IntValue upgradedMaxStoredMana,
            ModConfigSpec.IntValue cleanRadius,
            ModConfigSpec.IntValue mageLightManaRecovery,
            ModConfigSpec.IntValue wizardlampManaRecovery,
            ModConfigSpec.DoubleValue mageLightExtendedRange
    ) {
        this.maxStoredItems = maxStoredItems;
        this.maxStoredMana = maxStoredMana;
        this.upgradedMaxStoredMana = upgradedMaxStoredMana;
        this.cleanRadius = cleanRadius;
        this.mageLightManaRecovery = mageLightManaRecovery;
        this.wizardlampManaRecovery = wizardlampManaRecovery;
        this.mageLightExtendedRange = mageLightExtendedRange;
    }

    public static LuminousDeviceServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("LuminousDevice");
        var maxStoredItems = builder
                .comment("Maximum total item count stored by a Luminous Device.")
                .defineInRange("maxStoredItems", DEFAULT_MAX_STORED_ITEMS, 0, Integer.MAX_VALUE);
        var maxStoredMana = builder
                .comment("Maximum mana stored by a Luminous Device.")
                .defineInRange("maxStoredMana", DEFAULT_MAX_STORED_MANA, 0, Integer.MAX_VALUE);
        var upgradedMaxStoredMana = builder
                .comment("Maximum mana stored after unlocking the Luminous Device mana and Wizardlamp upgrade.")
                .defineInRange(
                        "upgradedMaxStoredMana",
                        DEFAULT_UPGRADED_MAX_STORED_MANA,
                        0,
                        Integer.MAX_VALUE
                );
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
        var mageLightExtendedRange = builder
                .comment("Extended Mage Light placement range used by a Luminous Device. Values at or below the spell range disable the extension.")
                .defineInRange(
                        "mageLightExtendedRange",
                        DEFAULT_MAGE_LIGHT_EXTENDED_RANGE,
                        0.0D,
                        MAX_MAGE_LIGHT_EXTENDED_RANGE
                );
        builder.pop();
        return new LuminousDeviceServerConfig(
                maxStoredItems,
                maxStoredMana,
                upgradedMaxStoredMana,
                cleanRadius,
                mageLightManaRecovery,
                wizardlampManaRecovery,
                mageLightExtendedRange
        );
    }

    public int maxStoredItems() {
        return maxStoredItemsOverride == null ? maxStoredItems.get() : maxStoredItemsOverride;
    }

    public int maxStoredMana() {
        return maxStoredManaOverride == null ? maxStoredMana.get() : maxStoredManaOverride;
    }

    public int upgradedMaxStoredMana() {
        return upgradedMaxStoredManaOverride == null
                ? upgradedMaxStoredMana.get()
                : upgradedMaxStoredManaOverride;
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

    public double mageLightExtendedRange() {
        return mageLightExtendedRangeOverride == null
                ? mageLightExtendedRange.get()
                : mageLightExtendedRangeOverride;
    }

    public void setForGameTest(int maxStoredItems, int maxStoredMana) {
        maxStoredItemsOverride = maxStoredItems;
        maxStoredManaOverride = maxStoredMana;
    }

    public void setUpgradedMaxStoredManaForGameTest(int upgradedMaxStoredMana) {
        upgradedMaxStoredManaOverride = upgradedMaxStoredMana;
    }

    public void setCleanForGameTest(int cleanRadius, int mageLightManaRecovery, int wizardlampManaRecovery) {
        cleanRadiusOverride = cleanRadius;
        this.mageLightManaRecoveryOverride = mageLightManaRecovery;
        this.wizardlampManaRecoveryOverride = wizardlampManaRecovery;
    }

    public void setMageLightExtendedRangeForGameTest(double range) {
        mageLightExtendedRangeOverride = range;
    }
}
