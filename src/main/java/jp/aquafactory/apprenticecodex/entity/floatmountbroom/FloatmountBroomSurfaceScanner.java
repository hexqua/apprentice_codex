package jp.aquafactory.apprenticecodex.entity.floatmountbroom;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.OptionalDouble;

public final class FloatmountBroomSurfaceScanner {
    private FloatmountBroomSurfaceScanner() {
    }

    public static OptionalDouble findSurfaceBelow(Level level, double x, double startY, double z,
                                                  int maxBlocks, boolean includeLava) {
        var cursor = BlockPos.containing(x, startY, z);
        var context = CollisionContext.empty();
        for (var offset = 0; offset <= maxBlocks; offset++) {
            var pos = cursor.below(offset);
            var state = level.getBlockState(pos);
            var fluid = level.getFluidState(pos);
            if (!fluid.isEmpty() && (fluid.is(FluidTags.WATER) || fluid.is(FluidTags.LAVA))) {
                if (fluid.is(FluidTags.WATER) || includeLava) {
                    // MistFormと同様、流水も検知したブロックの上面を液面として扱う。
                    return OptionalDouble.of(pos.getY() + 1.0D);
                }
                // 溶岩ブロック自身のcollision shapeを安全な地表として解釈しない。
                continue;
            }
            var shape = state.getCollisionShape(level, pos, context);
            if (!shape.isEmpty()) {
                return OptionalDouble.of(pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y));
            }
        }
        return OptionalDouble.empty();
    }
}
