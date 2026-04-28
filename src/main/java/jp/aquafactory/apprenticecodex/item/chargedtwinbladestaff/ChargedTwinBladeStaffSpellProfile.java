package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChargedTwinBladeStaffSpellProfile(
        ChargedTwinBladeStaffCastMode castMode,
        boolean allowInitialRecast
) {
    public static final ChargedTwinBladeStaffSpellProfile PLAYER_SELF = new ChargedTwinBladeStaffSpellProfile(
            ChargedTwinBladeStaffCastMode.PLAYER_SELF,
            false
    );
    public static final ChargedTwinBladeStaffSpellProfile IMPACT_PROXY_OWNER_MAGIC = new ChargedTwinBladeStaffSpellProfile(
            ChargedTwinBladeStaffCastMode.IMPACT_PROXY_OWNER_MAGIC,
            false
    );
    public static final ChargedTwinBladeStaffSpellProfile IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST = new ChargedTwinBladeStaffSpellProfile(
            ChargedTwinBladeStaffCastMode.IMPACT_PROXY_OWNER_MAGIC,
            true
    );

    public static final Codec<ChargedTwinBladeStaffSpellProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChargedTwinBladeStaffCastMode.CODEC.fieldOf("cast_mode")
                            .forGetter(ChargedTwinBladeStaffSpellProfile::castMode),
                    Codec.BOOL.optionalFieldOf("allow_initial_recast", false)
                            .forGetter(ChargedTwinBladeStaffSpellProfile::allowInitialRecast)
            ).apply(instance, ChargedTwinBladeStaffSpellProfile::new)
    );
}
