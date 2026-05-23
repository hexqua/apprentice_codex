package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellDispenserSpellProfile(
        SpellDispenserCastAnchorMode castAnchor,
        SpellDispenserCasterMode casterMode,
        double forwardOffset,
        double upOffset,
        double sideOffset,
        float yawOffset,
        float pitchOffset,
        boolean ownerRequired
) {
    public static final SpellDispenserSpellProfile DEFAULT = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            SpellDispenserCasterMode.AUTO,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            true
    );

    public static final SpellDispenserSpellProfile MINIMUM_CONE = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.TRACKED_ANCHOR,
            SpellDispenserCasterMode.AUTO,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final SpellDispenserSpellProfile CONE_BACKWARD = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.TRACKED_ANCHOR,
            SpellDispenserCasterMode.AUTO,
            -1.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final SpellDispenserSpellProfile OWNER_OPTIONAL = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            SpellDispenserCasterMode.AUTO,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final SpellDispenserSpellProfile PROXY_NEUTRAL = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            SpellDispenserCasterMode.NEUTRAL_LIVING,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final SpellDispenserSpellProfile OWNER_OPTIONAL_UP = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            SpellDispenserCasterMode.AUTO,
            0.0D,
            0.5D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final SpellDispenserSpellProfile OWNER_OPTIONAL_BACKWARD = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            SpellDispenserCasterMode.AUTO,
            -1.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F,
            false
    );

    public static final Codec<SpellDispenserSpellProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SpellDispenserCastAnchorMode.CODEC.optionalFieldOf("cast_anchor", DEFAULT.castAnchor()).forGetter(SpellDispenserSpellProfile::castAnchor),
                    SpellDispenserCasterMode.CODEC.optionalFieldOf("caster_mode", DEFAULT.casterMode()).forGetter(SpellDispenserSpellProfile::casterMode),
                    Codec.DOUBLE.optionalFieldOf("forward_offset", DEFAULT.forwardOffset()).forGetter(SpellDispenserSpellProfile::forwardOffset),
                    Codec.DOUBLE.optionalFieldOf("up_offset", DEFAULT.upOffset()).forGetter(SpellDispenserSpellProfile::upOffset),
                    Codec.DOUBLE.optionalFieldOf("side_offset", DEFAULT.sideOffset()).forGetter(SpellDispenserSpellProfile::sideOffset),
                    Codec.FLOAT.optionalFieldOf("yaw_offset", DEFAULT.yawOffset()).forGetter(SpellDispenserSpellProfile::yawOffset),
                    Codec.FLOAT.optionalFieldOf("pitch_offset", DEFAULT.pitchOffset()).forGetter(SpellDispenserSpellProfile::pitchOffset),
                    Codec.BOOL.optionalFieldOf("owner_required", DEFAULT.ownerRequired()).forGetter(SpellDispenserSpellProfile::ownerRequired)
            ).apply(instance, SpellDispenserSpellProfile::new)
    );
}
