package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

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

    public static boolean tryPlaceBlockByEntity(Level level, LivingEntity entity, BlockPos pos,
                                                BlockState state, Direction placedFace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.setBlockAndUpdate(pos, state);
        }

        if (entity instanceof ServerPlayer player
                && (!serverLevel.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, placedFace, ItemStack.EMPTY))) {
            return false;
        }

        var snapshot = BlockSnapshot.create(serverLevel.dimension(), serverLevel, pos);
        if (!serverLevel.setBlockAndUpdate(pos, state)) {
            return false;
        }

        // 魔法による直接配置も通常のBlockItem配置と同じForgeイベントへ流し、土地保護MODが拒否できるようにする。
        if (ForgeEventFactory.onBlockPlace(entity, snapshot, placedFace)) {
            snapshot.restore(true, false);
            return false;
        }
        return true;
    }

    public static void breakBlockByPlayerHands(Level level, ServerPlayer player, BlockPos pos, ItemStack dummyTool){
        tryBreakBlockByPlayerHands(level, player, pos, dummyTool);
    }

    public static boolean tryBreakBlockByPlayerHands(Level level, ServerPlayer player, BlockPos pos, ItemStack dummyTool){
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
                return isBroken;
            } finally {
                // すぐにアイテムをもとに戻す.
                player.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
            }
        } else {
            return level.destroyBlock(pos, true);
        }
    }

    public static InteractionResult useItemOnBlockByPlayerMainHand(Level level, ServerPlayer player, BlockPos pos, ItemStack interactionStack) {
        return useItemOnBlockByPlayerMainHand(level, player, pos, interactionStack, Direction.UP);
    }

    public static InteractionResult useBlockByPlayerMainHand(Level level, ServerPlayer player, BlockPos pos, ItemStack interactionStack) {
        return useBlockByPlayerMainHand(level, player, pos, interactionStack, Direction.UP);
    }

    public static InteractionResult useItemOnBlockByPlayerMainHand(Level level, ServerPlayer player, BlockPos pos,
                                                                   ItemStack interactionStack, Direction hitFace) {
        if (player == null || player.isRemoved()) {
            return InteractionResult.PASS;
        }

        var originalItem = player.getMainHandItem();
        var effectiveInteractionStack = copyForTemporaryUse(interactionStack);
        // まずは通常の useItemOn 経路へ流し、mod 独自の右クリック収穫を優先する。
        var hitResult = new BlockHitResult(Vec3.atCenterOf(pos), hitFace, pos, false);
        try {
            // 右クリック判定だけ現在手持ちのコピーへ差し替え、耐久や個数は本物へ反映しない。
            player.setItemInHand(InteractionHand.MAIN_HAND, effectiveInteractionStack);
            return player.gameMode.useItemOn(player, level, effectiveInteractionStack, InteractionHand.MAIN_HAND, hitResult);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
        }
    }

    public static ItemStack copyForTemporaryUse(ItemStack interactionStack) {
        if (interactionStack.isEmpty()) {
            return interactionStack;
        }

        var normalizedStack = interactionStack.copy();
        // 仮想スタックが空になると外部の restock 系 MOD が本物の手持ちスロットを補充対象と誤認しうる。
        // 2 個なら通常の 1 回使用後にも 1 個残り、アイテム側へ見せる個数の差も最小限に抑えられる。
        if (normalizedStack.getCount() <= 1 && normalizedStack.getMaxStackSize() > 1) {
            normalizedStack.setCount(2);
        }
        return normalizedStack;
    }

    public static InteractionResult useBlockByPlayerMainHand(Level level, ServerPlayer player, BlockPos pos,
                                                             ItemStack interactionStack, Direction hitFace) {
        if (player == null || player.isRemoved()) {
            return InteractionResult.PASS;
        }

        var originalItem = player.getMainHandItem();
        var hitResult = new BlockHitResult(Vec3.atCenterOf(pos), hitFace, pos, true);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, interactionStack);
            return level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hitResult);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
        }
    }
}
