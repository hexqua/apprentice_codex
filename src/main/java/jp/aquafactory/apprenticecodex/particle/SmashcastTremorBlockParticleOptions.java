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

public record SmashcastTremorBlockParticleOptions(BlockState state, Vec3 motion) implements ParticleOptions {
    public static final Codec<SmashcastTremorBlockParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("block_state")
                            .forGetter(SmashcastTremorBlockParticleOptions::state),
                    Vec3.CODEC.optionalFieldOf("motion", Vec3.ZERO)
                            .forGetter(SmashcastTremorBlockParticleOptions::motion)
            ).apply(instance, SmashcastTremorBlockParticleOptions::new)
    );

    public SmashcastTremorBlockParticleOptions(BlockState state) {
        this(state, Vec3.ZERO);
    }

    @SuppressWarnings("deprecation")
    public static final Deserializer<SmashcastTremorBlockParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull SmashcastTremorBlockParticleOptions fromCommand(
                @NotNull ParticleType<SmashcastTremorBlockParticleOptions> type,
                @NotNull StringReader reader
        ) throws CommandSyntaxException {
            reader.expect(' ');
            var state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false)
                    .blockState();
            var vector = DustParticleOptionsBase.readVector3f(reader);
            return new SmashcastTremorBlockParticleOptions(state, new Vec3(vector.x(), vector.y(), vector.z()));
        }

        @Override
        public @NotNull SmashcastTremorBlockParticleOptions fromNetwork(
                @NotNull ParticleType<SmashcastTremorBlockParticleOptions> type,
                @NotNull FriendlyByteBuf buf
        ) {
            var state = Block.stateById(buf.readVarInt());
            var motion = buf.readBoolean()
                    ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
                    : Vec3.ZERO;
            return new SmashcastTremorBlockParticleOptions(state, motion);
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.SMASHCAST_TREMOR_BLOCK.get();
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
    }

    @Override
    public @NotNull String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(getType()) + " " + state + " " + motion;
    }
}
