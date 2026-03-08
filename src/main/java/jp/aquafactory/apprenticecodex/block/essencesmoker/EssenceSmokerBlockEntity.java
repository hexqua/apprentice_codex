package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EssenceSmokerBlockEntity extends BlockEntity {
    public EssenceSmokerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ESSENCE_SMOKER.get(), pos, state);
    }
}
