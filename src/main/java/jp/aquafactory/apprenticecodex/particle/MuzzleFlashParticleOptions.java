package jp.aquafactory.apprenticecodex.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MuzzleFlashParticleOptions(float size) implements ParticleOptions {
    public static final MapCodec<MuzzleFlashParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.floatRange(0.0f, 10.0f)
                            .fieldOf("size")
                            .forGetter(MuzzleFlashParticleOptions::size)
            ).apply(instance, MuzzleFlashParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MuzzleFlashParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            MuzzleFlashParticleOptions::size,
            MuzzleFlashParticleOptions::new
    );

    @Override
    public ParticleType<?> getType() {
        return ParticleRegistry.MUZZLE_FLASH.get();
    }
}
