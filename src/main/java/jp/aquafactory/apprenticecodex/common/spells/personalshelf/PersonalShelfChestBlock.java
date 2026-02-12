package jp.aquafactory.apprenticecodex.common.spells.personalshelf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersonalShelfChestBlock extends BaseEntityBlock {
    public PersonalShelfChestBlock() {
        super(Properties.of()
                .strength(4.0f)
                .sound(SoundType.STONE)
                .lightLevel(s -> 8));
    }

    // 害は無いっぽいので警告握りつぶしをする.
    @SuppressWarnings("deprecation")
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PersonalShelfChestBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PersonalShelfChestBlockEntity shelf && player instanceof ServerPlayer server){
            NetworkHooks.openScreen(server, shelf, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
