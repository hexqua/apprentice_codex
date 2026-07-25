package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum LuminousDeviceUpgrade {
    CLEAN("clean"),
    ENHANCED_MAGE_LIGHT("enhanced_mage_light"),
    MANA_WIZARDLAMP("mana_wizardlamp");

    private final ResourceLocation id;
    private final String translationKey;

    LuminousDeviceUpgrade(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
        this.translationKey = "item.apprenticecodex.luminous_device.upgrade." + path;
    }

    public ResourceLocation id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public static @Nullable LuminousDeviceUpgrade byId(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(upgrade -> upgrade.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
