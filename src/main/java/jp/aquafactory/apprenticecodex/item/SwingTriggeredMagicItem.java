package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public interface SwingTriggeredMagicItem {
    boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck);
}
