package jp.aquafactory.apprenticecodex.spell.tinylumberjack;

import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

final class TinyLumberjackDropMover {
    private TinyLumberjackDropMover() {
        // do nothing.
    }

    static void breakBlockByPlayerHands(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack dummyTool, Vec3 attractPos) {
        // 疑似ツールのエンチャントも破壊時点の装備を参照するため、ドロップ集約も開始時フラグではなく現在の装備に追従する.
        if (!CraftsmansDelight.isEquippedBy(player)) {
            BlockTools.breakBlockByPlayerHands(level, player, pos, dummyTool);
            return;
        }

        var dropBox = createDropBox(pos);
        var beforeIds = captureNearbyItemIds(level, dropBox);
        BlockTools.breakBlockByPlayerHands(level, player, pos, dummyTool);
        moveNewDropsTo(level, dropBox, beforeIds, attractPos);
    }

    private static AABB createDropBox(BlockPos pos) {
        return new AABB(pos).inflate(1.5);
    }

    private static Set<Integer> captureNearbyItemIds(ServerLevel level, AABB box) {
        var ids = new HashSet<Integer>();
        for (var item : level.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
            ids.add(item.getId());
        }
        return ids;
    }

    private static int moveNewDropsTo(ServerLevel level, AABB box, Set<Integer> beforeIds, Vec3 attractPos) {
        var moved = 0;
        for (var item : level.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
            if (beforeIds.contains(item.getId())) {
                continue;
            }
            item.setPos(attractPos.x, attractPos.y, attractPos.z);
            item.setDeltaMovement(Vec3.ZERO);
            item.setNoPickUpDelay();
            item.hurtMarked = true;
            ++moved;
        }
        return moved;
    }
}
