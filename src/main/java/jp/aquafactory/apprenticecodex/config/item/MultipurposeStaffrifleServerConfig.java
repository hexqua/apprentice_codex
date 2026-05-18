package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class MultipurposeStaffrifleServerConfig {
    private final ModConfigSpec.IntValue cooldownBypassThresholdTicks;
    private final ModConfigSpec.IntValue cooldownReductionTicks;
    private final ModConfigSpec.IntValue reducedCooldownMinimumTicks;
    private final ModConfigSpec.IntValue adsFullAutoIntervalTicks;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private List<String> spellDenylistOverride;

    private MultipurposeStaffrifleServerConfig(
            ModConfigSpec.IntValue cooldownBypassThresholdTicks,
            ModConfigSpec.IntValue cooldownReductionTicks,
            ModConfigSpec.IntValue reducedCooldownMinimumTicks,
            ModConfigSpec.IntValue adsFullAutoIntervalTicks,
            ModConfigSpec.ConfigValue<List<? extends String>> spellDenylist
    ) {
        this.cooldownBypassThresholdTicks = cooldownBypassThresholdTicks;
        this.cooldownReductionTicks = cooldownReductionTicks;
        this.reducedCooldownMinimumTicks = reducedCooldownMinimumTicks;
        this.adsFullAutoIntervalTicks = adsFullAutoIntervalTicks;
        this.spellDenylist = spellDenylist;
    }

    public static MultipurposeStaffrifleServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("MultipurposeStaffrifle");
        var cooldownBypassThresholdTicks = builder
                .comment("Cooldowns at or below this value are removed for Multipurpose Staffrifle special casts. 200 ticks = 10 seconds.")
                .defineInRange("cooldownBypassThresholdTicks", 20 * 10, 0, 72000);
        var cooldownReductionTicks = builder
                .comment("Cooldown ticks subtracted from longer Multipurpose Staffrifle special casts. 600 ticks = 30 seconds.")
                .defineInRange("cooldownReductionTicks", 20 * 30, 0, 72000);
        var reducedCooldownMinimumTicks = builder
                .comment("Minimum cooldown after Multipurpose Staffrifle special cast reduction. 200 ticks = 10 seconds.")
                .defineInRange("reducedCooldownMinimumTicks", 20 * 10, 0, 72000);
        var adsFullAutoIntervalTicks = builder
                .comment("Minimum server-side interval between ADS full-auto special cast attempts.")
                .defineInRange("adsFullAutoIntervalTicks", 3, 1, 72000);
        var spellDenylist = builder
                .comment("Additional spell IDs blocked only for Multipurpose Staffrifle special casts. Entries use \"modid:path\".")
                .defineList("spellDenylist", List.<String>of(), MultipurposeStaffrifleServerConfig::isSpellId);
        builder.pop();

        return new MultipurposeStaffrifleServerConfig(
                cooldownBypassThresholdTicks,
                cooldownReductionTicks,
                reducedCooldownMinimumTicks,
                adsFullAutoIntervalTicks,
                spellDenylist
        );
    }

    public int cooldownBypassThresholdTicks() {
        return cooldownBypassThresholdTicks.get();
    }

    public int cooldownReductionTicks() {
        return cooldownReductionTicks.get();
    }

    public int reducedCooldownMinimumTicks() {
        return reducedCooldownMinimumTicks.get();
    }

    public int adsFullAutoIntervalTicks() {
        return adsFullAutoIntervalTicks.get();
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
