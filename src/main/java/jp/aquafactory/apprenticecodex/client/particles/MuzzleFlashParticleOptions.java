package jp.aquafactory.apprenticecodex.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.common.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record MuzzleFlashParticleOptions(float size) implements ParticleOptions {
    public static final MapCodec<MuzzleFlashParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.floatRange(0.0f, 10.0f).fieldOf("size").forGetter(MuzzleFlashParticleOptions::size))
                    .apply(instance, MuzzleFlashParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MuzzleFlashParticleOptions> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, MuzzleFlashParticleOptions::size, MuzzleFlashParticleOptions::new);

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.MUZZLE_FLASH.get();
    }
}
