package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RemoteOwnerCastMode implements StringRepresentable {
    REMOTE_PLAYER_GEOMETRY("remote_player_geometry"),
    PROXY_OWNER_MAGIC("proxy_owner_magic"),
    PLAYER_SELF("player_self"),
    LEGACY_SPELL_DISPENSER("legacy_spell_dispenser");

    private static final Map<String, RemoteOwnerCastMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RemoteOwnerCastMode::getSerializedName, Function.identity()));

    public static final Codec<RemoteOwnerCastMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Remote Owner Cast mode: " + name);
                }
                return DataResult.success(mode);
            },
            RemoteOwnerCastMode::getSerializedName
    );

    private final String serializedName;

    RemoteOwnerCastMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
