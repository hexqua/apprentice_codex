package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.particle.SmashcastDustPillarParticleOptions;
import jp.aquafactory.apprenticecodex.particle.SmashcastTremorBlockParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public final class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "apprenticecodex");

    public static final RegistryObject<SimpleParticleType> RETICLE_DOT =
            PARTICLES.register("reticle_dot", () -> new SimpleParticleType(true));

    public static final RegistryObject<ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_CIRCLE =
            PARTICLES.register("additive_circle", () -> new ParticleType<>(false,
                    AdditiveGlowParticleOptions.deserializer()) {
                @Override
                public com.mojang.serialization.@NotNull Codec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.codec(this);
                }
            });

    public static final RegistryObject<ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_RHOMBUS =
            PARTICLES.register("additive_rhombus", () -> new ParticleType<>(false,
                    AdditiveGlowParticleOptions.deserializer()) {
                @Override
                public com.mojang.serialization.@NotNull Codec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.codec(this);
                }
            });

    public static final RegistryObject<ParticleType<AdditiveGlowParticleOptions>> ADDITIVE_SPARK =
            PARTICLES.register("additive_spark", () -> new ParticleType<>(false,
                    AdditiveGlowParticleOptions.deserializer()) {
                @Override
                public com.mojang.serialization.@NotNull Codec<AdditiveGlowParticleOptions> codec() {
                    return AdditiveGlowParticleOptions.codec(this);
                }
            });

    public static final RegistryObject<ParticleType<MuzzleFlashParticleOptions>> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new ParticleType<>(false, MuzzleFlashParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.@NotNull Codec<MuzzleFlashParticleOptions> codec() {
                    return MuzzleFlashParticleOptions.CODEC;
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
