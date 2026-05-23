package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SpellDispenserCasterMode implements StringRepresentable {
    AUTO("auto"),
    FAKE_PLAYER("fake_player"),
    NEUTRAL_LIVING("neutral_living");

    private static final Map<String, SpellDispenserCasterMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SpellDispenserCasterMode::getSerializedName, Function.identity()));

    public static final Codec<SpellDispenserCasterMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var mode = BY_NAME.get(name);
                if (mode == null) {
                    return DataResult.error(() -> "Unknown Spell Dispenser caster mode: " + name);
                }
                return DataResult.success(mode);
            },
            SpellDispenserCasterMode::getSerializedName
    );

    private final String serializedName;

    SpellDispenserCasterMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
