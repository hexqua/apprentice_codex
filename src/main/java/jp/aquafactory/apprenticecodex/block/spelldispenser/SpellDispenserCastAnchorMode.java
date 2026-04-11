package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SpellDispenserCastAnchorMode implements StringRepresentable {
    AUTO("auto"),
    FAKE_PLAYER("fake_player"),
    TRACKED_ANCHOR("tracked_anchor");

    private static final Map<String, SpellDispenserCastAnchorMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SpellDispenserCastAnchorMode::getSerializedName, Function.identity()));

    public static final Codec<SpellDispenserCastAnchorMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Spell Dispenser cast anchor mode: " + name);
                }
                return DataResult.success(mode);
            },
            SpellDispenserCastAnchorMode::getSerializedName
    );

    private final String serializedName;

    SpellDispenserCastAnchorMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
