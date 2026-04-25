package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ChargedTwinBladeStaffCastMode implements StringRepresentable {
    PLAYER_SELF("player_self"),
    IMPACT_PROXY_OWNER_MAGIC("impact_proxy_owner_magic");

    private static final Map<String, ChargedTwinBladeStaffCastMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ChargedTwinBladeStaffCastMode::getSerializedName, Function.identity()));

    public static final Codec<ChargedTwinBladeStaffCastMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Charged Twin Blade Staff cast mode: " + name);
                }
                return DataResult.success(mode);
            },
            ChargedTwinBladeStaffCastMode::getSerializedName
    );

    private final String serializedName;

    ChargedTwinBladeStaffCastMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
