package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArcanumInAJarBlockEntity extends BlockEntity {
    // 演出はクライアント描画だけで完結させるため、同期用データは持たない.
    public ArcanumInAJarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ARCANUM_IN_A_JAR.get(), pos, state);
    }
}
