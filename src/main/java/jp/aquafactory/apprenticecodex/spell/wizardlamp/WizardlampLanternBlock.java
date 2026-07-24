package jp.aquafactory.apprenticecodex.spell.wizardlamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class WizardlampLanternBlock extends Block implements EntityBlock {
    public static final double FLOATING_OFFSET = 0.25D;

    public WizardlampLanternBlock() {
        // pickaxeタグ付きブロックの素手破壊は100除数になるため、硬さ0.3で30tickに合わせる。
        super(Properties.of()
                .strength(0.3F)
                .noOcclusion()
                .sound(SoundType.LANTERN)
                .lightLevel(state -> 15));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Blocks.LANTERN.defaultBlockState().getShape(level, pos, context)
                .move(0.0D, FLOATING_OFFSET, 0.0D);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Blocks.LANTERN.defaultBlockState().getCollisionShape(level, pos, context)
                .move(0.0D, FLOATING_OFFSET, 0.0D);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WizardlampLanternBlockEntity(pos, state);
    }
}
