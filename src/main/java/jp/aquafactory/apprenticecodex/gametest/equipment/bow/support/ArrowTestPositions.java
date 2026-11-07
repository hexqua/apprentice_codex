package jp.aquafactory.apprenticecodex.gametest.equipment.bow.support;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 壁・射線幅・逆方向への移動の余白を取り、局所座標を専用structure内へ配置する。
 */
public final class ArrowTestPositions {
    private ArrowTestPositions() {
    }

    public static Vec3 absolute(GameTestHelper helper, Vec3 local) {
        return helper.absoluteVec(local.add(12, 0, 12));
    }

    public static BlockPos absolute(GameTestHelper helper, BlockPos local) {
        return helper.absolutePos(local.offset(12, 0, 12));
    }

    public static void setBlock(GameTestHelper helper, BlockPos local, Block block) {
        helper.setBlock(local.offset(12, 0, 12), block);
    }

    public static void setBlock(GameTestHelper helper, BlockPos local, BlockState state) {
        helper.setBlock(local.offset(12, 0, 12), state);
    }
}
