package jp.aquafactory.apprenticecodex.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record SmashcastDustPillarParticleOptions(BlockState state) implements ParticleOptions {
    public static MapCodec<SmashcastDustPillarParticleOptions> mapCodec(
            ParticleType<SmashcastDustPillarParticleOptions> ignored
    ) {
        return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        BlockState.CODEC.fieldOf("block_state")
                                .forGetter(SmashcastDustPillarParticleOptions::state)
                ).apply(instance, SmashcastDustPillarParticleOptions::new)
        );
    }

    public static StreamCodec<RegistryFriendlyByteBuf, SmashcastDustPillarParticleOptions> streamCodec(
            ParticleType<SmashcastDustPillarParticleOptions> ignored
    ) {
        return StreamCodec.of(SmashcastDustPillarParticleOptions::encode, SmashcastDustPillarParticleOptions::decode);
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleRegistry.SMASHCAST_DUST_PILLAR.get();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SmashcastDustPillarParticleOptions options) {
        buffer.writeVarInt(Block.getId(options.state()));
    }

    private static SmashcastDustPillarParticleOptions decode(RegistryFriendlyByteBuf buffer) {
        return new SmashcastDustPillarParticleOptions(Block.stateById(buffer.readVarInt()));
    }
}
