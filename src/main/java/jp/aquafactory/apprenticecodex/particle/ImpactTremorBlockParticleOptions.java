package jp.aquafactory.apprenticecodex.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public record ImpactTremorBlockParticleOptions(BlockState state, Vec3 motion, Source source) implements ParticleOptions {
    public static final Codec<ImpactTremorBlockParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("block_state")
                            .forGetter(ImpactTremorBlockParticleOptions::state),
                    Vec3.CODEC.optionalFieldOf("motion", Vec3.ZERO)
                            .forGetter(ImpactTremorBlockParticleOptions::motion),
                    Source.CODEC.optionalFieldOf("source", Source.SMASHCAST_SCEPTER)
                            .forGetter(ImpactTremorBlockParticleOptions::source)
            ).apply(instance, ImpactTremorBlockParticleOptions::new)
    );

    public ImpactTremorBlockParticleOptions(BlockState state) {
        this(state, Vec3.ZERO, Source.SMASHCAST_SCEPTER);
    }

    public ImpactTremorBlockParticleOptions(BlockState state, Vec3 motion) {
        this(state, motion, Source.SMASHCAST_SCEPTER);
    }

    @SuppressWarnings("deprecation")
    public static final Deserializer<ImpactTremorBlockParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull ImpactTremorBlockParticleOptions fromCommand(
                @NotNull ParticleType<ImpactTremorBlockParticleOptions> type,
                @NotNull StringReader reader
        ) throws CommandSyntaxException {
            reader.expect(' ');
            var state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false)
                    .blockState();
            var vector = DustParticleOptionsBase.readVector3f(reader);
            return new ImpactTremorBlockParticleOptions(state, new Vec3(vector.x(), vector.y(), vector.z()));
        }

        @Override
        public @NotNull ImpactTremorBlockParticleOptions fromNetwork(
                @NotNull ParticleType<ImpactTremorBlockParticleOptions> type,
                @NotNull FriendlyByteBuf buf
        ) {
            var state = Block.stateById(buf.readVarInt());
            var motion = buf.readBoolean()
                    ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
                    : Vec3.ZERO;
            var source = Source.fromNetworkId(buf.readVarInt());
            return new ImpactTremorBlockParticleOptions(state, motion, source);
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.IMPACT_TREMOR_BLOCK.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(Block.getId(state));
        if (motion == Vec3.ZERO) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeDouble(motion.x);
            buf.writeDouble(motion.y);
            buf.writeDouble(motion.z);
        }
        buf.writeVarInt(source.networkId);
    }

    @Override
    public @NotNull String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(getType()) + " " + state + " " + motion;
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
