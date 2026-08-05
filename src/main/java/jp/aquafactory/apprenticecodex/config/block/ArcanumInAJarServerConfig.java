package jp.aquafactory.apprenticecodex.config.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class ArcanumInAJarServerConfig {
    public static final String DEFAULT_MATERIAL_ITEM_ID = "minecraft:redstone";
    public static final String DEFAULT_PRODUCT_ITEM_ID = "apprenticecodex:crystalline_arcane_shard";

    private final ModConfigSpec.IntValue ticksPerStoredParameter;
    private Integer ticksPerStoredParameterOverride;
    private final ModConfigSpec.ConfigValue<String> materialItemId;
    private final ModConfigSpec.ConfigValue<String> productItemId;
    private ItemSettings activeItemSettings = new ItemSettings(DEFAULT_MATERIAL_ITEM_ID, DEFAULT_PRODUCT_ITEM_ID);
    private ItemSettings itemSettingsOverride;
    private boolean invalidItemSettingsWarningLogged;

    private ArcanumInAJarServerConfig(
            ModConfigSpec.IntValue ticksPerStoredParameter,
            ModConfigSpec.ConfigValue<String> materialItemId,
            ModConfigSpec.ConfigValue<String> productItemId
    ) {
        this.ticksPerStoredParameter = ticksPerStoredParameter;
        this.materialItemId = materialItemId;
        this.productItemId = productItemId;
    }

    public static ArcanumInAJarServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment(
                        "20 ticks = 1 second. Default = 3 minutes.",
                        "Item ID changes are applied when the SERVER config is loaded, normally after a server restart.",
                        "Changes affect material and product counts already stored in every Arcanum in a Jar.",
                        "Empty all jars before changing item IDs. Invalid settings prevent interaction and stored contents cannot be recovered by breaking the jar."
                )
                .push("ArcanumInAJar");
        var ticksPerStoredParameter = builder.defineInRange("ticksPerStoredParameter", 3600, 1, Integer.MAX_VALUE);
        var materialItemId = builder
                .comment("The single item ID accepted as conversion material.")
                .define("materialItemId", DEFAULT_MATERIAL_ITEM_ID);
        var productItemId = builder
                .comment("The single item ID produced after conversion.")
                .define("productItemId", DEFAULT_PRODUCT_ITEM_ID);
        builder.pop();

        return new ArcanumInAJarServerConfig(ticksPerStoredParameter, materialItemId, productItemId);
    }

    public int ticksPerStoredParameter() {
        return ticksPerStoredParameterOverride != null
                ? ticksPerStoredParameterOverride
                : ticksPerStoredParameter.get();
    }

    public void setTicksPerStoredParameterForGameTest(@Nullable Integer value) {
        ticksPerStoredParameterOverride = value;
    }

    public ItemSettings itemSettings() {
        return itemSettingsOverride != null ? itemSettingsOverride : activeItemSettings;
    }

    public void captureItemSettingsOnConfigLoad() {
        activeItemSettings = new ItemSettings(materialItemId.get(), productItemId.get());
        invalidItemSettingsWarningLogged = false;
    }

    public boolean markInvalidItemSettingsWarningLogged() {
        if (invalidItemSettingsWarningLogged) {
            return false;
        }

        invalidItemSettingsWarningLogged = true;
        return true;
    }

    public void setItemSettingsForGameTest(@Nullable ItemSettings settings) {
        itemSettingsOverride = settings;
        invalidItemSettingsWarningLogged = false;
    }

    public record ItemSettings(String materialItemId, String productItemId) {
        public @Nullable Item materialItem() {
            return resolveItem(materialItemId);
        }

        public @Nullable Item productItem() {
            return resolveItem(productItemId);
        }

        public boolean isValid() {
            return materialItem() != null && productItem() != null;
        }

        private static @Nullable Item resolveItem(String configuredId) {
            if (configuredId == null || !configuredId.contains(":")) {
                return null;
            }

            var itemId = ResourceLocation.tryParse(configuredId);
            if (itemId == null) {
                return null;
            }

            var item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            return item == Items.AIR ? null : item;
        }
    }
}
