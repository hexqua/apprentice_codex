package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MulticastEchoStaffMobEffectProfile(
        double durationExtendRate,
        int durationExtendFlat,
        int durationExtendLimit,
        double amplifierStackRate,
        int amplifierStackFlat,
        int amplifierStackLimit
) {
    public static final int DEFAULT_DURATION_EXTEND_LIMIT = 6000;
    public static final MulticastEchoStaffMobEffectProfile DEFAULT = new MulticastEchoStaffMobEffectProfile(
            0.0D,
            0,
            DEFAULT_DURATION_EXTEND_LIMIT,
            0.0D,
            0,
            0
    );
    public static final MulticastEchoStaffMobEffectProfile DEFAULT_DURATION_EXTENSION = new MulticastEchoStaffMobEffectProfile(
            0.5D,
            0,
            DEFAULT_DURATION_EXTEND_LIMIT,
            0.0D,
            0,
            0
    );

    public static final Codec<MulticastEchoStaffMobEffectProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("duration_extend_rate", DEFAULT.durationExtendRate())
                            .forGetter(MulticastEchoStaffMobEffectProfile::durationExtendRate),
                    Codec.INT.optionalFieldOf("duration_extend_flat", DEFAULT.durationExtendFlat())
                            .forGetter(MulticastEchoStaffMobEffectProfile::durationExtendFlat),
                    Codec.INT.optionalFieldOf("duration_extend_limit", DEFAULT.durationExtendLimit())
                            .forGetter(MulticastEchoStaffMobEffectProfile::durationExtendLimit),
                    Codec.DOUBLE.optionalFieldOf("amplifier_stack_rate", DEFAULT.amplifierStackRate())
                            .forGetter(MulticastEchoStaffMobEffectProfile::amplifierStackRate),
                    Codec.INT.optionalFieldOf("amplifier_stack_flat", DEFAULT.amplifierStackFlat())
                            .forGetter(MulticastEchoStaffMobEffectProfile::amplifierStackFlat),
                    Codec.INT.optionalFieldOf("amplifier_stack_limit", DEFAULT.amplifierStackLimit())
                            .forGetter(MulticastEchoStaffMobEffectProfile::amplifierStackLimit)
            ).apply(instance, MulticastEchoStaffMobEffectProfile::new)
    );
}
