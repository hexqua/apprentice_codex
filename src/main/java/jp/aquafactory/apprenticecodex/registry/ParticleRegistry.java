package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import net.minecraft.core.registries.Registries;
import jp.aquafactory.apprenticecodex.particle.SmashcastDustPillarParticleOptions;
import jp.aquafactory.apprenticecodex.particle.SmashcastTremorBlockParticleOptions;
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

    public static final DeferredHolder<ParticleType<?>, ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_CIRCLE =
            PARTICLES.register("additive_circle", () -> new ParticleType<>(false) {
                @Override
                public com.mojang.serialization.@NotNull MapCodec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.mapCodec(this);
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, AdditiveGlowParticleOptions> streamCodec() {
                    return AdditiveGlowParticleOptions.streamCodec(this);
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_RHOMBUS =
            PARTICLES.register("additive_rhombus", () -> new ParticleType<>(false) {
                @Override
                public com.mojang.serialization.@NotNull MapCodec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.mapCodec(this);
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, AdditiveGlowParticleOptions> streamCodec() {
                    return AdditiveGlowParticleOptions.streamCodec(this);
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_SPARK =
            PARTICLES.register("additive_spark", () -> new ParticleType<>(false) {
                @Override
                public com.mojang.serialization.@NotNull MapCodec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.mapCodec(this);
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, AdditiveGlowParticleOptions> streamCodec() {
                    return AdditiveGlowParticleOptions.streamCodec(this);
                }
            });

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

    public static final RegistryObject<ParticleType<SmashcastTremorBlockParticleOptions>> SMASHCAST_TREMOR_BLOCK =
            PARTICLES.register("smashcast_tremor_block", () -> new ParticleType<>(true,
                    SmashcastTremorBlockParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.@NotNull Codec<SmashcastTremorBlockParticleOptions> codec() {
                    return SmashcastTremorBlockParticleOptions.CODEC;
                }
            });

    public static final RegistryObject<ParticleType<SmashcastDustPillarParticleOptions>> SMASHCAST_DUST_PILLAR =
            PARTICLES.register("smashcast_dust_pillar", () -> new ParticleType<>(true,
                    SmashcastDustPillarParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.@NotNull Codec<SmashcastDustPillarParticleOptions> codec() {
                    return SmashcastDustPillarParticleOptions.CODEC;
                }
            });
}

