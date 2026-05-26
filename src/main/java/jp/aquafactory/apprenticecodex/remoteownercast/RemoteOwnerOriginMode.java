package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RemoteOwnerOriginMode implements StringRepresentable {
    PROVIDED_ORIGIN("provided_origin"),
    PLAYER_SELF("player_self");

    private static final Map<String, RemoteOwnerOriginMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RemoteOwnerOriginMode::getSerializedName, Function.identity()));

    public static final Codec<RemoteOwnerOriginMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Remote Owner Cast origin mode: " + name);
                }
                return DataResult.success(mode);
            },
            RemoteOwnerOriginMode::getSerializedName
    );

    private final String serializedName;

    RemoteOwnerOriginMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
