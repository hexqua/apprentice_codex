package jp.aquafactory.apprenticecodex.spell.catchflame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    static boolean isPortalFrame(Level level, BlockPos position) {
        return level.getBlockState(position).isPortalFrame(level, position);
    }
}
