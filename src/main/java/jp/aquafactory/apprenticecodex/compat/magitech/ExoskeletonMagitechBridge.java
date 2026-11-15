package jp.aquafactory.apprenticecodex.compat.magitech;

import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.item.armor.AetherLifterItem;
import net.stln.magitech.content.item.armor.FlamglideStriderItem;

public final class ExoskeletonMagitechBridge {
    private ExoskeletonMagitechBridge() {}

    public static void apply(Player player, ExoskeletonAction action, ItemStack stack) {
        // 外骨格には回数制限を設けず、毎回マナを使って上流の移動・演出を利用する。
        if (action.isStrider()) {
            FlamglideStriderItem.longJump(player, 0, stack);
            ExoskeletonMovement.applyStriderCorrection(player, action);
        } else {
            AetherLifterItem.doubleJump(player, 0, stack);
        }
    }
}
