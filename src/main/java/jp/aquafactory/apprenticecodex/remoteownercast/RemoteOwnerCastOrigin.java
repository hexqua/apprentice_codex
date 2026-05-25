package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RemoteOwnerCastOrigin implements StringRepresentable {
    SATELLITE_FOLLOWCAST("satellite_followcast"),
    CHARGED_TWIN_BLADE_STAFF_IMPACT("charged_twin_blade_staff_impact");

    private static final Map<String, RemoteOwnerCastOrigin> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RemoteOwnerCastOrigin::getSerializedName, Function.identity()));

    public static final Codec<RemoteOwnerCastOrigin> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var origin = BY_NAME.get(name);
                if (origin == null) {
                    return DataResult.error(() -> "Unknown Remote Owner Cast origin: " + name);
                }
                return DataResult.success(origin);
            },
            RemoteOwnerCastOrigin::getSerializedName
    );

    private final String serializedName;

    RemoteOwnerCastOrigin(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
