package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
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

    public static final RegistryObject<ParticleType<MuzzleFlashParticleOptions>> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new ParticleType<>(false, MuzzleFlashParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.@NotNull Codec<MuzzleFlashParticleOptions> codec() {
                    return MuzzleFlashParticleOptions.CODEC;
                }
            });
}
