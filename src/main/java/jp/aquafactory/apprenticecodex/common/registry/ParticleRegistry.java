package jp.aquafactory.apprenticecodex.common.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "apprenticecodex");

    public static final RegistryObject<SimpleParticleType> RETICLE_DOT =
            PARTICLES.register("reticle_dot", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new SimpleParticleType(true));

}
