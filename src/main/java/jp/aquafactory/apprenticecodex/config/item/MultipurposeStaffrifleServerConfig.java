package jp.aquafactory.apprenticecodex.config.item;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MultipurposeStaffrifleServerConfig {
    private final ModConfigSpec.DoubleValue adsMovementSpeedMultiplier;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private List<String> spellDenylistOverride;

    private MultipurposeStaffrifleServerConfig(
            ModConfigSpec.DoubleValue adsMovementSpeedMultiplier,
            ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist
    ) {
        this.adsMovementSpeedMultiplier = adsMovementSpeedMultiplier;
        this.spellDenylist = spellDenylist;
    }

    public static MultipurposeStaffrifleServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("MultipurposeStaffrifle");
        var adsMovementSpeedMultiplier = builder
                .comment("Movement speed multiplier while aiming Multipurpose Staffrifle. 0 disables movement; 1 applies no slowdown.")
                .defineInRange("adsMovementSpeedMultiplier", 0.7D, 0.0D, 1.0D);
        var spellDenylist = builder
                .comment("Additional spell IDs blocked only for Multipurpose Staffrifle special casts. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), MultipurposeStaffrifleServerConfig::isSpellId);
        builder.pop();

        return new MultipurposeStaffrifleServerConfig(
                adsMovementSpeedMultiplier,
                spellDenylist
        );
    }

    public boolean isSpellDenied(ResourceLocation spellId) {
        if (spellId == null) {
            return false;
        }
        for (var configuredId : spellDenylist()) {
            if (spellId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    public double adsMovementSpeedMultiplier() {
        return adsMovementSpeedMultiplier.get();
    }

    public List<String> spellDenylist() {
        return Objects.requireNonNullElseGet(spellDenylistOverride, () -> spellDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public void setSpellDenylistForGameTest(List<String> spellDenylist) {
        spellDenylistOverride = List.copyOf(spellDenylist);
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
