package jp.aquafactory.apprenticecodex.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public record SmashcastDustPillarParticleOptions(BlockState state) implements ParticleOptions {
    public static final Codec<SmashcastDustPillarParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("block_state")
                            .forGetter(SmashcastDustPillarParticleOptions::state)
            ).apply(instance, SmashcastDustPillarParticleOptions::new)
    );

    @SuppressWarnings("deprecation")
    public static final Deserializer<SmashcastDustPillarParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull SmashcastDustPillarParticleOptions fromCommand(
                @NotNull ParticleType<SmashcastDustPillarParticleOptions> type,
                @NotNull StringReader reader
        ) throws CommandSyntaxException {
            reader.expect(' ');
            var state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false)
                    .blockState();
            return new SmashcastDustPillarParticleOptions(state);
        }

        @Override
        public @NotNull SmashcastDustPillarParticleOptions fromNetwork(
                @NotNull ParticleType<SmashcastDustPillarParticleOptions> type,
                @NotNull FriendlyByteBuf buf
        ) {
            return new SmashcastDustPillarParticleOptions(Block.stateById(buf.readVarInt()));
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.SMASHCAST_DUST_PILLAR.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(Block.getId(state));
    }

    @Override
    public @NotNull String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(getType()) + " " + state;
    }
}
