package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerEvents;
import net.minecraft.server.level.ServerPlayer;

final class MalumStaffChargeBridgeImpl {
    private MalumStaffChargeBridgeImpl() {}
    static void clearHeldCooldowns(ServerPlayer player) {
        for (var stack : player.getHandSlots()) {
            var item = stack.getItem();
            if (item instanceof AbstractStaffItem && player.getCooldowns().isOnCooldown(item)
                    && ManaSoulTransducerEvents.tryPay(player)) {
                // ServerItemCooldownsが解除をクライアントへ同期する。
                player.getCooldowns().removeCooldown(item);
            }
        }
    }
}
