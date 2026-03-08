package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SenseEvilHighlightVariant implements StringRepresentable {
    NORMAL("normal", "normal"),
    LIGHT_STRONG("light_strong", "strong");

    private static final SenseEvilHighlightVariant[] VALUES = values();
    private static final Map<String, SenseEvilHighlightVariant> BY_NAME = Arrays.stream(VALUES)
            .collect(Collectors.toUnmodifiableMap(SenseEvilHighlightVariant::getSerializedName, Function.identity()));
    private static final Map<String, SenseEvilHighlightVariant> BY_DATA_FILE_NAME = Arrays.stream(VALUES)
            .collect(Collectors.toUnmodifiableMap(SenseEvilHighlightVariant::getDataFileName, Function.identity()));

    public static final Codec<SenseEvilHighlightVariant> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                var variant = BY_NAME.get(name);
                if (variant == null) {
                    return DataResult.error(() -> "Unknown SenseEvil highlight variant: " + name);
                }
                return DataResult.success(variant);
            },
            SenseEvilHighlightVariant::getSerializedName
    );

    private final String serializedName;
    private final String dataFileName;

    SenseEvilHighlightVariant(String serializedName, String dataFileName) {
        this.serializedName = serializedName;
        this.dataFileName = dataFileName;
    }

    public static SenseEvilHighlightVariant byNetworkId(int networkId) {
        if (networkId < 0 || networkId >= VALUES.length) {
            return NORMAL;
        }
        return VALUES[networkId];
    }

    public static SenseEvilHighlightVariant fromDataFileName(String dataFileName) {
        return BY_DATA_FILE_NAME.get(dataFileName);
    }

    public static SenseEvilHighlightVariant max(SenseEvilHighlightVariant first, SenseEvilHighlightVariant second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }

    public int toNetworkId() {
        return ordinal();
    }

    public String getDataFileName() {
        return dataFileName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
