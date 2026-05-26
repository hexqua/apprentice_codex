package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record RemoteOwnerCastProfile(
        RemoteOwnerCastMode castMode,
        RemoteOwnerOriginMode originMode,
        RemoteOwnerDirectionMode directionMode,
        Optional<List<RemoteOwnerCastOrigin>> allowedCastOrigins,
        boolean allowInitialRecast
) {
    public static final RemoteOwnerCastProfile REMOTE_PLAYER_GEOMETRY = new RemoteOwnerCastProfile(
            RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
            RemoteOwnerOriginMode.PROVIDED_ORIGIN,
            RemoteOwnerDirectionMode.PROVIDED_FORWARD,
            Optional.empty(),
            false
    );

    public static final Codec<RemoteOwnerCastProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RemoteOwnerCastMode.CODEC.fieldOf("cast_mode")
                            .forGetter(RemoteOwnerCastProfile::castMode),
                    RemoteOwnerOriginMode.CODEC.optionalFieldOf("origin_mode", RemoteOwnerOriginMode.PROVIDED_ORIGIN)
                            .forGetter(RemoteOwnerCastProfile::originMode),
                    RemoteOwnerDirectionMode.CODEC.optionalFieldOf("direction_mode", RemoteOwnerDirectionMode.PROVIDED_FORWARD)
                            .forGetter(RemoteOwnerCastProfile::directionMode),
                    RemoteOwnerCastOrigin.CODEC.listOf().optionalFieldOf("allowed_cast_origins")
                            .forGetter(RemoteOwnerCastProfile::allowedCastOrigins),
                    Codec.BOOL.optionalFieldOf("allow_initial_recast", false)
                            .forGetter(RemoteOwnerCastProfile::allowInitialRecast)
            ).apply(instance, RemoteOwnerCastProfile::new)
    );

    public boolean allowsOrigin(RemoteOwnerCastOrigin origin) {
        return allowedCastOrigins
                .map(origins -> origins.contains(origin))
                .orElse(true);
    }

    public RemoteOwnerCastProfile withCastMode(RemoteOwnerCastMode castMode) {
        return new RemoteOwnerCastProfile(castMode, originMode, directionMode, allowedCastOrigins, allowInitialRecast);
    }
}
