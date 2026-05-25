package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RemoteOwnerDirectionMode implements StringRepresentable {
    PROVIDED_FORWARD("provided_forward"),
    PLAYER_LOOK("player_look");

    private static final Map<String, RemoteOwnerDirectionMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RemoteOwnerDirectionMode::getSerializedName, Function.identity()));

    public static final Codec<RemoteOwnerDirectionMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Remote Owner Cast direction mode: " + name);
                }
                return DataResult.success(mode);
            },
            RemoteOwnerDirectionMode::getSerializedName
    );

    private final String serializedName;

    RemoteOwnerDirectionMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
