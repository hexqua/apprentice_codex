package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DispenserDropTestSupport {
    private DispenserDropTestSupport() {}

    public static List<ItemEntity> getFreshItemDrops(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(radius),
                itemEntity -> itemEntity.getAge() <= 1
        );
    }
}
