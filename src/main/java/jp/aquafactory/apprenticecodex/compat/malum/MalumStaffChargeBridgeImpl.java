package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

final class MalumStaffChargeBridgeImpl {
    private MalumStaffChargeBridgeImpl() {}
    static boolean isWithinStaffChargeWindow(LivingEntity entity, int windowTicks) {
        if (!entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof AbstractStaffItem)) return false;
        // 1.6.7はチャージ属性を持たない。発射数取得は呼ばず、実際の使用状態だけを見る。
        int elapsed = entity.getTicksUsingItem();
        return elapsed >= 0 && elapsed <= windowTicks;
    }
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
