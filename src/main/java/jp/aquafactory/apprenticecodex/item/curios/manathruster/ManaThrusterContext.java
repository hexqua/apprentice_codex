package jp.aquafactory.apprenticecodex.item.curios.manathruster;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

public final class ManaThrusterContext {
    private ManaThrusterContext() {
    }

    public static boolean isDisabled(Player player) {
        var abilities = player.getAbilities();
        return player.isSpectator()
                || !player.isAlive()
                || abilities.instabuild
                || abilities.mayfly
                || abilities.flying
                || player.onClimbable()
                || player.isPassenger();
    }

    public static boolean isManaRecoveryFree(Player player) {
        return player.isInWaterOrBubble() || player.isSwimming() || isInLavaOrTouchingLava(player);
    }

    private static boolean isInLavaOrTouchingLava(Player player) {
        if (player.isInLava()) {
            return true;
        }

        var level = player.level();
        var box = player.getBoundingBox().deflate(1.0E-4D);
        var minX = net.minecraft.util.Mth.floor(box.minX);
        var maxX = net.minecraft.util.Mth.floor(box.maxX);
        var minY = net.minecraft.util.Mth.floor(box.minY);
        var maxY = net.minecraft.util.Mth.floor(box.maxY);
        var minZ = net.minecraft.util.Mth.floor(box.minZ);
        var maxZ = net.minecraft.util.Mth.floor(box.maxZ);

        var mutablePos = new BlockPos.MutableBlockPos();
        for (var y = minY; y <= maxY; ++y) {
            for (var x = minX; x <= maxX; ++x) {
                for (var z = minZ; z <= maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    var fluidState = level.getFluidState(mutablePos);
                    if (!fluidState.is(FluidTags.LAVA)) {
                        continue;
                    }

                    var fluidTop = y + fluidState.getHeight(level, mutablePos);
                    if (box.maxY > y && box.minY < fluidTop) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
