package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientBlockTargetingHelper {
    private ClientBlockTargetingHelper() {
    }

    public static BlockTargetData captureOutlinedTarget(Player player, double range) {
        var targetData = new BlockTargetData();
        var hitResult = Minecraft.getInstance().hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return targetData;
        }
        if (player.getEyePosition(1.0F).distanceToSqr(blockHit.getLocation()) > range * range) {
            return targetData;
        }

        var level = player.level();
        var hitPos = blockHit.getBlockPos();
        var hitFace = blockHit.getDirection();
        var placePos = level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.relative(hitFace);
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return targetData;
        }

        targetData.setTarget(hitPos, hitFace, blockHit.getLocation(), placePos, hitFace.getOpposite());
        return targetData;
    }

    public static BlockTargetData captureRaycastTarget(Player player, double range) {
        var targetData = new BlockTargetData();
        if (range <= 0.0D) {
            return targetData;
        }

        // 通常の照準結果はブロックリーチで打ち切られるため、スペル固有射程で改めてレイを取得する。
        var hitResult = player.pick(range, 1.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHit)) {
            return targetData;
        }

        var level = player.level();
        var hitPos = blockHit.getBlockPos();
        var hitFace = blockHit.getDirection();
        var placePos = blockHit.getType() == HitResult.Type.BLOCK
                ? (level.getBlockState(hitPos).canBeReplaced() ? hitPos : hitPos.relative(hitFace))
                : BlockPos.containing(blockHit.getLocation());

        // MISS終点も送ることで、短射程側が視線方向を保ったまま空中座標へ補正できる。
        targetData.setTarget(hitPos, hitFace, blockHit.getLocation(), placePos, hitFace.getOpposite());
        return targetData;
    }

    public static BlockTargetData captureOutlinedHitTarget(Player player, double range, boolean ignoreRange) {
        var targetData = new BlockTargetData();
        var hitResult = Minecraft.getInstance().hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return targetData;
        }
        if (!ignoreRange && player.getEyePosition(1.0F).distanceToSqr(blockHit.getLocation()) > range * range) {
            return targetData;
        }

        var hitPos = blockHit.getBlockPos();
        var hitFace = blockHit.getDirection();
        targetData.setTarget(hitPos, hitFace, blockHit.getLocation(), hitPos, hitFace);
        return targetData;
    }
}
