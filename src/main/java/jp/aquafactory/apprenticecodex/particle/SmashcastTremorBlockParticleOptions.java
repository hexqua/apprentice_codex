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
import net.minecraft.world.phys.Vec3;

public record SmashcastTremorBlockParticleOptions(BlockState state, Vec3 motion) implements ParticleOptions {
    public SmashcastTremorBlockParticleOptions(BlockState state) {
        this(state, Vec3.ZERO);
    }

    public static MapCodec<SmashcastTremorBlockParticleOptions> mapCodec(
            ParticleType<SmashcastTremorBlockParticleOptions> ignored
    ) {
        return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        BlockState.CODEC.fieldOf("block_state")
                                .forGetter(SmashcastTremorBlockParticleOptions::state),
                        Vec3.CODEC.optionalFieldOf("motion", Vec3.ZERO)
                                .forGetter(SmashcastTremorBlockParticleOptions::motion)
                ).apply(instance, SmashcastTremorBlockParticleOptions::new)
        );
    }

    public static StreamCodec<RegistryFriendlyByteBuf, SmashcastTremorBlockParticleOptions> streamCodec(
            ParticleType<SmashcastTremorBlockParticleOptions> ignored
    ) {
        return StreamCodec.of(SmashcastTremorBlockParticleOptions::encode, SmashcastTremorBlockParticleOptions::decode);
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleRegistry.SMASHCAST_TREMOR_BLOCK.get();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SmashcastTremorBlockParticleOptions options) {
        buffer.writeVarInt(Block.getId(options.state()));
        buffer.writeBoolean(options.motion() != Vec3.ZERO);
        if (options.motion() != Vec3.ZERO) {
            buffer.writeDouble(options.motion().x);
            buffer.writeDouble(options.motion().y);
            buffer.writeDouble(options.motion().z);
        }
    }

    private static SmashcastTremorBlockParticleOptions decode(RegistryFriendlyByteBuf buffer) {
        var state = Block.stateById(buffer.readVarInt());
        var motion = buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                : Vec3.ZERO;
        return new SmashcastTremorBlockParticleOptions(state, motion);
    }
}
