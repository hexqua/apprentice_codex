package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SenseEvilHighlightVariant implements StringRepresentable {
    NORMAL(
            "normal",
            "normal",
            new VisualProfile(
                    1.0f, 0.9f, 0.38f,
                    1.0f, 0.84f, 0.26f,
                    10, 6,
                    1.08f, 0.9f,
                    0.38f,
                    18, 0.82f,
                    1.0f, 1.0f
            )
    ),
    // variant はテクスチャ種別ではなく、壁越し表示での見せ方パターンを表す。
    STRONG(
            "strong",
            "strong",
            new VisualProfile(
                    1.0f, 0.24f, 0.14f,
                    1.0f, 0.34f, 0.16f,
                    12, 7,
                    1.14f, 0.96f,
                    0.44f,
                    22, 0.9f,
                    1.0f, 1.0f
            )
    );

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
    private final VisualProfile profile;

    SenseEvilHighlightVariant(String serializedName, String dataFileName, VisualProfile profile) {
        this.serializedName = serializedName;
        this.dataFileName = dataFileName;
        this.profile = profile;
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

    public VisualProfile getProfile() {
        return profile;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public record VisualProfile(
            float circleRed,
            float circleGreen,
            float circleBlue,
            float flameRed,
            float flameGreen,
            float flameBlue,
            int circleWhitenTicks,
            int flameWhitenTicks,
            float circleScaleMultiplier,
            float flameSizeMultiplier,
            float circleAlpha,
            int flameCount,
            float flameAlphaMultiplier,
            float flameWidthMultiplier,
            float flameHeightMultiplier
    ) {
    }
}
