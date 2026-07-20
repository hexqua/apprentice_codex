package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class ChargecastCatalystbookServerConfig {
    private final ForgeConfigSpec.IntValue castTimeTicks;
    private final ForgeConfigSpec.DoubleValue spellPowerMultiplier;
    private final ForgeConfigSpec.DoubleValue silverRingCastTimeBonusFactor;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private Values override;

    private ChargecastCatalystbookServerConfig(
            ForgeConfigSpec.IntValue castTimeTicks,
            ForgeConfigSpec.DoubleValue spellPowerMultiplier,
            ForgeConfigSpec.DoubleValue silverRingCastTimeBonusFactor,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist
    ) {
        this.castTimeTicks = castTimeTicks;
        this.spellPowerMultiplier = spellPowerMultiplier;
        this.silverRingCastTimeBonusFactor = silverRingCastTimeBonusFactor;
        this.spellDenylist = spellDenylist;
    }

    public static ChargecastCatalystbookServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ChargecastCatalystbook");
        var castTimeTicks = builder
                .comment("Base casting time added to instant spells. 20 ticks = 1 second.")
                .defineInRange("castTimeTicks", 30, 1, 72000);
        var spellPowerMultiplier = builder
                .comment("Final spell power multiplier applied when the charged cast completes.")
                .defineInRange("spellPowerMultiplier", 1.2D, 1.0D, 100.0D);
        var silverRingCastTimeBonusFactor = builder
                .comment("Extra final spell power gained from cast time reduction when a Silver Ring is installed.")
                .defineInRange("silverRingCastTimeBonusFactor", 0.2D, 0.0D, 100.0D);
        var spellDenylist = builder
                .comment("Spell IDs blocked for Chargecast Catalystbook casts. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), ChargecastCatalystbookServerConfig::isSpellId);
        builder.pop();
        return new ChargecastCatalystbookServerConfig(
                castTimeTicks,
                spellPowerMultiplier,
                silverRingCastTimeBonusFactor,
                spellDenylist
        );
    }

    public Values values() {
        return override != null
                ? override
                : new Values(
                        castTimeTicks.get(),
                        spellPowerMultiplier.get(),
                        silverRingCastTimeBonusFactor.get(),
                        spellDenylist.get().stream()
                                .map(String::valueOf)
                                .map(ResourceLocation::tryParse)
                                .filter(Objects::nonNull)
                                .toList()
                );
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }

    public record Values(
            int castTimeTicks,
            double spellPowerMultiplier,
            double silverRingCastTimeBonusFactor,
            List<ResourceLocation> spellDenylist
    ) {
        public static final Values DEFAULT = new Values(30, 1.2D, 0.2D, List.of());

        public Values {
            spellDenylist = List.copyOf(spellDenylist);
        }

        public boolean isSpellDenied(ResourceLocation spellId) {
            return spellId != null && spellDenylist.contains(spellId);
        }
    }
}
