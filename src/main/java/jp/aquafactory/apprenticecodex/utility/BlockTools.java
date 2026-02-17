package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public final class BlockTools {
    private BlockTools(){}

    public record PlaceData(BlockPos pos, Direction facing) {}

    public static Optional<PlaceData> findPlacePos(Level level, LivingEntity entity, double range) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(range));

        // 設置判定を見るので当たり判定通りのレイを飛ばす.
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }

        var hitPos = hit.getBlockPos();
        var hitState = level.getBlockState(hitPos);
        var placePos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hit.getDirection());
        var placeState = level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return Optional.empty();
        }

        return Optional.of(new PlaceData(placePos, hit.getDirection().getOpposite()));
    }

    public static void breakBlockByPlayerHands(Level level, ServerPlayer player, BlockPos pos, ItemStack dummyTool){
        if (player != null && !player.isRemoved()) {
            var originalItem = player.getMainHandItem();
            var state = level.getBlockState(pos);
            try{
                // ダミーツールをもたせる.
                player.setItemInHand(InteractionHand.MAIN_HAND, dummyTool);
                var isBroken = player.gameMode.destroyBlock(pos);

                // イベントを呼ばないと破壊演出が出ない.
                if (isBroken){
                    level.levelEvent(2001, pos, Block.getId(state));
                }
            } finally {
                // すぐにアイテムをもとに戻す.
                player.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
            }
        } else {
            level.destroyBlock(pos, true);
        }
    }
}
