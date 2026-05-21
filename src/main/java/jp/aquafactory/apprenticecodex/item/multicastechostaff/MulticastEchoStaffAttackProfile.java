package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MulticastEchoStaffAttackProfile(
        double repeatDamageMultiplier,
        boolean ignoreIframe,
        boolean projectileTracking,
        boolean directDamageTracking,
        int trackingLifetimeTicks,
        int postHitIframeTicks
) {
    public static final MulticastEchoStaffAttackProfile DEFAULT = new MulticastEchoStaffAttackProfile(
            0.5D,
            true,
            true,
            true,
            100,
            1
    );
    public static final MulticastEchoStaffAttackProfile WITHOUT_PROJECTILE_TRACKING = new MulticastEchoStaffAttackProfile(
            DEFAULT.repeatDamageMultiplier(),
            DEFAULT.ignoreIframe(),
            false,
            DEFAULT.directDamageTracking(),
            DEFAULT.trackingLifetimeTicks(),
            DEFAULT.postHitIframeTicks()
    );

    public static MulticastEchoStaffAttackProfile GenerateDefaultWithLifeTime(int lifeTime) {
        return new MulticastEchoStaffAttackProfile(
                DEFAULT.repeatDamageMultiplier(),
                DEFAULT.ignoreIframe(),
                DEFAULT.projectileTracking(),
                DEFAULT.directDamageTracking(),
                lifeTime,
                DEFAULT.postHitIframeTicks()
        );
    }

    public static final Codec<MulticastEchoStaffAttackProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("repeat_damage_multiplier", DEFAULT.repeatDamageMultiplier())
                            .forGetter(MulticastEchoStaffAttackProfile::repeatDamageMultiplier),
                    Codec.BOOL.optionalFieldOf("ignore_iframe", DEFAULT.ignoreIframe())
                            .forGetter(MulticastEchoStaffAttackProfile::ignoreIframe),
                    Codec.BOOL.optionalFieldOf("projectile_tracking", DEFAULT.projectileTracking())
                            .forGetter(MulticastEchoStaffAttackProfile::projectileTracking),
                    Codec.BOOL.optionalFieldOf("direct_damage_tracking", DEFAULT.directDamageTracking())
                            .forGetter(MulticastEchoStaffAttackProfile::directDamageTracking),
                    Codec.INT.optionalFieldOf("tracking_lifetime_ticks", DEFAULT.trackingLifetimeTicks())
                            .forGetter(MulticastEchoStaffAttackProfile::trackingLifetimeTicks),
                    Codec.INT.optionalFieldOf("post_hit_iframe_ticks", DEFAULT.postHitIframeTicks())
                            .forGetter(MulticastEchoStaffAttackProfile::postHitIframeTicks)
            ).apply(instance, MulticastEchoStaffAttackProfile::new)
    );
}
