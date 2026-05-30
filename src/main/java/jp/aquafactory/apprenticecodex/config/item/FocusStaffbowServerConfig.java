package jp.aquafactory.apprenticecodex.config.item;

import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class FocusStaffbowServerConfig {
    private final ForgeConfigSpec.BooleanValue enableContinuousFocusedCast;
    private final ForgeConfigSpec.BooleanValue enableManaLoan;
    private final ForgeConfigSpec.BooleanValue enableArrowCatalystRequirement;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> arrowCatalystItems;
    private final ForgeConfigSpec.DoubleValue pendingMaxChargeMultiplier;
    private final ForgeConfigSpec.DoubleValue continuousMaxChargeMultiplier;
    private final ForgeConfigSpec.IntValue minimumOverchargeBaselineTicks;
    private final ForgeConfigSpec.DoubleValue chargeManaCostExponent;
    private final ForgeConfigSpec.DoubleValue chargeManaCostMultiplier;
    private final ForgeConfigSpec.DoubleValue pendingMaxLoanManaRatio;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private final ForgeConfigSpec.BooleanValue enableSpellAllowlist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellAllowlist;

    private Boolean enableContinuousFocusedCastOverride;
    private Boolean enableManaLoanOverride;
    private Boolean enableArrowCatalystRequirementOverride;
    private List<String> arrowCatalystItemsOverride;
    private Double pendingMaxChargeMultiplierOverride;
    private Double continuousMaxChargeMultiplierOverride;
    private Integer minimumOverchargeBaselineTicksOverride;
    private Double chargeManaCostExponentOverride;
    private Double chargeManaCostMultiplierOverride;
    private Double pendingMaxLoanManaRatioOverride;
    private List<String> spellDenylistOverride;
    private Boolean enableSpellAllowlistOverride;
    private List<String> spellAllowlistOverride;

    private FocusStaffbowServerConfig(
            ForgeConfigSpec.BooleanValue enableContinuousFocusedCast,
            ForgeConfigSpec.BooleanValue enableManaLoan,
            ForgeConfigSpec.BooleanValue enableArrowCatalystRequirement,
            ForgeConfigSpec.ConfigValue<List<? extends String>> arrowCatalystItems,
            ForgeConfigSpec.DoubleValue pendingMaxChargeMultiplier,
            ForgeConfigSpec.DoubleValue continuousMaxChargeMultiplier,
            ForgeConfigSpec.IntValue minimumOverchargeBaselineTicks,
            ForgeConfigSpec.DoubleValue chargeManaCostExponent,
            ForgeConfigSpec.DoubleValue chargeManaCostMultiplier,
            ForgeConfigSpec.DoubleValue pendingMaxLoanManaRatio,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist,
            ForgeConfigSpec.BooleanValue enableSpellAllowlist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellAllowlist
    ) {
        this.enableContinuousFocusedCast = enableContinuousFocusedCast;
        this.enableManaLoan = enableManaLoan;
        this.enableArrowCatalystRequirement = enableArrowCatalystRequirement;
        this.arrowCatalystItems = arrowCatalystItems;
        this.pendingMaxChargeMultiplier = pendingMaxChargeMultiplier;
        this.continuousMaxChargeMultiplier = continuousMaxChargeMultiplier;
        this.minimumOverchargeBaselineTicks = minimumOverchargeBaselineTicks;
        this.chargeManaCostExponent = chargeManaCostExponent;
        this.chargeManaCostMultiplier = chargeManaCostMultiplier;
        this.pendingMaxLoanManaRatio = pendingMaxLoanManaRatio;
        this.spellDenylist = spellDenylist;
        this.enableSpellAllowlist = enableSpellAllowlist;
        this.spellAllowlist = spellAllowlist;
    }

    public static FocusStaffbowServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("FocusStaffbow");
        var enableContinuousFocusedCast = builder
                .comment("Enables Focus Staffbow custom continuous cast handling. When false, CONTINUOUS spells fail before consuming arrows or mana.")
                .define("enableContinuousFocusedCast", true);
        var enableManaLoan = builder
                .comment("Allows pending Focus Staffbow casts to borrow missing mana after the base mana start check succeeds.")
                .define("enableManaLoan", true);
        var enableArrowCatalystRequirement = builder
                .comment("Requires an arrow catalyst for Focus Staffbow casts. When false, no arrow is searched or consumed.")
                .define("enableArrowCatalystRequirement", true);
        var arrowCatalystItems = builder
                .comment("Item IDs accepted as Focus Staffbow arrow catalysts. Empty list makes non-Synthesis survival casts unusable while arrow catalysts are required.")
                .defineListAllowEmpty("arrowCatalystItems", List.of("minecraft:arrow"), FocusStaffbowServerConfig::isItemId);
        var pendingMaxChargeMultiplier = builder
                .comment("Maximum spell power multiplier for pending Focus Staffbow casts.")
                .defineInRange("pendingMaxChargeMultiplier", 3.0D, 1.0D, 100.0D);
        var continuousMaxChargeMultiplier = builder
                .comment("Maximum spell power multiplier for continuous Focus Staffbow casts.")
                .defineInRange("continuousMaxChargeMultiplier", 2.0D, 1.0D, 100.0D);
        var minimumOverchargeBaselineTicks = builder
                .comment("Minimum baseline ticks used before pending Focus Staffbow overcharge starts. 20 ticks = 1 second.")
                .defineInRange("minimumOverchargeBaselineTicks", 20, 0, 72000);
        var chargeManaCostExponent = builder
                .comment("Exponent applied to charge multiplier for Focus Staffbow scaled mana cost.")
                .defineInRange("chargeManaCostExponent", 2.0D, 0.0D, 10.0D);
        var chargeManaCostMultiplier = builder
                .comment("Final multiplier applied to Focus Staffbow scaled mana cost.")
                .defineInRange("chargeManaCostMultiplier", 1.0D, 0.0D, 100.0D);
        var pendingMaxLoanManaRatio = builder
                .comment("Maximum pending Focus Staffbow borrowed mana as a ratio of the caster's max mana. 0 disables borrowing.")
                .defineInRange("pendingMaxLoanManaRatio", 1.0D, 0.0D, 100.0D);
        var spellDenylist = builder
                .comment("Spell IDs blocked for Focus Staffbow casts. Entries use \"modid:path\" and are checked before ammo or mana is consumed.")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), FocusStaffbowServerConfig::isSpellId);
        var enableSpellAllowlist = builder
                .comment("Enables the Focus Staffbow spell allowlist. The denylist still has priority.")
                .define("enableSpellAllowlist", false);
        var spellAllowlist = builder
                .comment("Spell IDs allowed when enableSpellAllowlist is true. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellAllowlist", List.<String>of(), FocusStaffbowServerConfig::isSpellId);
        builder.pop();

        return new FocusStaffbowServerConfig(
                enableContinuousFocusedCast,
                enableManaLoan,
                enableArrowCatalystRequirement,
                arrowCatalystItems,
                pendingMaxChargeMultiplier,
                continuousMaxChargeMultiplier,
                minimumOverchargeBaselineTicks,
                chargeManaCostExponent,
                chargeManaCostMultiplier,
                pendingMaxLoanManaRatio,
                spellDenylist,
                enableSpellAllowlist,
                spellAllowlist
        );
    }

    public boolean enableContinuousFocusedCast() {
        return enableContinuousFocusedCastOverride == null ? enableContinuousFocusedCast.get() : enableContinuousFocusedCastOverride;
    }

    public boolean enableManaLoan() {
        return enableManaLoanOverride == null ? enableManaLoan.get() : enableManaLoanOverride;
    }

    public boolean enableArrowCatalystRequirement() {
        return enableArrowCatalystRequirementOverride == null
                ? enableArrowCatalystRequirement.get()
                : enableArrowCatalystRequirementOverride;
    }

    public List<String> arrowCatalystItems() {
        return Objects.requireNonNullElseGet(arrowCatalystItemsOverride, () -> arrowCatalystItems.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public List<ResourceLocation> arrowCatalystItemIds() {
        return arrowCatalystItems().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .toList();
    }

    public double pendingMaxChargeMultiplier() {
        return pendingMaxChargeMultiplierOverride == null ? pendingMaxChargeMultiplier.get() : pendingMaxChargeMultiplierOverride;
    }

    public double continuousMaxChargeMultiplier() {
        return continuousMaxChargeMultiplierOverride == null ? continuousMaxChargeMultiplier.get() : continuousMaxChargeMultiplierOverride;
    }

    public int minimumOverchargeBaselineTicks() {
        return minimumOverchargeBaselineTicksOverride == null
                ? minimumOverchargeBaselineTicks.get()
                : minimumOverchargeBaselineTicksOverride;
    }

    public double chargeManaCostExponent() {
        return chargeManaCostExponentOverride == null ? chargeManaCostExponent.get() : chargeManaCostExponentOverride;
    }

    public double chargeManaCostMultiplier() {
        return chargeManaCostMultiplierOverride == null ? chargeManaCostMultiplier.get() : chargeManaCostMultiplierOverride;
    }

    public double pendingMaxLoanManaRatio() {
        return pendingMaxLoanManaRatioOverride == null ? pendingMaxLoanManaRatio.get() : pendingMaxLoanManaRatioOverride;
    }

    public List<String> spellDenylist() {
        return Objects.requireNonNullElseGet(spellDenylistOverride, () -> spellDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public boolean enableSpellAllowlist() {
        return enableSpellAllowlistOverride == null ? enableSpellAllowlist.get() : enableSpellAllowlistOverride;
    }

    public List<String> spellAllowlist() {
        return Objects.requireNonNullElseGet(spellAllowlistOverride, () -> spellAllowlist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public boolean isSpellDenied(ResourceLocation spellId) {
        return spellId != null && containsSpellId(spellDenylist(), spellId);
    }

    public boolean isSpellAllowed(ResourceLocation spellId) {
        return !enableSpellAllowlist() || spellId != null && containsSpellId(spellAllowlist(), spellId);
    }

    public FocusStaffbowChargeSettings chargeSettings() {
        return new FocusStaffbowChargeSettings(
                pendingMaxChargeMultiplier(),
                continuousMaxChargeMultiplier(),
                minimumOverchargeBaselineTicks(),
                chargeManaCostExponent(),
                chargeManaCostMultiplier()
        );
    }

    public void setForGameTest(
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            List<String> arrowCatalystItems,
            double pendingMaxChargeMultiplier,
            double continuousMaxChargeMultiplier,
            int minimumOverchargeBaselineTicks,
            double chargeManaCostExponent,
            double chargeManaCostMultiplier,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        this.enableContinuousFocusedCastOverride = enableContinuousFocusedCast;
        this.enableManaLoanOverride = enableManaLoan;
        this.enableArrowCatalystRequirementOverride = enableArrowCatalystRequirement;
        this.arrowCatalystItemsOverride = List.copyOf(arrowCatalystItems);
        this.pendingMaxChargeMultiplierOverride = pendingMaxChargeMultiplier;
        this.continuousMaxChargeMultiplierOverride = continuousMaxChargeMultiplier;
        this.minimumOverchargeBaselineTicksOverride = minimumOverchargeBaselineTicks;
        this.chargeManaCostExponentOverride = chargeManaCostExponent;
        this.chargeManaCostMultiplierOverride = chargeManaCostMultiplier;
        this.pendingMaxLoanManaRatioOverride = pendingMaxLoanManaRatio;
        this.spellDenylistOverride = List.copyOf(spellDenylist);
        this.enableSpellAllowlistOverride = enableSpellAllowlist;
        this.spellAllowlistOverride = List.copyOf(spellAllowlist);
    }

    private static boolean containsSpellId(List<String> configuredIds, ResourceLocation spellId) {
        for (var configuredId : configuredIds) {
            if (spellId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }

    private static boolean isItemId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
