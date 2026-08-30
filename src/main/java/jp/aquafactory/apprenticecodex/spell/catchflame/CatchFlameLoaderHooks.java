package jp.aquafactory.apprenticecodex.spell.catchflame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * FakePlayer と portal frame 判定は loader 間で型・API が異なるため、backport 時の補正箇所をここへ限定する。
 */
final class CatchFlameLoaderHooks {
    private CatchFlameLoaderHooks() {
    }

    static boolean isRealPlayer(Entity entity) {
        return entity instanceof Player && !(entity instanceof FakePlayer);
    }

    static boolean mayIgniteEssenceSmoker(
            ServerPlayer player, BlockPos position, Direction hitFace, ItemStack igniter
    ) {
        if (player.isRemoved()) {
            return false;
        }

        var originalItem = player.getMainHandItem();
        var hitResult = new BlockHitResult(Vec3.atCenterOf(position), hitFace, position, false);
        try {
            // 保護 MOD には通常の火打ち石操作として見せるが、block/item の use 自体は呼び出さない。
            player.setItemInHand(InteractionHand.MAIN_HAND, igniter);
            var event = CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, position, hitResult);
            return !event.isCanceled() && !event.getUseBlock().isFalse();
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
        }
    }
}
