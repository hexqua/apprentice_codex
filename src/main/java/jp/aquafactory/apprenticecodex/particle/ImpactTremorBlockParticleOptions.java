package jp.aquafactory.apprenticecodex.particle;

import com.mojang.serialization.Codec;
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

public record ImpactTremorBlockParticleOptions(BlockState state, Vec3 motion, Source source) implements ParticleOptions {
    public ImpactTremorBlockParticleOptions(BlockState state) {
        this(state, Vec3.ZERO, Source.SMASHCAST_SCEPTER);
    }

    public ImpactTremorBlockParticleOptions(BlockState state, Vec3 motion) {
        this(state, motion, Source.SMASHCAST_SCEPTER);
    }

    public static MapCodec<ImpactTremorBlockParticleOptions> mapCodec(
            ParticleType<ImpactTremorBlockParticleOptions> ignored
    ) {
        return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        BlockState.CODEC.fieldOf("block_state")
                                .forGetter(ImpactTremorBlockParticleOptions::state),
                        Vec3.CODEC.optionalFieldOf("motion", Vec3.ZERO)
                                .forGetter(ImpactTremorBlockParticleOptions::motion),
                        Source.CODEC.optionalFieldOf("source", Source.SMASHCAST_SCEPTER)
                                .forGetter(ImpactTremorBlockParticleOptions::source)
                ).apply(instance, ImpactTremorBlockParticleOptions::new)
        );
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ImpactTremorBlockParticleOptions> streamCodec(
            ParticleType<ImpactTremorBlockParticleOptions> ignored
    ) {
        return StreamCodec.of(ImpactTremorBlockParticleOptions::encode, ImpactTremorBlockParticleOptions::decode);
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleRegistry.IMPACT_TREMOR_BLOCK.get();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ImpactTremorBlockParticleOptions options) {
        buffer.writeVarInt(Block.getId(options.state()));
        buffer.writeBoolean(options.motion() != Vec3.ZERO);
        if (options.motion() != Vec3.ZERO) {
            buffer.writeDouble(options.motion().x);
            buffer.writeDouble(options.motion().y);
            buffer.writeDouble(options.motion().z);
        }
        buffer.writeVarInt(options.source().networkId);
    }

    private static ImpactTremorBlockParticleOptions decode(RegistryFriendlyByteBuf buffer) {
        var state = Block.stateById(buffer.readVarInt());
        var motion = buffer.readBoolean()
                ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                : Vec3.ZERO;
        var source = Source.fromNetworkId(buffer.readVarInt());
        return new ImpactTremorBlockParticleOptions(state, motion, source);
    }

    public enum Source {
        SMASHCAST_SCEPTER(0, "smashcast_scepter"),
        HEAVENLY_FIST(1, "heavenly_fist");

        private static final Codec<Source> CODEC = Codec.STRING.xmap(Source::fromSerializedName, Source::serializedName);

        private final int networkId;
        private final String serializedName;

        Source(int networkId, String serializedName) {
            this.networkId = networkId;
            this.serializedName = serializedName;
        }

        private static Source fromNetworkId(int networkId) {
            for (var source : values()) {
                if (source.networkId == networkId) {
                    return source;
                }
            }
            return SMASHCAST_SCEPTER;
        }

        private static Source fromSerializedName(String serializedName) {
            for (var source : values()) {
                if (source.serializedName.equals(serializedName)) {
                    return source;
                }
            }
            return SMASHCAST_SCEPTER;
        }

        private String serializedName() {
            return serializedName;
        }
    }
}
