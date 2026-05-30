package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class SatelliteFollowcastAmuletServerConfig {
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist;

    private List<String> spellDenylistOverride;

    private SatelliteFollowcastAmuletServerConfig(ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist) {
        this.spellDenylist = spellDenylist;
    }

    public static SatelliteFollowcastAmuletServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("SatelliteFollowcastAmulet");
        var spellDenylist = builder
                .comment("Spell IDs blocked only for Satellite Followcast Amulet imbue and followcast. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), SatelliteFollowcastAmuletServerConfig::isSpellId);
        builder.pop();

        return new SatelliteFollowcastAmuletServerConfig(spellDenylist);
    }

    public boolean isSpellDenied(ResourceLocation spellId) {
        if (spellId == null) {
            return false;
        }
        for (var configuredId : spellDenylist()) {
            if (spellId.equals(ResourceLocation.tryParse(configuredId))) {
                return true;
            }
        }
        return false;
    }

    public List<String> spellDenylist() {
        return Objects.requireNonNullElseGet(spellDenylistOverride, () -> spellDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public void setSpellDenylistForGameTest(List<String> spellDenylist) {
        this.spellDenylistOverride = List.copyOf(spellDenylist);
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
