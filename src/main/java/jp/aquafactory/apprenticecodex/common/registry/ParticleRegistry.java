package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.client.particles.MuzzleFlashParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, "apprenticecodex");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RETICLE_DOT =
            PARTICLES.register("reticle_dot", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, ParticleType<MuzzleFlashParticleOptions>> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new ParticleType<>(false) {
                @Override
                public com.mojang.serialization.MapCodec<MuzzleFlashParticleOptions> codec() {
                    return MuzzleFlashParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, MuzzleFlashParticleOptions> streamCodec() {
                    return MuzzleFlashParticleOptions.STREAM_CODEC;
                }
            });
}
