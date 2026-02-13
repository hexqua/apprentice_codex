package jp.aquafactory.apprenticecodex.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public record MuzzleFlashParticleOptions(float size) implements ParticleOptions {
    public static final Codec<MuzzleFlashParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.floatRange(0.0f, 10.0f)
                            .fieldOf("size")
                            .forGetter(MuzzleFlashParticleOptions::size)
            ).apply(instance, MuzzleFlashParticleOptions::new)
    );

    // 1.20.1専用のため(1.21.1対応時に考える)
    @SuppressWarnings("deprecation")
    public static final Deserializer<MuzzleFlashParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull MuzzleFlashParticleOptions fromCommand(@NotNull ParticleType<MuzzleFlashParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new MuzzleFlashParticleOptions(size);
        }

        @Override
        public @NotNull MuzzleFlashParticleOptions fromNetwork(@NotNull ParticleType<MuzzleFlashParticleOptions> type, FriendlyByteBuf buf) {
            float size = buf.readFloat();
            return new MuzzleFlashParticleOptions(size);
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.MUZZLE_FLASH.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(this.size);
    }

    @Override
    public @NotNull String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " " + this.size;
    }
}
