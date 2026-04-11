package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellDispenserSpellProfile(
        SpellDispenserCastAnchorMode castAnchor,
        double forwardOffset,
        double upOffset,
        double sideOffset,
        float yawOffset,
        float pitchOffset
) {
    public static final SpellDispenserSpellProfile DEFAULT = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.AUTO,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F
    );

    public static final SpellDispenserSpellProfile MINIMUM_CONE = new SpellDispenserSpellProfile(
            SpellDispenserCastAnchorMode.TRACKED_ANCHOR,
            0.0D,
            0.0D,
            0.0D,
            0.0F,
            0.0F
    );

    public static final Codec<SpellDispenserSpellProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SpellDispenserCastAnchorMode.CODEC.optionalFieldOf("cast_anchor", DEFAULT.castAnchor()).forGetter(SpellDispenserSpellProfile::castAnchor),
                    Codec.DOUBLE.optionalFieldOf("forward_offset", DEFAULT.forwardOffset()).forGetter(SpellDispenserSpellProfile::forwardOffset),
                    Codec.DOUBLE.optionalFieldOf("up_offset", DEFAULT.upOffset()).forGetter(SpellDispenserSpellProfile::upOffset),
                    Codec.DOUBLE.optionalFieldOf("side_offset", DEFAULT.sideOffset()).forGetter(SpellDispenserSpellProfile::sideOffset),
                    Codec.FLOAT.optionalFieldOf("yaw_offset", DEFAULT.yawOffset()).forGetter(SpellDispenserSpellProfile::yawOffset),
                    Codec.FLOAT.optionalFieldOf("pitch_offset", DEFAULT.pitchOffset()).forGetter(SpellDispenserSpellProfile::pitchOffset)
            ).apply(instance, SpellDispenserSpellProfile::new)
    );
}
