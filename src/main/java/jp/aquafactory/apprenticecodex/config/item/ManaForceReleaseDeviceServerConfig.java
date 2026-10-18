package jp.aquafactory.apprenticecodex.config.item;

import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseTier;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaForceReleaseDeviceServerConfig {
    public record Values(double noviceMultiplier, double adeptMultiplier, double masterMultiplier,
                         double noviceSoftCap, double adeptSoftCap) {
        public static final Values DEFAULT = new Values(1, 1, 1, 300, 500);

        public double multiplier(ManaForceReleaseTier tier) {
            return switch (tier) {
                case NOVICE -> noviceMultiplier;
                case ADEPT -> adeptMultiplier;
                case MASTER -> masterMultiplier;
            };
        }

        public double softCap(ManaForceReleaseTier tier) {
            return tier == ManaForceReleaseTier.NOVICE ? noviceSoftCap : adeptSoftCap;
        }
    }

    private final ModConfigSpec.DoubleValue noviceMultiplier;
    private final ModConfigSpec.DoubleValue adeptMultiplier;
    private final ModConfigSpec.DoubleValue masterMultiplier;
    private final ModConfigSpec.DoubleValue noviceSoftCap;
    private final ModConfigSpec.DoubleValue adeptSoftCap;

    private ManaForceReleaseDeviceServerConfig(ModConfigSpec.Builder builder) {
        builder.push("ManaForceReleaseDevice");
        noviceMultiplier = builder.comment("Novice special attack damage multiplier.")
                .defineInRange("noviceDamageMultiplier", 1.0, 0.0, 10000.0);
        adeptMultiplier = builder.comment("Adept special attack damage multiplier.")
                .defineInRange("adeptDamageMultiplier", 1.0, 0.0, 10000.0);
        masterMultiplier = builder.comment("Master special attack damage multiplier.")
                .defineInRange("masterDamageMultiplier", 1.0, 0.0, 10000.0);
        noviceSoftCap = builder.comment("Mana above this amount has reduced conversion efficiency for Novice.")
                .defineInRange("noviceManaSoftCap", 300.0, 1.0, 1000000.0);
        adeptSoftCap = builder.comment("Mana above this amount has reduced conversion efficiency for Adept.")
                .defineInRange("adeptManaSoftCap", 500.0, 1.0, 1000000.0);
        builder.pop();
    }

    public static ManaForceReleaseDeviceServerConfig define(ModConfigSpec.Builder builder) {
        return new ManaForceReleaseDeviceServerConfig(builder);
    }

    public Values values() {
        return new Values(noviceMultiplier.get(), adeptMultiplier.get(), masterMultiplier.get(),
                noviceSoftCap.get(), adeptSoftCap.get());
    }
}
