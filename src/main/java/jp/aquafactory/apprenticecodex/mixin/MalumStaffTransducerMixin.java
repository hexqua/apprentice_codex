package jp.aquafactory.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.injection.Redirect;
import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import com.sammy.malum.common.capability.MalumPlayerDataCapability;
import net.minecraft.world.entity.player.Player;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerEvents;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractStaffItem.class)
public abstract class MalumStaffTransducerMixin {
    @Redirect(method = "releaseUsing", remap = true,
            at = @At(value = "FIELD", target = "Lcom/sammy/malum/common/item/curiosities/weapons/staff/AbstractStaffItem;chargeDuration:F", remap = false))
    private float apprenticecodex$releaseDuration(AbstractStaffItem staff, ItemStack stack, Level level, LivingEntity entity, int remaining) {
        return ManaSoulTransducerLogic.chargeDuration(staff.chargeDuration, entity);
    }

    @Redirect(method = "onUseTick", remap = true,
            at = @At(value = "FIELD", target = "Lcom/sammy/malum/common/item/curiosities/weapons/staff/AbstractStaffItem;chargeDuration:F", remap = false))
    private float apprenticecodex$visualDuration(AbstractStaffItem staff, Level level, LivingEntity entity, ItemStack stack, int remaining) {
        // 完了音の等値比較も含め、発射と演出に同じ整数tickを使う。
        return ManaSoulTransducerLogic.chargeDuration(staff.chargeDuration, entity);
    }

    @Redirect(method = "releaseUsing", remap = true,
            at = @At(value = "INVOKE", target = "Lcom/sammy/malum/common/capability/MalumPlayerDataCapability;getCapability(Lnet/minecraft/world/entity/player/Player;)Lcom/sammy/malum/common/capability/MalumPlayerDataCapability;", remap = false))
    private MalumPlayerDataCapability apprenticecodex$payForShot(Player player) {
        // この呼出しは発射成立・耐久消費の後に一度だけ通る。ローカルの代替capabilityに
        // 消費させ、本物の予備回数・回復進捗を触らない。代替をentityへ保存・同期しない。
        if (player instanceof ServerPlayer serverPlayer && ManaSoulTransducerEvents.tryPay(serverPlayer)) {
            var paid = new MalumPlayerDataCapability();
            paid.reserveStaffChargeHandler.chargeCount = 1;
            return paid;
        }
        return MalumPlayerDataCapability.getCapability(player);
    }
}
