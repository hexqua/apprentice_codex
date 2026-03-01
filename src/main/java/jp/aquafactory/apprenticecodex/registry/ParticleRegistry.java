package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public final class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, "apprenticecodex");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RETICLE_DOT =
            PARTICLES.register("reticle_dot", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, ParticleType<MuzzleFlashParticleOptions>> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new ParticleType<>(false) {
                @Override
                public com.mojang.serialization.@NotNull MapCodec<MuzzleFlashParticleOptions> codec() {
                    return MuzzleFlashParticleOptions.MAP_CODEC;
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, MuzzleFlashParticleOptions> streamCodec() {
                    return MuzzleFlashParticleOptions.STREAM_CODEC;
                }
            });
}

