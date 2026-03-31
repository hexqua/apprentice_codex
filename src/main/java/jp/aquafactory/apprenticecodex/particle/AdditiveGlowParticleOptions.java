package jp.aquafactory.apprenticecodex.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public record AdditiveGlowParticleOptions(ParticleType<AdditiveGlowParticleOptions> type,
                                          float size,
                                          float red,
                                          float green,
                                          float blue,
                                          int whitenTicks,
                                          int lifetime,
                                          int lifetimeVariance,
                                          float minSizeMultiplier,
                                          float maxSizeMultiplier,
                                          float minAlpha,
                                          float maxAlpha,
                                          float fadeInEnd,
                                          float fadeOutStart,
                                          float endScaleMultiplier,
                                          boolean useLinearAlphaFade) implements ParticleOptions {
    private static final int DEFAULT_INT_OVERRIDE = -1;
    private static final float DEFAULT_FLOAT_OVERRIDE = -1.0F;

    public AdditiveGlowParticleOptions(ParticleType<AdditiveGlowParticleOptions> type,
                                       float size,
                                       float red,
                                       float green,
                                       float blue,
                                       int whitenTicks) {
        this(type, size, red, green, blue, whitenTicks,
                DEFAULT_INT_OVERRIDE, DEFAULT_INT_OVERRIDE,
                DEFAULT_FLOAT_OVERRIDE, DEFAULT_FLOAT_OVERRIDE,
                DEFAULT_FLOAT_OVERRIDE, DEFAULT_FLOAT_OVERRIDE,
                DEFAULT_FLOAT_OVERRIDE, DEFAULT_FLOAT_OVERRIDE,
                DEFAULT_FLOAT_OVERRIDE, false);
    }

    public static Codec<AdditiveGlowParticleOptions> codec(ParticleType<AdditiveGlowParticleOptions> type) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.floatRange(0.01F, 4.0F)
                                .fieldOf("size")
                                .forGetter(AdditiveGlowParticleOptions::size),
                        Codec.floatRange(0.0F, 1.0F)
                                .fieldOf("red")
                                .forGetter(AdditiveGlowParticleOptions::red),
                        Codec.floatRange(0.0F, 1.0F)
                                .fieldOf("green")
                                .forGetter(AdditiveGlowParticleOptions::green),
                        Codec.floatRange(0.0F, 1.0F)
                                .fieldOf("blue")
                                .forGetter(AdditiveGlowParticleOptions::blue),
                        Codec.intRange(0, 200)
                                .fieldOf("whiten_ticks")
                                .forGetter(AdditiveGlowParticleOptions::whitenTicks),
                        Codec.INT.optionalFieldOf("lifetime", DEFAULT_INT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::lifetime),
                        Codec.INT.optionalFieldOf("lifetime_variance", DEFAULT_INT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::lifetimeVariance),
                        Codec.FLOAT.optionalFieldOf("min_size_multiplier", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::minSizeMultiplier),
                        Codec.FLOAT.optionalFieldOf("max_size_multiplier", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::maxSizeMultiplier),
                        Codec.FLOAT.optionalFieldOf("min_alpha", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::minAlpha),
                        Codec.FLOAT.optionalFieldOf("max_alpha", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::maxAlpha),
                        Codec.FLOAT.optionalFieldOf("fade_in_end", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::fadeInEnd),
                        Codec.FLOAT.optionalFieldOf("fade_out_start", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::fadeOutStart),
                        Codec.FLOAT.optionalFieldOf("end_scale_multiplier", DEFAULT_FLOAT_OVERRIDE)
                                .forGetter(AdditiveGlowParticleOptions::endScaleMultiplier),
                        Codec.BOOL.optionalFieldOf("use_linear_alpha_fade", false)
                                .forGetter(AdditiveGlowParticleOptions::useLinearAlphaFade)
                ).apply(instance, (size, red, green, blue, whitenTicks, lifetime, lifetimeVariance,
                                   minSizeMultiplier, maxSizeMultiplier, minAlpha, maxAlpha,
                                   fadeInEnd, fadeOutStart, endScaleMultiplier, useLinearAlphaFade) ->
                        new AdditiveGlowParticleOptions(type, size, red, green, blue, whitenTicks,
                                lifetime, lifetimeVariance, minSizeMultiplier, maxSizeMultiplier,
                                minAlpha, maxAlpha, fadeInEnd, fadeOutStart, endScaleMultiplier, useLinearAlphaFade))
        );
    }

    // 1.20.1専用のため(1.21.1対応時に考える)
    @SuppressWarnings("deprecation")
    public static Deserializer<AdditiveGlowParticleOptions> deserializer() {
        return new Deserializer<>() {
            @Override
            public @NotNull AdditiveGlowParticleOptions fromCommand(@NotNull ParticleType<AdditiveGlowParticleOptions> type,
                                                                    @NotNull StringReader reader) throws CommandSyntaxException {
                reader.expect(' ');
                float size = reader.readFloat();
                reader.expect(' ');
                float red = reader.readFloat();
                reader.expect(' ');
                float green = reader.readFloat();
                reader.expect(' ');
                float blue = reader.readFloat();
                reader.expect(' ');
                int whitenTicks = reader.readInt();
                return new AdditiveGlowParticleOptions(type, size, red, green, blue, whitenTicks);
            }

            @Override
            public @NotNull AdditiveGlowParticleOptions fromNetwork(@NotNull ParticleType<AdditiveGlowParticleOptions> type,
                                                                    @NotNull FriendlyByteBuf buf) {
                float size = buf.readFloat();
                float red = buf.readFloat();
                float green = buf.readFloat();
                float blue = buf.readFloat();
                int whitenTicks = buf.readVarInt();
                int lifetime = buf.readVarInt();
                int lifetimeVariance = buf.readVarInt();
                float minSizeMultiplier = buf.readFloat();
                float maxSizeMultiplier = buf.readFloat();
                float minAlpha = buf.readFloat();
                float maxAlpha = buf.readFloat();
                float fadeInEnd = buf.readFloat();
                float fadeOutStart = buf.readFloat();
                float endScaleMultiplier = buf.readFloat();
                boolean useLinearAlphaFade = buf.readBoolean();
                return new AdditiveGlowParticleOptions(type, size, red, green, blue, whitenTicks,
                        lifetime, lifetimeVariance, minSizeMultiplier, maxSizeMultiplier,
                        minAlpha, maxAlpha, fadeInEnd, fadeOutStart, endScaleMultiplier, useLinearAlphaFade);
            }
        };
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(size);
        buf.writeFloat(red);
        buf.writeFloat(green);
        buf.writeFloat(blue);
        buf.writeVarInt(whitenTicks);
        buf.writeVarInt(lifetime);
        buf.writeVarInt(lifetimeVariance);
        buf.writeFloat(minSizeMultiplier);
        buf.writeFloat(maxSizeMultiplier);
        buf.writeFloat(minAlpha);
        buf.writeFloat(maxAlpha);
        buf.writeFloat(fadeInEnd);
        buf.writeFloat(fadeOutStart);
        buf.writeFloat(endScaleMultiplier);
        buf.writeBoolean(useLinearAlphaFade);
    }

    @Override
    public @NotNull String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(getType())
                + " " + size
                + " " + red
                + " " + green
                + " " + blue
                + " " + whitenTicks;
    }
}
