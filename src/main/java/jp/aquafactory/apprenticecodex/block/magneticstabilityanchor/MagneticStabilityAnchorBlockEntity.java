package jp.aquafactory.apprenticecodex.block.magneticstabilityanchor;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MagneticStabilityAnchorBlockEntity extends BlockEntity {
    public MagneticStabilityAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.MAGNETIC_STABILITY_ANCHOR.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            MagneticStabilityAnchorProtection.register(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            MagneticStabilityAnchorProtection.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }
}
