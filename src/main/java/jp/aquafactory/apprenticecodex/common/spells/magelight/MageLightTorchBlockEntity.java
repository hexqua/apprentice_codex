package jp.aquafactory.apprenticecodex.common.spells.magelight;

import jp.aquafactory.apprenticecodex.common.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MageLightTorchBlockEntity extends BlockEntity {
    // tick類をなくすことで極力軽量化する.
    public MageLightTorchBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MAGE_LIGHT_TORCH.get(), pos, state);
    }
}
