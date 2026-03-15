package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
}
