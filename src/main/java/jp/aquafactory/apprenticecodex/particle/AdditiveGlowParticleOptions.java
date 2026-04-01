package jp.aquafactory.apprenticecodex.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

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

    public static MapCodec<AdditiveGlowParticleOptions> mapCodec(ParticleType<AdditiveGlowParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.floatRange(0.01F, 4.0F).fieldOf("size").forGetter(AdditiveGlowParticleOptions::size),
                        Codec.floatRange(0.0F, 1.0F).fieldOf("red").forGetter(AdditiveGlowParticleOptions::red),
                        Codec.floatRange(0.0F, 1.0F).fieldOf("green").forGetter(AdditiveGlowParticleOptions::green),
                        Codec.floatRange(0.0F, 1.0F).fieldOf("blue").forGetter(AdditiveGlowParticleOptions::blue),
                        Codec.intRange(0, 200).fieldOf("whiten_ticks").forGetter(AdditiveGlowParticleOptions::whitenTicks),
                        Codec.INT.optionalFieldOf("lifetime", DEFAULT_INT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::lifetime),
                        Codec.INT.optionalFieldOf("lifetime_variance", DEFAULT_INT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::lifetimeVariance),
                        Codec.FLOAT.optionalFieldOf("min_size_multiplier", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::minSizeMultiplier),
                        Codec.FLOAT.optionalFieldOf("max_size_multiplier", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::maxSizeMultiplier),
                        Codec.FLOAT.optionalFieldOf("min_alpha", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::minAlpha),
                        Codec.FLOAT.optionalFieldOf("max_alpha", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::maxAlpha),
                        Codec.FLOAT.optionalFieldOf("fade_in_end", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::fadeInEnd),
                        Codec.FLOAT.optionalFieldOf("fade_out_start", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::fadeOutStart),
                        Codec.FLOAT.optionalFieldOf("end_scale_multiplier", DEFAULT_FLOAT_OVERRIDE).forGetter(AdditiveGlowParticleOptions::endScaleMultiplier),
                        Codec.BOOL.optionalFieldOf("use_linear_alpha_fade", false).forGetter(AdditiveGlowParticleOptions::useLinearAlphaFade)
                ).apply(instance, (size, red, green, blue, whitenTicks, lifetime, lifetimeVariance,
                                   minSizeMultiplier, maxSizeMultiplier, minAlpha, maxAlpha,
                                   fadeInEnd, fadeOutStart, endScaleMultiplier, useLinearAlphaFade) ->
                        new AdditiveGlowParticleOptions(
                                type,
                                size,
                                red,
                                green,
                                blue,
                                whitenTicks,
                                lifetime,
                                lifetimeVariance,
                                minSizeMultiplier,
                                maxSizeMultiplier,
                                minAlpha,
                                maxAlpha,
                                fadeInEnd,
                                fadeOutStart,
                                endScaleMultiplier,
                                useLinearAlphaFade
                        ))
        );
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AdditiveGlowParticleOptions> streamCodec(ParticleType<AdditiveGlowParticleOptions> type) {
        return StreamCodec.of(
                (buffer, particle) -> encode(particle, buffer),
                buffer -> decode(type, buffer)
        );
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    private static void encode(AdditiveGlowParticleOptions particle, RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(particle.size());
        buffer.writeFloat(particle.red());
        buffer.writeFloat(particle.green());
        buffer.writeFloat(particle.blue());
        buffer.writeVarInt(particle.whitenTicks());
        buffer.writeVarInt(particle.lifetime());
        buffer.writeVarInt(particle.lifetimeVariance());
        buffer.writeFloat(particle.minSizeMultiplier());
        buffer.writeFloat(particle.maxSizeMultiplier());
        buffer.writeFloat(particle.minAlpha());
        buffer.writeFloat(particle.maxAlpha());
        buffer.writeFloat(particle.fadeInEnd());
        buffer.writeFloat(particle.fadeOutStart());
        buffer.writeFloat(particle.endScaleMultiplier());
        buffer.writeBoolean(particle.useLinearAlphaFade());
    }

    private static AdditiveGlowParticleOptions decode(ParticleType<AdditiveGlowParticleOptions> type, RegistryFriendlyByteBuf buffer) {
        return new AdditiveGlowParticleOptions(
                type,
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }
}
