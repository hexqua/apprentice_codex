package jp.aquafactory.apprenticecodex.item.curios.manathruster;

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
}
