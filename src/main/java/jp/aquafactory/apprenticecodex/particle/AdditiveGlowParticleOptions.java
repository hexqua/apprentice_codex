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
                                          int whitenTicks) implements ParticleOptions {

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
                                .forGetter(AdditiveGlowParticleOptions::whitenTicks)
                ).apply(instance, (size, red, green, blue, whitenTicks) ->
                        new AdditiveGlowParticleOptions(type, size, red, green, blue, whitenTicks))
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
                return new AdditiveGlowParticleOptions(type, size, red, green, blue, whitenTicks);
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
